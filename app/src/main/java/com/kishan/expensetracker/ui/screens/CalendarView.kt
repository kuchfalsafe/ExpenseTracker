package com.kishan.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kishan.expensetracker.ExpenseTrackerApp
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.data.repository.ExpenseRepository
import com.kishan.expensetracker.ui.components.MonthCalendar
import com.kishan.expensetracker.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarView(
    selectedMonth: Int,
    selectedYear: Int,
    selectedDate: Int?,
    onDateSelected: (Int) -> Unit,
    viewModel: TransactionViewModel,
    repository: ExpenseRepository,
    navController: NavController
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthName = monthNames[selectedMonth]

    // Get date range for selected day
    val (dayStartDate, dayEndDate) = remember(selectedDate, selectedMonth, selectedYear) {
        if (selectedDate != null) {
            val dayDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDate)
            }.time
            Pair(
                repository.getStartOfDay(dayDate),
                repository.getEndOfDay(dayDate)
            )
        } else {
            Pair(null, null)
        }
    }

    val dayTransactions by remember(dayStartDate, dayEndDate) {
        if (dayStartDate != null && dayEndDate != null) {
            viewModel.getTransactionsByDateRange(dayStartDate, dayEndDate)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<Transaction>())
        }
    }.collectAsState(initial = emptyList())

    val dayTotalDebit by remember(dayStartDate, dayEndDate) {
        if (dayStartDate != null && dayEndDate != null) {
            viewModel.getTotalDebitByDateRange(dayStartDate, dayEndDate)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)

    val dayCategoryExpenses by remember(dayStartDate, dayEndDate) {
        if (dayStartDate != null && dayEndDate != null) {
            viewModel.getCategoryWiseExpenses(dayStartDate, dayEndDate)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyMap<String, Double>())
        }
    }.collectAsState(initial = emptyMap())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header
        item {
            Text(
                text = "$monthName $selectedYear",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Calendar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    MonthCalendar(
                        month = selectedMonth,
                        year = selectedYear,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }

        // Selected Date Details
        if (selectedDate != null) {
            item {
                Text(
                    text = "Expenses for $selectedDate ${monthName.take(3)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Total Expense for the Day
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Expense",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${String.format("%.2f", dayTotalDebit ?: 0.0)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Category Wise Expenses
            if (dayCategoryExpenses.isNotEmpty()) {
                item {
                    Text(
                        text = "Category Wise Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(dayCategoryExpenses.toList().sortedByDescending { it.second }) { (category, amount) ->
                    CategoryExpenseCard(
                        category = category,
                        amount = amount,
                        onClick = {
                            navController.navigate("category_expenses/$category/daily")
                        }
                    )
                }
            }

            // Individual Expenses
            item {
                Text(
                    text = "Individual Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(dayTransactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    onEdit = {
                        navController.navigate("edit_transaction/${transaction.id}")
                    },
                    onDelete = { transactionToDelete ->
                        viewModel.deleteTransaction(transactionToDelete)
                    }
                )
            }

            if (dayTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Select a date to view expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

