package com.kishan.expensetracker.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.SharedPreferences
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes

class GmailAuthHelper(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "gmail_auth_prefs",
        Context.MODE_PRIVATE
    )

    private val accountManager: AccountManager = AccountManager.get(context)

    companion object {
        private const val PREF_ACCOUNT_NAME = "account_name"
        private const val PREF_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val GMAIL_SCOPE = GmailScopes.GMAIL_READONLY
    }

    /**
     * Get Google account credential for Gmail API
     */
    fun getCredential(): GoogleAccountCredential {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(GMAIL_SCOPE)
        )

        // Set account if previously selected
        val savedAccountName = prefs.getString(PREF_ACCOUNT_NAME, null)
        if (savedAccountName != null) {
            credential.selectedAccountName = savedAccountName
        }

        return credential
    }

    /**
     * Set the selected Google account
     */
    fun setSelectedAccount(accountName: String) {
        prefs.edit().putString(PREF_ACCOUNT_NAME, accountName).apply()
    }

    /**
     * Get currently selected account name
     */
    fun getSelectedAccountName(): String? {
        return prefs.getString(PREF_ACCOUNT_NAME, null)
    }

    /**
     * Check if user has authenticated
     */
    fun isAuthenticated(): Boolean {
        return getSelectedAccountName() != null
    }

    /**
     * Clear authentication
     */
    fun clearAuthentication() {
        prefs.edit().remove(PREF_ACCOUNT_NAME).apply()
    }

    /**
     * Save last sync timestamp
     */
    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(PREF_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTimestamp(): Long? {
        val timestamp = prefs.getLong(PREF_LAST_SYNC_TIMESTAMP, 0L)
        return if (timestamp > 0) timestamp else null
    }

    /**
     * Get all Google accounts on device
     */
    fun getGoogleAccounts(): List<Account> {
        return try {
            // Get ALL accounts first, then filter for Google accounts
            val allAccounts = accountManager.accounts.toList()

            // Filter for Google accounts by checking:
            // 1. Account type is "com.google"
            // 2. Account name is an email (contains @)
            val googleAccounts = allAccounts.filter { account ->
                account.type == "com.google" ||
                (account.name.contains("@") && (
                    account.name.contains("@gmail.com") ||
                    account.name.contains("@googlemail.com") ||
                    account.name.endsWith("@google.com")
                ))
            }

            // If no accounts found with type "com.google", try all email accounts
            // (some devices might not set the type correctly)
            if (googleAccounts.isEmpty()) {
                allAccounts.filter { account ->
                    account.name.contains("@gmail.com") ||
                    account.name.contains("@googlemail.com") ||
                    account.name.endsWith("@google.com")
                }
            } else {
                googleAccounts
            }.distinctBy { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            // Last resort: return all accounts that look like emails
            try {
                accountManager.accounts.toList().filter {
                    it.name.contains("@")
                }.distinctBy { it.name }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
}

