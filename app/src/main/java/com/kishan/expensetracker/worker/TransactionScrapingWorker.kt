package com.kishan.expensetracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kishan.expensetracker.data.database.ExpenseDatabase
import com.kishan.expensetracker.data.repository.ExpenseRepository
import com.kishan.expensetracker.util.GmailTransactionScraper
import com.kishan.expensetracker.util.TransactionCategorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TransactionScrapingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = ExpenseDatabase.getDatabase(applicationContext)
            val repository = ExpenseRepository(
                database.transactionDao(),
                database.categoryDao()
            )

            val authHelper = com.kishan.expensetracker.auth.GmailAuthHelper(applicationContext)

            // Check if user is authenticated
            if (!authHelper.isAuthenticated()) {
                return@withContext Result.success() // Skip if not authenticated
            }

            val scraper = GmailTransactionScraper(applicationContext)

            // Get the selected account name
            val accountName = authHelper.getSelectedAccountName()
            if (accountName == null) {
                return@withContext Result.success() // Skip if no account selected
            }

            // Scrape transactions from Gmail (last 7 days for daily sync)
            val transactions = scraper.scrapeTransactions(accountName, daysToSync = 7)

            // Filter out duplicates by checking if transaction already exists
            val existingTransactions = repository.getAllTransactions().first()
            val existingKeys = existingTransactions.map {
                "${it.date.time}_${it.amount}_${it.source.name}_${it.description}"
            }.toSet()

            val newTransactions = transactions
                .map { TransactionCategorizer.categorizeTransaction(it) }
                .filter { transaction ->
                    val key = "${transaction.date.time}_${transaction.amount}_${transaction.source.name}_${transaction.description}"
                    !existingKeys.contains(key)
                }

            if (newTransactions.isNotEmpty()) {
                repository.insertTransactions(newTransactions)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

