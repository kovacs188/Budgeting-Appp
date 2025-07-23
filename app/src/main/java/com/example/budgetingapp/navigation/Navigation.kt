package com.example.budgetingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.screens.CategoryCreationScreen
import com.example.budgetingapp.ui.screens.CategoryTypeDetailsScreen
import com.example.budgetingapp.ui.screens.HomeScreen
import com.example.budgetingapp.ui.screens.TransactionHistoryScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CategoryCreator : Screen("category_creator")
    object TransactionHistory : Screen("transaction_history/{categoryId}") {
        fun createRoute(categoryId: String) = "transaction_history/$categoryId"
    }
    object CategoryTypeDetails : Screen("category_type_details/{categoryType}") {
        fun createRoute(categoryType: CategoryType) = "category_type_details/${categoryType.name}"
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
                onNavigateToCategoryTypeDetails = { categoryType: CategoryType ->
                    navController.navigate(Screen.CategoryTypeDetails.createRoute(categoryType))
                },
                onNavigateToCategoryCreator = {
                    navController.navigate(Screen.CategoryCreator.route)
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
                onNavigateToEditCategory = { catId ->
                    navController.navigate(Screen.CategoryCreator.route + "?categoryId=$catId")
                }
            )
        }

        // ** THE FIX IS HERE **
        // The route now accepts an optional categoryType parameter
        composable(Screen.CategoryCreator.route + "?categoryId={categoryId}&categoryType={categoryType}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            val categoryType = backStackEntry.arguments?.getString("categoryType")
            CategoryCreationScreen(
                categoryId = categoryId,
                defaultCategoryType = categoryType, // Pass it to the screen
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCategorySaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CategoryTypeDetails.route) { backStackEntry ->
            val categoryTypeString = backStackEntry.arguments?.getString("categoryType") ?: ""
            val categoryType = CategoryType.valueOf(categoryTypeString)
            CategoryTypeDetailsScreen(
                categoryType = categoryType,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTransactionHistory = { categoryId ->
                    navController.navigate(Screen.TransactionHistory.createRoute(categoryId))
                },
                onNavigateToCategoryCreator = {
                    // Pass the current category type when navigating
                    navController.navigate(Screen.CategoryCreator.route + "?categoryType=${categoryType.name}")
                }
            )
        }
    }
}
