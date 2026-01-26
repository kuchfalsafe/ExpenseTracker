package com.kishan.expensetracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kishan.expensetracker.ExpenseTrackerApp
import com.kishan.expensetracker.data.entity.Transaction
import com.kishan.expensetracker.data.entity.TransactionSource
import com.kishan.expensetracker.data.entity.TransactionType
import com.kishan.expensetracker.ui.components.DatePickerDialog
import com.kishan.expensetracker.viewmodel.CategoryViewModel
import com.kishan.expensetracker.viewmodel.CategoryViewModelFactory
import com.kishan.expensetracker.viewmodel.TransactionViewModel
import com.kishan.expensetracker.viewmodel.TransactionViewModelFactory
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    navController: NavController,
    transactionId: Long? = null
) {
    val context = LocalContext.current
    val transactionViewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            ExpenseTrackerApp.getRepository(context)
        )
    )
    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModelFactory(
            ExpenseTrackerApp.getRepository(context)
        )
    )
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.DEBIT) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val categories by categoryViewModel.allCategories.collectAsState()
    val allTransactions by transactionViewModel.allTransactions.collectAsState()

    // Load transaction if editing
    val existingTransaction = remember(transactionId, allTransactions) {
        if (transactionId != null && transactionId > 0) {
            allTransactions.find { it.id == transactionId }
        } else {
            null
        }
    }

    // Initialize fields with existing transaction data if editing
    LaunchedEffect(existingTransaction) {
        existingTransaction?.let { transaction ->
            amount = transaction.amount.toString()
            description = transaction.description
            selectedCategory = transaction.category
            selectedType = transaction.type
            selectedDate = transaction.date
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId != null && transactionId > 0) "Edit Transaction" else "Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showCategoryDialog = true }) {
                            Text("Select")
                        }
                    }
                )
            }

            // Transaction Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TransactionType.DEBIT,
                    onClick = { selectedType = TransactionType.DEBIT },
                    label = { Text("Debit") }
                )
                FilterChip(
                    selected = selectedType == TransactionType.CREDIT,
                    onClick = { selectedType = TransactionType.CREDIT },
                    label = { Text("Credit") }
                )
            }

            // Date
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            OutlinedTextField(
                value = dateFormat.format(selectedDate),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Text("📅")
                    }
                }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    onDateSelected = { dateMillis ->
                        dateMillis?.let {
                            selectedDate = Date(it)
                            showDatePicker = false
                        }
                    },
                    initialDate = selectedDate.time
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && selectedCategory.isNotEmpty()) {
                        val transaction = if (transactionId != null && transactionId > 0 && existingTransaction != null) {
                            // Update existing transaction
                            existingTransaction.copy(
                                date = selectedDate,
                                amount = amountValue,
                                category = selectedCategory,
                                type = selectedType,
                                description = description
                            )
                        } else {
                            // Create new transaction
                            Transaction(
                                date = selectedDate,
                                amount = amountValue,
                                category = selectedCategory,
                                type = selectedType,
                                source = TransactionSource.MANUAL,
                                description = description,
                                isManual = true
                            )
                        }

                        if (transactionId != null && transactionId > 0) {
                            transactionViewModel.updateTransaction(transaction)
                        } else {
                            transactionViewModel.insertTransaction(transaction)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull() != null && selectedCategory.isNotEmpty()
            ) {
                Text(if (transactionId != null && transactionId > 0) "Update Transaction" else "Save Transaction")
            }
        }
    }

    // Category Selection Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category") },
            text = {
                Column {
                    categories.forEach { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category.name
                                    showCategoryDialog = false
                                }
                        )
                        Divider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

