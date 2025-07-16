package com.example.budgetingapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.budgetingapp.ui.screens.HomeScreen
import com.example.budgetingapp.ui.screens.MonthScreen
import com.example.budgetingapp.ui.screens.CategoryCreationScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object MonthView : Screen("month_view")
    object CategoryCreator : Screen("category_creator")
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
                    navController.navigate(Screen.MonthView.route)
                }
            )
        }

        composable(Screen.MonthView.route) {
            MonthScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCategories = {
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
                    // Navigate back to month view after creating category
                    navController.popBackStack()
                }
            )
        }
    }
}