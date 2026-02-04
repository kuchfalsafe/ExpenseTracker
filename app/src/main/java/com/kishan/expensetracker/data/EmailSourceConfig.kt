package com.kishan.expensetracker.data

import com.kishan.expensetracker.data.entity.TransactionSource

/**
 * Configuration for email sources from which transactions are scraped
 */
data class EmailSourceConfig(
    val source: TransactionSource,
    val emailAddresses: List<String>, // List of email addresses to monitor
    val subjectKeywords: List<String>, // Keywords to search in subject (e.g., "UPI", "credit", "transaction")
    val descriptionPhrases: List<String>? = null // Phrases to extract description from body (e.g., "transaction of", "amount paid", "info:", "towards")
)

