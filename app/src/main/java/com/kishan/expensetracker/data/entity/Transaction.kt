package com.kishan.expensetracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val source: TransactionSource,
    val description: String = "",
    val isManual: Boolean = false
)

enum class TransactionType {
    CREDIT,
    DEBIT
}

enum class TransactionSource {
    HDFC_UPI,
    HDFC_CREDIT_CARD,
    ICICI_CREDIT_CARD,
    SBI_UPI,
    MANUAL
}

