package com.kishan.expensetracker.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.gmail.GmailScopes
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException
import com.kishan.expensetracker.ExpenseTrackerApp
import com.kishan.expensetracker.auth.GmailAuthHelper
import com.kishan.expensetracker.util.GmailTransactionScraper
import com.kishan.expensetracker.viewmodel.TransactionViewModel
import com.kishan.expensetracker.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import android.accounts.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmailSyncScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModelFactory(
            ExpenseTrackerApp.getRepository(context)
        )
    )
    val authHelper = remember { GmailAuthHelper(context) }
    val scope = rememberCoroutineScope()

    var selectedAccount by remember { mutableStateOf<String?>(authHelper.getSelectedAccountName()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    // Date range selection
    var selectedRangeType by remember { mutableStateOf("days") } // "days" or "months"
    var selectedRangeValue by remember { mutableStateOf(30) } // number of days/months

    var googleAccounts by remember { mutableStateOf<List<android.accounts.Account>>(emptyList()) }
    val credential = remember { authHelper.getCredential() }

    // Account picker launcher (also used for permission consent)
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // After consent, the account should be set in credential
            val accountName = credential.selectedAccountName ?: selectedAccount
            if (accountName != null && accountName.isNotEmpty()) {
                selectedAccount = accountName
                authHelper.setSelectedAccount(accountName)
                // Refresh accounts list
                googleAccounts = authHelper.getGoogleAccounts()

                // If we were syncing, retry the sync
                if (isSyncing) {
                    scope.launch {
                        // Retry sync after permission granted
                        try {
                            val daysToSync = if (selectedRangeType == "days") {
                                selectedRangeValue
                            } else {
                                selectedRangeValue * 30
                            }
                            val scraper = GmailTransactionScraper(context)
                            val transactions = scraper.scrapeTransactions(accountName, daysToSync)
                            transactions.forEach { transaction ->
                                viewModel.insertTransaction(transaction)
                            }
                            syncStatus = "Successfully synced ${transactions.size} transactions"
                            isSyncing = false
                        } catch (e: Exception) {
                            syncError = "Sync failed after permission grant: ${e.message}"
                            isSyncing = false
                        }
                    }
                }
            }
        } else {
            // User cancelled or denied permission
            if (isSyncing) {
                syncError = "Permission denied. Please grant Gmail access to sync transactions."
                isSyncing = false
            }
        }
    }

    // Load accounts when screen is displayed
    LaunchedEffect(Unit) {
        googleAccounts = authHelper.getGoogleAccounts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gmail Sync") },
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
            // Account Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Google Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Refresh button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    googleAccounts = authHelper.getGoogleAccounts()
                                }
                            }
                        ) {
                            Text("Refresh")
                        }
                    }

                    if (googleAccounts.isEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No Google accounts detected automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "You can manually enter your Gmail address below, or tap Refresh to try again.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Use Google Account Picker
                            Button(
                                onClick = {
                                    try {
                                        val pickerIntent = credential.newChooseAccountIntent()
                                        accountPickerLauncher.launch(pickerIntent)
                                    } catch (e: Exception) {
                                        syncError = "Failed to open account picker: ${e.message}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choose Google Account")
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Manual account entry (fallback)
                            var manualAccount by remember { mutableStateOf("") }
                            Text(
                                text = "Or enter manually:",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            OutlinedTextField(
                                value = manualAccount,
                                onValueChange = { manualAccount = it },
                                label = { Text("Enter Gmail Address") },
                                placeholder = { Text("your.email@gmail.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (manualAccount.isNotEmpty() && manualAccount.contains("@")) {
                                        selectedAccount = manualAccount
                                        authHelper.setSelectedAccount(manualAccount)
                                        credential.selectedAccountName = manualAccount
                                        googleAccounts = listOf(Account(manualAccount, "com.google"))
                                    } else {
                                        syncError = "Please enter a valid email address"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = manualAccount.isNotEmpty()
                            ) {
                                Text("Use This Account")
                            }
                        }
                    } else {
                        googleAccounts.forEach { account ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = account.type,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = selectedAccount == account.name,
                                    onClick = {
                                        selectedAccount = account.name
                                        authHelper.setSelectedAccount(account.name)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Date Range Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sync Period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Select how many past days/months to sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Range Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRangeType == "days",
                            onClick = { selectedRangeType = "days" },
                            label = { Text("Days") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedRangeType == "months",
                            onClick = { selectedRangeType = "months" },
                            label = { Text("Months") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Range Value Selection
                    val rangeOptions = if (selectedRangeType == "days") {
                        listOf(7, 15, 30, 60, 90, 180, 365)
                    } else {
                        listOf(1, 3, 6, 12, 24)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rangeOptions.forEach { value ->
                            FilterChip(
                                selected = selectedRangeValue == value,
                                onClick = { selectedRangeValue = value },
                                label = {
                                    Text(
                                        if (selectedRangeType == "days") "$value days"
                                        else "$value ${if (value == 1) "month" else "months"}"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Display selected range
                    Text(
                        text = "Will sync transactions from the past $selectedRangeValue ${if (selectedRangeType == "days") "days" else if (selectedRangeValue == 1) "month" else "months"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Authentication Status
            if (selectedAccount != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Authenticated",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedAccount ?: "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(
                            onClick = {
                                authHelper.clearAuthentication()
                                selectedAccount = null
                            }
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            // Sync Button
            Button(
                onClick = {
                    if (selectedAccount == null) {
                        syncError = "Please select a Google account first"
                        return@Button
                    }

                    isSyncing = true
                    syncStatus = null
                    syncError = null

                    scope.launch {
                        try {
                            // Ensure account is selected
                            val accountToUse = selectedAccount
                            if (accountToUse == null || accountToUse.isEmpty()) {
                                syncError = "Please select a Google account first"
                                isSyncing = false
                                return@launch
                            }

                            // Set account in credential and auth helper BEFORE creating scraper
                            credential.selectedAccountName = accountToUse
                            authHelper.setSelectedAccount(accountToUse)

                            // Verify account is set
                            if (credential.selectedAccountName == null || credential.selectedAccountName!!.isEmpty()) {
                                syncError = "Failed to set account. Please try selecting the account again."
                                isSyncing = false
                                return@launch
                            }

                            // Calculate days to sync
                            val daysToSync = if (selectedRangeType == "days") {
                                selectedRangeValue
                            } else {
                                selectedRangeValue * 30 // Approximate months to days
                            }

                            // Create scraper and pass the account name explicitly
                            // Make sure accountToUse is not null/empty at this point
                            if (accountToUse.isBlank()) {
                                syncError = "Account name is empty. Please select a Google account again."
                                isSyncing = false
                                return@launch
                            }

                            val scraper = GmailTransactionScraper(context)
                            // Pass the account name explicitly - ensure it's not null
                            val transactions = scraper.scrapeTransactions(accountToUse, daysToSync)

                            // Insert transactions
                            transactions.forEach { transaction ->
                                viewModel.insertTransaction(transaction)
                            }

                            syncStatus = "Successfully synced ${transactions.size} transactions from the past $selectedRangeValue ${if (selectedRangeType == "days") "days" else if (selectedRangeValue == 1) "month" else "months"}"
                            isSyncing = false
                        } catch (e: UserRecoverableAuthException) {
                            // User needs to grant permission - launch intent
                            syncError = "Please grant Gmail access permission"
                            accountPickerLauncher.launch(e.intent)
                            isSyncing = false
                        } catch (e: UserRecoverableAuthIOException) {
                            // UserRecoverableAuthIOException wraps UserRecoverableAuthException
                            // We need to extract the intent to show the consent screen
                            val cause = e.cause
                            if (cause is UserRecoverableAuthException) {
                                syncError = "Please grant Gmail access permission to sync transactions"
                                accountPickerLauncher.launch(cause.intent)
                            } else {
                                // If we can't get the intent, show a helpful message
                                syncError = "Please grant Gmail access permission.\n\n" +
                                        "The app needs permission to read your Gmail to sync transactions.\n" +
                                        "Tap 'Sync Now' again to see the permission dialog."
                            }
                            isSyncing = false
                        } catch (e: GooglePlayServicesAvailabilityException) {
                            syncError = "Google Play Services not available. Please update Google Play Services."
                            isSyncing = false
                        } catch (e: GoogleAuthException) {
                            // Check for UnregisteredOnApiConsole error
                            val errorMessage = if (e.message?.contains("UnregisteredOnApiConsole", ignoreCase = true) == true) {
                                "App not registered in Google Cloud Console.\n\n" +
                                "Required setup:\n" +
                                "1. Go to Google Cloud Console > APIs & Services > Credentials\n" +
                                "2. Create OAuth 2.0 Client ID (Android type)\n" +
                                "3. Package name: com.kishan.expensetracker\n" +
                                "4. SHA-1: AE:D8:C6:38:C7:B3:65:63:82:35:55:68:23:90:91:AD:F2:1A:0B:0B\n" +
                                "5. Enable Gmail API\n\n" +
                                "See GMAIL_SETUP.md for detailed instructions."
                            } else {
                                "Authentication failed: ${e.message}. Please try selecting the account again."
                            }
                            syncError = errorMessage
                            isSyncing = false
                        } catch (e: GoogleAuthIOException) {
                            // Handle GoogleAuthIOException which wraps GoogleAuthException
                            val cause = e.cause
                            if (cause is GoogleAuthException && cause.message?.contains("UnregisteredOnApiConsole", ignoreCase = true) == true) {
                                syncError = "App not registered in Google Cloud Console.\n\n" +
                                        "Required setup:\n" +
                                        "1. Create OAuth 2.0 Client ID in Google Cloud Console\n" +
                                        "2. Package name: com.kishan.expensetracker\n" +
                                        "3. SHA-1: AE:D8:C6:38:C7:B3:65:63:82:35:55:68:23:90:91:AD:F2:1A:0B:0B\n" +
                                        "4. Enable Gmail API\n\n" +
                                        "See GMAIL_SETUP.md for step-by-step instructions."
                            } else {
                                syncError = "Authentication error: ${cause?.message ?: e.message}. Please check GMAIL_SETUP.md for setup instructions."
                            }
                            isSyncing = false
                        } catch (e: Exception) {
                            syncError = "Sync failed: ${e.message ?: e.javaClass.simpleName}\n\nIf this is an authentication error, please check GMAIL_SETUP.md for setup instructions."
                            e.printStackTrace()
                            isSyncing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSyncing && selectedAccount != null
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...")
                } else {
                    Text("Sync Now")
                }
            }

            // Sync Status
            syncStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error Message
            syncError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Select your Google account\n" +
                                "• Choose how many past days/months to sync\n" +
                                "• Tap 'Sync Now' to fetch transactions from Gmail\n" +
                                "• Transactions from HDFC, ICICI, and SBI will be automatically imported\n" +
                                "• A daily job will sync new transactions automatically (last 7 days)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

