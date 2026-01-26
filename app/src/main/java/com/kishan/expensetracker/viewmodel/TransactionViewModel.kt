package com.kishan.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.data.entity.TransactionType
import com.kishan.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class TransactionViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTransactionsByCategoryAndDateRange(
        category: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>> {
        return repository.getTransactionsByCategoryAndDateRange(category, startDate, endDate)
    }

    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun getTotalDebitByDateRange(startDate: Date, endDate: Date): Flow<Double?> {
        return repository.getTotalDebitByDateRange(startDate, endDate)
    }

    fun getTotalCreditByDateRange(startDate: Date, endDate: Date): Flow<Double?> {
        return repository.getTotalCreditByDateRange(startDate, endDate)
    }

    fun getCategoryWiseExpenses(startDate: Date, endDate: Date): Flow<Map<String, Double>> {
        return flow {
            val transactions = repository.getTransactionsByDateRange(startDate, endDate)
                .first()
                .filter { it.type == TransactionType.DEBIT }

            val categoryMap = transactions.groupBy { it.category }
                .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

            emit(categoryMap)
        }
    }
}

