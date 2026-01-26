package com.kishan.expensetracker.data.dao

import androidx.room.*
import com.kishan.expensetracker.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByCategoryAndDateRange(category: String, startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByTypeAndDateRange(type: String, startDate: Date, endDate: Date): Flow<List<Transaction>>

    @Query("SELECT DISTINCT category FROM transactions")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalDebitByDateRange(startDate: Date, endDate: Date): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalCreditByDateRange(startDate: Date, endDate: Date): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate")
    suspend fun getDebitTransactionsByDateRange(startDate: Date, endDate: Date): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: Transaction): Int

    @Delete
    suspend fun deleteTransaction(transaction: Transaction): Int
}

