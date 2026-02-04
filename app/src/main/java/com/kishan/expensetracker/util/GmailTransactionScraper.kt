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
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
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
        android.util.Log.d("GmailScraper", "getGmailService called with accountName: '$accountName'")

        // Get the account name - prioritize passed parameter, then saved account
        val authHelper = com.kishan.expensetracker.auth.GmailAuthHelper(context)
        val savedAccount = authHelper.getSelectedAccountName()
        android.util.Log.d("GmailScraper", "Saved account from helper: '$savedAccount'")

        val accountToUse = accountName?.takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: savedAccount?.takeIf { it.isNotEmpty() && it.isNotBlank() }

        if (accountToUse == null || accountToUse.isEmpty() || accountToUse.isBlank()) {
            val error = "Account name must not be null or empty. Provided: '$accountName', Saved: '$savedAccount'"
            android.util.Log.e("GmailScraper", error)
            throw IllegalArgumentException(error)
        }

        android.util.Log.d("GmailScraper", "Using account: '$accountToUse'")

        // Create a FRESH credential - don't reuse from auth helper as it might have stale state
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(GmailScopes.GMAIL_READONLY)
        )

        // CRITICAL: Set account name BEFORE any operations
        // GoogleAccountCredential stores this internally and uses it when getToken() is called
        credential.selectedAccountName = accountToUse

        // Verify it was set (even though getter might return null, the setter works)
        android.util.Log.d("GmailScraper", "Set credential.selectedAccountName to: '$accountToUse'")
        android.util.Log.d("GmailScraper", "Credential selectedAccountName getter returns: '${credential.selectedAccountName}'")

        // Note: The getter might return null even after setting, but the setter works.
        // GoogleAccountCredential uses the account name internally when getToken() is called.
        // If the account doesn't exist or permission is needed, getToken() will throw
        // UserRecoverableAuthException which we handle in the UI layer.

        android.util.Log.d("GmailScraper", "Gmail service created with account: '$accountToUse'")
        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("ExpenseTracker").build()
    }

    suspend fun scrapeTransactions(accountName: String?, daysToSync: Int = 7, fromTimestamp: Long? = null): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()

        try {
            // Validate account name before proceeding
            val validatedAccountName = accountName?.takeIf { it.isNotEmpty() && it.isNotBlank() }
                ?: throw IllegalArgumentException("Account name is null or empty. Cannot proceed with Gmail sync.")

            android.util.Log.d("GmailScraper", "Starting scrapeTransactions with account: '$validatedAccountName', daysToSync: $daysToSync, fromTimestamp: $fromTimestamp")

            // Get Gmail service with the account name - this ensures account is set
            // Note: We don't check AccountManager here because GET_ACCOUNTS permission
            // is deprecated. GoogleAccountCredential will handle account lookup internally.
            val gmailService = getGmailService(validatedAccountName)

            // Calculate date for filtering (Gmail uses YYYY/MM/DD format)
            val calendar = Calendar.getInstance()
            if (fromTimestamp != null) {
                // Use provided timestamp
                calendar.timeInMillis = fromTimestamp
                android.util.Log.d("GmailScraper", "Using provided timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)}")
            } else {
                // Use daysToSync
                calendar.add(Calendar.DAY_OF_MONTH, -daysToSync)
                android.util.Log.d("GmailScraper", "Using daysToSync: $daysToSync")
            }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val dateFilter = "after:$year/$month/$day"

            // Get configured email sources
            val configHelper = EmailSourceConfigHelper(context)
            val emailSources = configHelper.getEmailSources()

            // Build queries from configured sources
            val queries = emailSources.map { (source, config) ->
                // Build "from:" query with all email addresses
                val fromQuery = if (config.emailAddresses.size == 1) {
                    "from:${config.emailAddresses[0]}"
                } else {
                    config.emailAddresses.joinToString(" OR ") { "from:$it" }.let { "($it)" }
                }

                // Build subject query with keywords
                val subjectQuery = if (config.subjectKeywords.isNotEmpty()) {
                    if (config.subjectKeywords.size == 1) {
                        "subject:${config.subjectKeywords[0]}"
                    } else {
                        config.subjectKeywords.joinToString(" OR ") { "subject:$it" }.let { "($it)" }
                    }
                } else {
                    ""
                }

                // Combine into final query
                val query = listOfNotNull(fromQuery, subjectQuery, dateFilter)
                    .joinToString(" ")
                    .trim()

                query to source
            }

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
        } catch (e: UserRecoverableAuthIOException) {
            // Propagate this exception to UI layer so consent dialog can be shown
            android.util.Log.d("GmailScraper", "UserRecoverableAuthIOException - needs consent, propagating to UI")
            throw e
        } catch (e: UserRecoverableAuthException) {
            // Propagate this exception to UI layer so consent dialog can be shown
            android.util.Log.d("GmailScraper", "UserRecoverableAuthException - needs consent, propagating to UI")
            // Wrap in UserRecoverableAuthIOException for consistency
            // UserRecoverableAuthIOException constructor takes the underlying exception
            throw com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException(e)
        } catch (e: Exception) {
            android.util.Log.e("GmailScraper", "Error during Gmail scraping", e)
            e.printStackTrace()
            // Re-throw other exceptions so UI can handle them
            throw e
        }

        transactions
    }

    private fun parseEmailToTransaction(message: Message, source: TransactionSource): Transaction? {
        try {
            val headers = message.payload.headers
            val subject = headers.find { it.name == "Subject" }?.value ?: ""
            val body = extractBody(message.payload)

            // Log email content for pattern analysis (only first 500 chars to avoid spam)
            val emailPreview = (subject + " " + body).take(500)
            android.util.Log.d("GmailScraper", "=== Email Pattern Analysis ===")
            android.util.Log.d("GmailScraper", "Source: $source")
            android.util.Log.d("GmailScraper", "Subject: $subject")
            android.util.Log.d("GmailScraper", "Body preview (first 500 chars): $emailPreview")
            android.util.Log.d("GmailScraper", "Body length: ${body.length}")
            android.util.Log.d("GmailScraper", "=============================")

            // Parse transaction details from email
            val amount = extractAmount(subject + " " + body)
            val date = extractDate(headers, body)
            val description = extractDescription(subject, body, source)
            val type = determineTransactionType(subject + " " + body, amount)

            android.util.Log.d("GmailScraper", "Extracted - Amount: $amount, Date: $date, Description: $description, Type: $type")

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
        var plainTextBody = ""
        var htmlBody = ""

        if (payload.body?.data != null) {
            val decoded = String(Base64.getUrlDecoder().decode(payload.body.data), UTF_8)
            // Check if it's HTML or plain text
            if (decoded.trimStart().startsWith("<", ignoreCase = false)) {
                htmlBody = decoded
            } else {
                plainTextBody = decoded
            }
        } else if (payload.parts != null) {
            // First pass: collect text/plain and text/html separately
            for (part in payload.parts) {
                if (part.body?.data != null) {
                    val decoded = String(Base64.getUrlDecoder().decode(part.body.data), UTF_8)
                    when (part.mimeType) {
                        "text/plain" -> {
                            plainTextBody += decoded
                        }
                        "text/html" -> {
                            htmlBody += decoded
                        }
                    }
                }

                // Recursively check nested parts
                if (part.parts != null) {
                    for (nestedPart in part.parts) {
                        if (nestedPart.body?.data != null) {
                            val decoded = String(Base64.getUrlDecoder().decode(nestedPart.body.data), UTF_8)
                            when (nestedPart.mimeType) {
                                "text/plain" -> {
                                    plainTextBody += decoded
                                }
                                "text/html" -> {
                                    htmlBody += decoded
                                }
                            }
                        }
                    }
                }
            }
        }

        // Prefer plain text, but if only HTML is available, extract text from HTML
        return if (plainTextBody.isNotEmpty()) {
            plainTextBody
        } else if (htmlBody.isNotEmpty()) {
            stripHtmlTags(htmlBody)
        } else {
            ""
        }
    }

    /**
     * Strip HTML tags and extract plain text from HTML content
     */
    private fun stripHtmlTags(html: String): String {
        var text = html

        // Remove script and style tags with their content (using multiline mode)
        text = text.replace(Regex("""<script[^>]*>[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""<style[^>]*>[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")

        // Replace common HTML entities
        text = text.replace("&nbsp;", " ")
        text = text.replace("&amp;", "&")
        text = text.replace("&lt;", "<")
        text = text.replace("&gt;", ">")
        text = text.replace("&quot;", "\"")
        text = text.replace("&#39;", "'")
        text = text.replace("&apos;", "'")

        // Remove HTML tags
        text = text.replace(Regex("""<[^>]+>"""), " ")

        // Decode numeric HTML entities (e.g., &#160;)
        text = text.replace(Regex("""&#(\d+);""")) { matchResult ->
            val code = matchResult.groupValues[1].toIntOrNull()
            if (code != null && code in 0..127) {
                code.toChar().toString()
            } else {
                matchResult.value
            }
        }

        // Clean up whitespace
        text = text.replace(Regex("""\s+"""), " ")
        text = text.replace(Regex("""\n\s*\n"""), "\n")

        return text.trim()
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

    private fun extractDescription(subject: String, body: String, source: TransactionSource): String {
        val fullText = "$subject $body"
        val bodyLower = body.lowercase(Locale.getDefault())

        // First, try user-provided description phrases from email source config (search in body only, not subject)
        val configHelper = EmailSourceConfigHelper(context)
        val emailSources = configHelper.getEmailSources()
        val config = emailSources[source]
        val phrases = config?.descriptionPhrases?.filter { it.isNotBlank() } ?: emptyList()

        for (phrase in phrases) {
            if (phrase.isBlank()) continue

            val phraseLower = phrase.lowercase(Locale.getDefault()).trim()

            // Match phrase (not word boundaries, as phrases can contain spaces)
            // Escape special regex characters in the phrase
            val escapedPhrase = Regex.escape(phraseLower)
            val phrasePattern = Regex(escapedPhrase, RegexOption.IGNORE_CASE)
            val match = phrasePattern.find(bodyLower)

            if (match != null) {
                val phraseIndex = match.range.last + 1 // Position after the matched phrase

                // Extract text after the phrase from body only
                val afterPhrase = body.substring(phraseIndex).trim()

                // Extract text until period (full stop) - split on period and take first part
                val parts = afterPhrase.split(Regex("""\."""), limit = 2)
                val extracted = parts[0].trim()

                if (extracted.isNotEmpty() && extracted.length in 3..100) {
                    android.util.Log.d("GmailScraper", "Extracted description using phrase '$phrase' from body: '$extracted'")
                    return extracted.take(50) // Limit length
                }
            }
        }

        // Bank-specific patterns for extracting merchant/company names
        val patterns = when (source) {
            TransactionSource.HDFC_UPI -> listOf(
                // HDFC UPI patterns
                Regex("""(?:paid to|to|at)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+UPI|\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""UPI\s+(?:payment|transaction)\s+(?:to|at)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""merchant[:\s]+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""([A-Z][A-Za-z0-9\s&]{3,30}?)\s+UPI""", RegexOption.IGNORE_CASE)
            )
            TransactionSource.HDFC_CREDIT_CARD -> listOf(
                // HDFC Credit Card patterns
                Regex("""(?:purchase|transaction|payment)\s+(?:at|from)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""merchant[:\s]+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""([A-Z][A-Za-z0-9\s&]{3,30}?)\s+(?:card|transaction)""", RegexOption.IGNORE_CASE)
            )
            TransactionSource.ICICI_CREDIT_CARD -> listOf(
                // ICICI Credit Card patterns
                Regex("""(?:purchase|transaction|payment)\s+(?:at|from)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""merchant[:\s]+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""([A-Z][A-Za-z0-9\s&]{3,30}?)\s+(?:card|transaction)""", RegexOption.IGNORE_CASE)
            )
            TransactionSource.SBI_UPI -> listOf(
                // SBI UPI patterns
                Regex("""(?:paid to|to|at)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+UPI|\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""UPI\s+(?:payment|transaction)\s+(?:to|at)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""merchant[:\s]+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""([A-Z][A-Za-z0-9\s&]{3,30}?)\s+UPI""", RegexOption.IGNORE_CASE)
            )
            else -> listOf(
                // Generic patterns
                Regex("""(?:to|at|from)\s+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE),
                Regex("""merchant[:\s]+([A-Z][A-Za-z0-9\s&]+?)(?:\s+on|\s+for|\.|$)""", RegexOption.IGNORE_CASE)
            )
        }

        // Try patterns in order
        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val extracted = match.groupValues[1].trim()
                // Clean up common suffixes/prefixes
                val cleaned = extracted
                    .replace(Regex("""\s+(?:UPI|card|transaction|payment|on|for).*$""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^(?:at|to|from)\s+""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (cleaned.length in 3..50) { // Reasonable length
                    android.util.Log.d("GmailScraper", "Extracted description: '$cleaned' using pattern: ${pattern.pattern}")
                    return cleaned
                }
            }
        }

        // Fallback: Try to extract from subject line (usually more concise)
        val subjectWords = subject.split(Regex("""\s+"""))
            .filter { it.length > 2 && !it.matches(Regex("""\d+""")) } // Remove short words and numbers
            .filter { !it.equals("UPI", ignoreCase = true) &&
                      !it.equals("transaction", ignoreCase = true) &&
                      !it.equals("payment", ignoreCase = true) &&
                      !it.equals("card", ignoreCase = true) &&
                      !it.equals("credit", ignoreCase = true) &&
                      !it.equals("debit", ignoreCase = true) &&
                      !it.equals("INR", ignoreCase = true) &&
                      !it.equals("Rs", ignoreCase = true) }
            .take(5) // Take first 5 meaningful words

        if (subjectWords.isNotEmpty()) {
            val subjectDesc = subjectWords.joinToString(" ").take(50)
            android.util.Log.d("GmailScraper", "Using subject words as description: '$subjectDesc'")
            return subjectDesc
        }

        // Last fallback: first meaningful words from body (not full body)
        val bodyWords = body.split(Regex("""\s+"""))
            .filter { it.length > 3 && !it.matches(Regex("""\d+""")) }
            .filter { !it.matches(Regex("""^(?:to|at|from|on|for|the|and|or|is|was|are|were)$""", RegexOption.IGNORE_CASE)) }
            .take(5)

        if (bodyWords.isNotEmpty()) {
            val bodyDesc = bodyWords.joinToString(" ").take(50)
            android.util.Log.d("GmailScraper", "Using body words as description: '$bodyDesc'")
            return bodyDesc
        }

        // Final fallback: return a generic description
        android.util.Log.w("GmailScraper", "Could not extract meaningful description, using generic")
        return "Transaction"
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

