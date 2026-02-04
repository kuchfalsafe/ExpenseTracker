package com.kishan.expensetracker.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kishan.expensetracker.data.EmailSourceConfig
import com.kishan.expensetracker.data.entity.TransactionSource

class EmailSourceConfigHelper(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "email_source_config",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val PREF_EMAIL_SOURCES = "email_sources"

        // Default email sources (for backward compatibility)
        private val DEFAULT_CONFIGS = mapOf(
            TransactionSource.HDFC_UPI to EmailSourceConfig(
                source = TransactionSource.HDFC_UPI,
                emailAddresses = listOf("noreply@hdfcbank.net", "alerts@hdfcbank.net"),
                subjectKeywords = listOf("UPI"),
                descriptionPhrases = null
            ),
            TransactionSource.HDFC_CREDIT_CARD to EmailSourceConfig(
                source = TransactionSource.HDFC_CREDIT_CARD,
                emailAddresses = listOf("noreply@hdfcbank.net", "alerts@hdfcbank.net"),
                subjectKeywords = listOf("credit"),
                descriptionPhrases = null
            ),
            TransactionSource.ICICI_CREDIT_CARD to EmailSourceConfig(
                source = TransactionSource.ICICI_CREDIT_CARD,
                emailAddresses = listOf("credit_cards@icicibank.com"),
                subjectKeywords = listOf("transaction"),
                descriptionPhrases = null
            ),
            TransactionSource.SBI_UPI to EmailSourceConfig(
                source = TransactionSource.SBI_UPI,
                emailAddresses = listOf("cbsalerts.sbi@alerts.sbi.co.in", "donotreply.sbiatm@alerts.sbi.co.in"),
                subjectKeywords = listOf("UPI"),
                descriptionPhrases = null
            )
        )
    }

    /**
     * Get all configured email sources
     */
    fun getEmailSources(): Map<TransactionSource, EmailSourceConfig> {
        val json = prefs.getString(PREF_EMAIL_SOURCES, null)
        return if (json != null && json.isNotEmpty()) {
            try {
                val type = object : TypeToken<Map<String, EmailSourceConfig>>() {}.type
                val map: Map<String, EmailSourceConfig> = gson.fromJson(json, type)
                map.mapKeys { TransactionSource.valueOf(it.key) }
            } catch (e: Exception) {
                e.printStackTrace()
                DEFAULT_CONFIGS
            }
        } else {
            DEFAULT_CONFIGS
        }
    }

    /**
     * Save email source configuration
     */
    fun saveEmailSource(config: EmailSourceConfig) {
        val currentConfigs = getEmailSources().toMutableMap()
        currentConfigs[config.source] = config

        val mapAsString = currentConfigs.mapKeys { it.key.name }
        val json = gson.toJson(mapAsString)
        prefs.edit().putString(PREF_EMAIL_SOURCES, json).apply()
    }

    /**
     * Remove email source configuration (revert to default)
     */
    fun removeEmailSource(source: TransactionSource) {
        val currentConfigs = getEmailSources().toMutableMap()
        currentConfigs.remove(source)

        if (currentConfigs.isEmpty()) {
            prefs.edit().remove(PREF_EMAIL_SOURCES).apply()
        } else {
            val mapAsString = currentConfigs.mapKeys { it.key.name }
            val json = gson.toJson(mapAsString)
            prefs.edit().putString(PREF_EMAIL_SOURCES, json).apply()
        }
    }

}

