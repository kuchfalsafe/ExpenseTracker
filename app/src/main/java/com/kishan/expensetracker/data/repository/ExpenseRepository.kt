package com.kishan.expensetracker.data.repository

import com.kishan.expensetracker.data.dao.CategoryDao
import com.kishan.expensetracker.data.dao.TransactionDao
import com.kishan.expensetracker.data.entity.Category
import com.kishan.expensetracker.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.*

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    // Transaction operations
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByCategoryAndDateRange(
        category: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategoryAndDateRange(category, startDate, endDate)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<Transaction>): List<Long> =
        transactionDao.insertTransactions(transactions)

    suspend fun updateTransaction(transaction: Transaction): Int =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction): Int =
        transactionDao.deleteTransaction(transaction)

    fun getTotalDebitByDateRange(startDate: Date, endDate: Date): Flow<Double?> =
        transactionDao.getTotalDebitByDateRange(startDate, endDate)

    fun getTotalCreditByDateRange(startDate: Date, endDate: Date): Flow<Double?> =
        transactionDao.getTotalCreditByDateRange(startDate, endDate)

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category): Int = categoryDao.deleteCategory(category)

    // Date utility functions
    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }

    fun getStartOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    fun getEndOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }

    fun getStartOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    fun getEndOfMonth(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }
}

