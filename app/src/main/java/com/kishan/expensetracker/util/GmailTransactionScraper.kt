package com.kishan.expensetracker.util

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePartHeader
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.data.entity.TransactionSource
import com.kishan.expensetracker.data.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.Charsets.UTF_8

class GmailTransactionScraper(private val context: Context) {

    private fun getGmailService(accountName: String?): Gmail {
        // Get the account name - prioritize passed parameter, then saved account
        val authHelper = com.kishan.expensetracker.auth.GmailAuthHelper(context)
        val accountToUse = accountName?.takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: authHelper.getSelectedAccountName()?.takeIf { it.isNotEmpty() && it.isNotBlank() }

        if (accountToUse == null || accountToUse.isEmpty() || accountToUse.isBlank()) {
            throw IllegalArgumentException("Account name must not be null or empty. Please select a Google account first. Provided: '$accountName'")
        }

        // Create a fresh credential with the account name set BEFORE building Gmail service
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(GmailScopes.GMAIL_READONLY)
        )

        // Set account name explicitly
        credential.selectedAccountName = accountToUse

        // Verify it's set
        if (credential.selectedAccountName == null || credential.selectedAccountName!!.isEmpty()) {
            throw IllegalStateException("Failed to set account name on credential. Account: '$accountToUse'")
        }

        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("ExpenseTracker").build()
    }

    suspend fun scrapeTransactions(accountName: String?, daysToSync: Int = 7): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()

        try {
            // Validate account name before proceeding
            val validatedAccountName = accountName?.takeIf { it.isNotEmpty() && it.isNotBlank() }
                ?: throw IllegalArgumentException("Account name is null or empty. Cannot proceed with Gmail sync.")

            // Get Gmail service with the account name - this ensures account is set
            val gmailService = getGmailService(validatedAccountName)

            // Calculate date for filtering (Gmail uses YYYY/MM/DD format)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -daysToSync)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val dateFilter = "after:$year/$month/$day"

            // Search for transaction emails from different sources
            val hdfcUpiQuery = "(from:noreply@hdfcbank.net OR from:alerts@hdfcbank.net) subject:UPI $dateFilter"
            val hdfcCreditQuery = "(from:noreply@hdfcbank.net OR from:alerts@hdfcbank.net) subject:credit $dateFilter"
            val iciciQuery = "from:alerts@icicibank.com subject:transaction $dateFilter"
            val sbiQuery = "(from:alerts@sbi.co.in OR from:onlinesbi@sbi.co.in) subject:UPI $dateFilter"

            val queries = listOf(
                hdfcUpiQuery to TransactionSource.HDFC_UPI,
                hdfcCreditQuery to TransactionSource.HDFC_CREDIT_CARD,
                iciciQuery to TransactionSource.ICICI_CREDIT_CARD,
                sbiQuery to TransactionSource.SBI_UPI
            )

            for ((query, source) in queries) {
                val messages = gmailService.users().messages().list("me")
                    .setQ(query)
                    .setMaxResults(500L) // Increased to handle more historical data
                    .execute()

                messages.messages?.forEach { messageRef ->
                    try {
                        val message = gmailService.users().messages()
                            .get("me", messageRef.id)
                            .setFormat("full")
                            .execute()

                        val transaction = parseEmailToTransaction(message, source)
                        transaction?.let { transactions.add(it) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        transactions
    }

    private fun parseEmailToTransaction(message: Message, source: TransactionSource): Transaction? {
        try {
            val headers = message.payload.headers
            val subject = headers.find { it.name == "Subject" }?.value ?: ""
            val body = extractBody(message.payload)

            // Parse transaction details from email
            val amount = extractAmount(subject + " " + body)
            val date = extractDate(headers, body)
            val description = extractDescription(subject + " " + body)
            val type = determineTransactionType(subject + " " + body, amount)

            if (amount != null && date != null) {
                val category = TransactionCategorizer.categorizeTransaction(description)
                return Transaction(
                    date = date,
                    amount = amount,
                    category = category,
                    type = type,
                    source = source,
                    description = description,
                    isManual = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun extractBody(payload: com.google.api.services.gmail.model.MessagePart): String {
        var body = ""
        if (payload.body?.data != null) {
            body = String(Base64.getUrlDecoder().decode(payload.body.data), UTF_8)
        } else if (payload.parts != null) {
            for (part in payload.parts) {
                if (part.mimeType == "text/plain" || part.mimeType == "text/html") {
                    if (part.body?.data != null) {
                        body += String(Base64.getUrlDecoder().decode(part.body.data), UTF_8)
                    }
                }
            }
        }
        return body
    }

    private fun extractAmount(text: String): Double? {
        // Pattern: INR 1,234.56 or ₹1,234.56 or Rs. 1234.56
        val patterns = listOf(
            Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""([\d,]+\.?\d*)\s*(?:INR|Rs\.?|₹)""", RegexOption.IGNORE_CASE),
            Regex("""debited.*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""credited.*?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }

        return null
    }

    private fun extractDate(headers: List<MessagePartHeader>, body: String): Date? {
        // Try to get date from email header first
        val dateHeader = headers.find { it.name == "Date" }?.value
        if (dateHeader != null) {
            try {
                val formats = listOf(
                    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
                    SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
                    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                )

                for (format in formats) {
                    try {
                        return format.parse(dateHeader)
                    } catch (e: Exception) {
                        // Try next format
                    }
                }
            } catch (e: Exception) {
                // Continue to body parsing
            }
        }

        // Try to extract from body
        val datePatterns = listOf(
            Regex("""(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})"""),
            Regex("""(\d{1,2}\s+\w+\s+\d{4})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(body)
            if (match != null) {
                val dateStr = match.groupValues[1]
                val formats = listOf(
                    SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
                    SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
                    SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                )

                for (format in formats) {
                    try {
                        return format.parse(dateStr)
                    } catch (e: Exception) {
                        // Try next format
                    }
                }
            }
        }

        // Default to current date if parsing fails
        return Date()
    }

    private fun extractDescription(text: String): String {
        // Extract merchant name or transaction description
        val patterns = listOf(
            Regex("""(?:to|at|from)\s+([A-Z][A-Za-z\s]+?)(?:\s+on|\s+for|$)""", RegexOption.IGNORE_CASE),
            Regex("""merchant[:\s]+([A-Z][A-Za-z\s]+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Fallback: return first 50 characters
        return text.take(50).trim()
    }

    private fun determineTransactionType(text: String, amount: Double?): TransactionType {
        val lowerText = text.lowercase(Locale.getDefault())
        return when {
            lowerText.contains("credited") || lowerText.contains("credit") -> TransactionType.CREDIT
            lowerText.contains("debited") || lowerText.contains("debit") -> TransactionType.DEBIT
            else -> TransactionType.DEBIT // Default to debit for expenses
        }
    }
}

