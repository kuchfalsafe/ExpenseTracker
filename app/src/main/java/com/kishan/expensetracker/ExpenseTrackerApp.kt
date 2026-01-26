package com.kishan.expensetracker

import android.app.Application
import com.kishan.expensetracker.data.database.ExpenseDatabase
import com.kishan.expensetracker.data.entity.Category
import com.kishan.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseTrackerApp : Application() {

    private val database by lazy { ExpenseDatabase.getDatabase(this) }
    val repository by lazy {
        ExpenseRepository(
            database.transactionDao(),
            database.categoryDao()
        )
    }

    companion object {
        private var instance: ExpenseTrackerApp? = null

        fun getRepository(context: android.content.Context): ExpenseRepository {
            if (instance == null) {
                instance = context.applicationContext as? ExpenseTrackerApp
            }
            return instance?.repository ?: run {
                val db = ExpenseDatabase.getDatabase(context)
                ExpenseRepository(db.transactionDao(), db.categoryDao())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDefaultCategories()
    }

    private fun initializeDefaultCategories() {
        // Initialize default categories
        val defaultCategories = listOf(
            "Food", "Groceries", "Transport", "Shopping", "Bills",
            "Entertainment", "Healthcare", "Education", "Travel", "Utilities", "Unknown"
        )

        CoroutineScope(Dispatchers.IO).launch {
            defaultCategories.forEach { categoryName ->
                try {
                    repository.insertCategory(
                        Category(
                            name = categoryName,
                            isDefault = true
                        )
                    )
                } catch (e: Exception) {
                    // Category might already exist, ignore
                }
            }
        }
    }
}

