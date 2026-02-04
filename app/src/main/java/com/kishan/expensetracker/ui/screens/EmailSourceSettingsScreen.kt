package com.kishan.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kishan.expensetracker.data.EmailSourceConfig
import com.kishan.expensetracker.data.entity.TransactionSource
import com.kishan.expensetracker.util.EmailSourceConfigHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSourceSettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val configHelper = remember { EmailSourceConfigHelper(context) }

    var emailSources by remember { mutableStateOf(configHelper.getEmailSources()) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showEditSourceDialog by remember { mutableStateOf<EmailSourceConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Source Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email Sources Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Email Sources",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAddSourceDialog = true }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }
                        }
                        Text(
                            text = "Configure email addresses from which to fetch transaction emails",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // List of configured email sources
            items(emailSources.values.toList()) { config ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.source.name.replace("_", " "),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Emails: ${config.emailAddresses.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Subject keywords: ${config.subjectKeywords.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                val phrases = config.descriptionPhrases ?: emptyList()
                                if (phrases.isNotEmpty()) {
                                    Text(
                                        text = "Description phrases: ${phrases.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    showEditSourceDialog = config
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Edit")
                            }
                            IconButton(
                                onClick = {
                                    configHelper.removeEmailSource(config.source)
                                    emailSources = configHelper.getEmailSources()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }

        }
    }

    // Add/Edit Email Source Dialog
    if (showAddSourceDialog || showEditSourceDialog != null) {
        AddEditEmailSourceDialog(
            existingConfig = showEditSourceDialog,
            onDismiss = {
                showAddSourceDialog = false
                showEditSourceDialog = null
            },
            onSave = { config ->
                configHelper.saveEmailSource(config)
                emailSources = configHelper.getEmailSources()
                showAddSourceDialog = false
                showEditSourceDialog = null
            }
        )
    }

}

// Helper functions to map between Bank/Source and TransactionSource
private fun getBankFromSource(source: TransactionSource): String {
    return when {
        source.name.startsWith("HDFC") -> "HDFC"
        source.name.startsWith("ICICI") -> "ICICI"
        source.name.startsWith("SBI") -> "SBI"
        else -> "HDFC"
    }
}

private fun getSourceTypeFromSource(source: TransactionSource): String {
    return when {
        source.name.contains("UPI") -> "UPI"
        source.name.contains("CREDIT_CARD") -> "Credit Card"
        source.name.contains("DEBIT_CARD") -> "Debit Card"
        else -> "UPI"
    }
}

private fun createTransactionSource(bank: String, sourceType: String): TransactionSource {
    val bankUpper = bank.uppercase()
    val sourceUpper = sourceType.uppercase().replace(" ", "_")

    return when {
        bankUpper == "HDFC" && sourceUpper == "UPI" -> TransactionSource.HDFC_UPI
        bankUpper == "HDFC" && sourceUpper == "CREDIT_CARD" -> TransactionSource.HDFC_CREDIT_CARD
        bankUpper == "ICICI" && sourceUpper == "UPI" -> {
            // ICICI UPI doesn't exist in enum, but we can add it or use a generic approach
            // For now, let's create a new source or use ICICI_CREDIT_CARD
            TransactionSource.ICICI_CREDIT_CARD // Fallback, but ideally we'd add ICICI_UPI
        }
        bankUpper == "ICICI" && sourceUpper == "CREDIT_CARD" -> TransactionSource.ICICI_CREDIT_CARD
        bankUpper == "SBI" && sourceUpper == "UPI" -> TransactionSource.SBI_UPI
        bankUpper == "SBI" && sourceUpper == "CREDIT_CARD" -> {
            // SBI Credit Card doesn't exist, use SBI_UPI as fallback
            TransactionSource.SBI_UPI
        }
        else -> TransactionSource.HDFC_UPI // Default fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmailSourceDialog(
    existingConfig: EmailSourceConfig?,
    onDismiss: () -> Unit,
    onSave: (EmailSourceConfig) -> Unit
) {
    // Initialize bank and source type from existing config or defaults
    val initialBank = remember {
        existingConfig?.let { getBankFromSource(it.source) } ?: "HDFC"
    }
    val initialSourceType = remember {
        existingConfig?.let { getSourceTypeFromSource(it.source) } ?: "UPI"
    }

    var selectedBank by remember { mutableStateOf(initialBank) }
    var selectedSourceType by remember { mutableStateOf(initialSourceType) }
    var emailAddressesText by remember { mutableStateOf(existingConfig?.emailAddresses?.joinToString(", ") ?: "") }
    var subjectKeywordsText by remember { mutableStateOf(existingConfig?.subjectKeywords?.joinToString(", ") ?: "") }
    var descriptionPhrasesText by remember { mutableStateOf(existingConfig?.descriptionPhrases?.joinToString(", ") ?: "") }

    // Compute TransactionSource from bank and source type
    val computedSource = remember(selectedBank, selectedSourceType) {
        createTransactionSource(selectedBank, selectedSourceType)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingConfig != null) "Edit Email Source" else "Add Email Source") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bank selection
                var expandedBank by remember { mutableStateOf(false) }
                val banks = listOf("HDFC", "ICICI", "SBI")

                ExposedDropdownMenuBox(
                    expanded = expandedBank,
                    onExpandedChange = { expandedBank = !expandedBank },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedBank,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Bank") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBank) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBank,
                        onDismissRequest = { expandedBank = false }
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank) },
                                onClick = {
                                    selectedBank = bank
                                    expandedBank = false
                                }
                            )
                        }
                    }
                }

                // Source Type selection
                var expandedSourceType by remember { mutableStateOf(false) }
                val sourceTypes = listOf("UPI", "Credit Card", "Debit Card")

                ExposedDropdownMenuBox(
                    expanded = expandedSourceType,
                    onExpandedChange = { expandedSourceType = !expandedSourceType },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSourceType,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Source Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSourceType) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSourceType,
                        onDismissRequest = { expandedSourceType = false }
                    ) {
                        sourceTypes.forEach { sourceType ->
                            DropdownMenuItem(
                                text = { Text(sourceType) },
                                onClick = {
                                    selectedSourceType = sourceType
                                    expandedSourceType = false
                                }
                            )
                        }
                    }
                }

                // Email addresses
                OutlinedTextField(
                    value = emailAddressesText,
                    onValueChange = { emailAddressesText = it },
                    label = { Text("Email Addresses (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., noreply@hdfcbank.net, alerts@hdfcbank.net") }
                )

                // Subject keywords
                OutlinedTextField(
                    value = subjectKeywordsText,
                    onValueChange = { subjectKeywordsText = it },
                    label = { Text("Subject Keywords (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., UPI, credit, transaction") }
                )

                // Description phrases
                OutlinedTextField(
                    value = descriptionPhrasesText,
                    onValueChange = { descriptionPhrasesText = it },
                    label = { Text("Description Phrases (comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., transaction of, amount paid, info:, towards") },
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val emails = emailAddressesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val keywords = subjectKeywordsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val phrases = descriptionPhrasesText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (emails.isNotEmpty()) {
                        onSave(
                            EmailSourceConfig(
                                source = computedSource,
                                emailAddresses = emails,
                                subjectKeywords = keywords,
                                descriptionPhrases = phrases
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

