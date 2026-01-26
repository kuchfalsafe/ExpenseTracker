package com.kishan.expensetracker.util

import com.kishan.expensetracker.data.entity.Transaction
import java.util.Locale

object TransactionCategorizer {
    private val categoryKeywords = mapOf(
        "Food" to listOf(
            "restaurant", "food", "zomato", "swiggy", "uber eats", "pizza", "burger",
            "cafe", "coffee", "starbucks", "mcdonald", "kfc", "domino", "subway"
        ),
        "Groceries" to listOf(
            "grocery", "supermarket", "bigbasket", "grofers", "dmart", "reliance",
            "more", "spencer", "hypermarket"
        ),
        "Transport" to listOf(
            "uber", "ola", "taxi", "metro", "bus", "train", "railway", "petrol",
            "fuel", "gas", "parking", "toll"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "shopping", "mall", "store", "retail"
        ),
        "Bills" to listOf(
            "electricity", "water", "gas", "phone", "mobile", "internet", "broadband",
            "utility", "bill payment", "recharge"
        ),
        "Entertainment" to listOf(
            "netflix", "prime", "spotify", "youtube", "movie", "cinema", "theater",
            "entertainment", "streaming"
        ),
        "Healthcare" to listOf(
            "hospital", "pharmacy", "medicine", "doctor", "clinic", "medical",
            "apollo", "pharmeasy", "1mg"
        ),
        "Education" to listOf(
            "school", "college", "university", "course", "education", "tuition",
            "book", "stationery"
        ),
        "Travel" to listOf(
            "hotel", "flight", "booking", "travel", "trip", "vacation", "make my trip",
            "goibibo", "yatra"
        ),
        "Utilities" to listOf(
            "maintenance", "repair", "service", "utility"
        )
    )

    fun categorizeTransaction(description: String): String {
        val lowerDescription = description.lowercase(Locale.getDefault())

        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lowerDescription.contains(it) }) {
                return category
            }
        }

        return "Unknown"
    }

    fun categorizeTransaction(transaction: Transaction): Transaction {
        if (transaction.category != "Unknown" && transaction.category.isNotEmpty()) {
            return transaction
        }

        val category = categorizeTransaction(transaction.description)
        return transaction.copy(category = category)
    }
}

