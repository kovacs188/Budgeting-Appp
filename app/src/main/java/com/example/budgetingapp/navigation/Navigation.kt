package com.example.budgetingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budgetingapp.ui.screens.CategoryCreationScreen
import com.example.budgetingapp.ui.screens.HomeScreen
import com.example.budgetingapp.ui.screens.MonthScreen
import com.example.budgetingapp.ui.screens.MonthsListScreen
import com.example.budgetingapp.ui.screens.TransactionHistoryScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CurrentMonth : Screen("current_month/{monthId}") {
        fun createRoute(monthId: String) = "current_month/$monthId"
    }
    object MonthsList : Screen("months_list")
    object CategoryCreator : Screen("category_creator")
    object TransactionHistory : Screen("transaction_history/{categoryId}") {
        fun createRoute(categoryId: String) = "transaction_history/$categoryId"
    }
}

@Composable
fun BudgetNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMonthView = {
                    navController.navigate(Screen.MonthsList.route)
                }
            )
        }

        composable(Screen.CurrentMonth.route) { backStackEntry ->
            val monthId = backStackEntry.arguments?.getString("monthId") ?: ""
            MonthScreen(
                monthId = monthId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.CategoryCreator.route)
                },
                onNavigateToTransactionHistory = { categoryId ->
                    navController.navigate(Screen.TransactionHistory.createRoute(categoryId))
                }
            )
        }

        composable(Screen.MonthsList.route) {
            MonthsListScreen(
                onNavigateToHome = {
                    navController.popBackStack()
                },
                onNavigateToMonth = { monthId ->
                    navController.navigate(Screen.CurrentMonth.createRoute(monthId))
                }
            )
        }

        composable(Screen.TransactionHistory.route) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            TransactionHistoryScreen(
                categoryId = categoryId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditCategory = { categoryId ->
                    // For now, just navigate to the category creator
                    // We'll pass the category through the viewModel
                    navController.navigate(Screen.CategoryCreator.route)
                }
            )
        }

        composable(Screen.CategoryCreator.route) {
            CategoryCreationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCategoryCreated = {
                    navController.popBackStack()
                }
            )
        }
    }
}