package com.kishan.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kishan.expensetracker.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("add_transaction") {
            AddTransactionScreen(navController = navController)
        }
        composable("edit_transaction/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.toLongOrNull() ?: 0L
            AddTransactionScreen(
                navController = navController,
                transactionId = transactionId
            )
        }
        composable("add_category") {
            AddCategoryScreen(navController = navController)
        }
        composable("gmail_sync") {
            GmailSyncScreen(navController = navController)
        }
        composable("email_source_settings") {
            EmailSourceSettingsScreen(navController = navController)
        }
        composable("category_expenses/{category}/{period}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val period = backStackEntry.arguments?.getString("period") ?: "daily"
            CategoryExpensesScreen(
                navController = navController,
                category = category,
                period = period
            )
        }
    }
}

