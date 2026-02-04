package com.kishan.expensetracker.util

import android.content.Context
import androidx.work.*
import com.kishan.expensetracker.worker.TransactionScrapingWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {
    private const val WORK_NAME = "transaction_scraping_work"

    fun scheduleDailyTransactionScraping(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<TransactionScrapingWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    fun cancelDailyTransactionScraping(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}


