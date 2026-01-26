package com.kishan.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.viewmodel.TransactionViewModel

@Composable
fun MonthSummaryView(
    totalDebit: Double,
    totalCredit: Double,
    categoryExpenses: Map<String, Double>,
    transactions: List<Transaction>,
    navController: NavController,
    selectedMonth: Int,
    selectedYear: Int,
    viewModel: TransactionViewModel
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthName = monthNames[selectedMonth]

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

        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "Total Expenses",
                    amount = totalDebit,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total Income",
                    amount = totalCredit,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Category Wise Expenses
        if (categoryExpenses.isNotEmpty()) {
            item {
                Text(
                    text = "Category Wise Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(categoryExpenses.toList().sortedByDescending { it.second }) { (category, amount) ->
                CategoryExpenseCard(
                    category = category,
                    amount = amount,
                    onClick = {
                        navController.navigate("category_expenses/$category/monthly")
                    }
                )
            }

            // Recent Transactions
            if (transactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(transactions.take(10)) { transaction ->
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
            }
        }
    }
}

