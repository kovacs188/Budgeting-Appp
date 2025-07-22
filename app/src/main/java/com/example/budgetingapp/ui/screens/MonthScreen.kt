package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.ui.components.BudgetSummaryCard
import com.example.budgetingapp.ui.components.CategoryCard
import com.example.budgetingapp.ui.components.CategoryPlaceholder
import com.example.budgetingapp.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    monthId: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTransactionHistory: (String) -> Unit,
    monthViewModel: MonthViewModel = hiltViewModel()
) {
    val uiState by monthViewModel.uiState.collectAsState()
    var selectedCategoryForQuickAdd by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { monthViewModel.navigateToPreviousMonth() }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Text(
                            text = uiState.currentMonth?.displayName ?: "Monthly Budget",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { monthViewModel.navigateToNextMonth() }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Months"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "All Months"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCategories,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Category"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Budget Summary
            item {
                BudgetSummaryCard(
                    totalIncome = uiState.totalIncome,
                    totalExpenses = uiState.totalExpenses,
                    remainingBudget = uiState.remainingBudget
                )
            }

            // Income Section
            item { SectionHeader("Income") }
            if (uiState.incomeCategories.isEmpty()) {
                item { CategoryPlaceholder("No income categories yet") }
            } else {
                items(uiState.incomeCategories) { category ->
                    CategoryCard(
                        category = category,
                        onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                        onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                    )
                }
            }

            // Fixed Expenses Section
            item { SectionHeader("Fixed Expenses") }
            if (uiState.fixedExpenseCategories.isEmpty()) {
                item { CategoryPlaceholder("No fixed expense categories yet") }
            } else {
                items(uiState.fixedExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                        onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                    )
                }
            }

            // Variable Expenses Section
            item { SectionHeader("Variable Expenses") }
            if (uiState.variableExpenseCategories.isEmpty()) {
                item { CategoryPlaceholder("No variable expense categories yet") }
            } else {
                items(uiState.variableExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                        onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                    )
                }
            }

            // Discretionary Expenses Section
            item { SectionHeader("Discretionary Expenses") }
            if (uiState.discretionaryExpenseCategories.isEmpty()) {
                item { CategoryPlaceholder("No discretionary expense categories yet") }
            } else {
                items(uiState.discretionaryExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                        onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                    )
                }
            }
        }
    }

    // Quick Add Transaction Dialog
    selectedCategoryForQuickAdd?.let { category ->
        TransactionEntryDialog(
            category = category,
            onDismiss = { selectedCategoryForQuickAdd = null },
            onTransactionAdded = {
                selectedCategoryForQuickAdd = null
                monthViewModel.refreshMonth()
            }
        )
    }
}