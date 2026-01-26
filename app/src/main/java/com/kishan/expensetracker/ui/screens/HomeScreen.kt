package com.kishan.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kishan.expensetracker.ExpenseTrackerApp
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.ui.components.MonthCalendar
import com.kishan.expensetracker.viewmodel.TransactionViewModel
import com.kishan.expensetracker.viewmodel.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            ExpenseTrackerApp.getRepository(context)
        )
    )
    val repository = remember {
        ExpenseTrackerApp.getRepository(context)
    }

    val calendar = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf<Int?>(null) } // Selected day in calendar view

    // Create a date in the selected month/year
    val selectedMonthDate = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
    }

    // Always use full month for date range
    val (startDate, endDate) = remember(selectedMonth, selectedYear) {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time

        Pair(
            repository.getStartOfMonth(monthStart),
            repository.getEndOfMonth(monthStart)
        )
    }

    // Date range for selected day in calendar view
    val (selectedDayStartDate, selectedDayEndDate) = remember(selectedDate, selectedMonth, selectedYear) {
        if (selectedDate != null) {
            val dayDate = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDate!!)
            }.time
            Pair(
                repository.getStartOfDay(dayDate),
                repository.getEndOfDay(dayDate)
            )
        } else {
            Pair(startDate, endDate)
        }
    }

    val transactions by viewModel.getTransactionsByDateRange(startDate, endDate)
        .collectAsState(initial = emptyList())

    val totalDebit by viewModel.getTotalDebitByDateRange(startDate, endDate)
        .collectAsState(initial = null)

    val totalCredit by viewModel.getTotalCreditByDateRange(startDate, endDate)
        .collectAsState(initial = null)

    val categoryExpenses by viewModel.getCategoryWiseExpenses(startDate, endDate)
        .collectAsState(initial = emptyMap())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Tracker") },
                actions = {
                    IconButton(onClick = { navController.navigate("gmail_sync") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Gmail Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_transaction") }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month/Year Selection with Dropdowns
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            var expandedMonth by remember { mutableStateOf(false) }
            var expandedYear by remember { mutableStateOf(false) }

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val years = (currentYear - 5..currentYear + 1).toList().reversed()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMonth,
                    onExpandedChange = { expandedMonth = !expandedMonth },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = monthNames[selectedMonth],
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Month") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonth) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        monthNames.forEachIndexed { index, month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    selectedMonth = index
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                // Year Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedYear,
                    onExpandedChange = { expandedYear = !expandedYear },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Year") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedYear) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }

            // Tabs
            val tabs = listOf("Summary", "Calendar")
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            if (index == 1) selectedDate = null // Reset date selection when switching to calendar
                        },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Tab 1: Month Summary View
                    MonthSummaryView(
                        totalDebit = totalDebit ?: 0.0,
                        totalCredit = totalCredit ?: 0.0,
                        categoryExpenses = categoryExpenses,
                        transactions = transactions,
                        navController = navController,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        viewModel = viewModel
                    )
                }
                1 -> {
                    // Tab 2: Calendar View
                    CalendarView(
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        selectedDate = selectedDate,
                        onDateSelected = { day ->
                            selectedDate = if (selectedDate == day) null else day
                        },
                        viewModel = viewModel,
                        repository = repository,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CategoryExpenseCard(
    category: String,
    amount: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "₹${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit = {},
    onDelete: (Transaction) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${dateFormat.format(transaction.date)} • ${transaction.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (transaction.type.name == "DEBIT") "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type.name == "DEBIT")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("✏️", style = MaterialTheme.typography.bodyLarge)
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("🗑️", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

