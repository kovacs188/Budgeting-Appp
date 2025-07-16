package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    monthViewModel: MonthViewModel = hiltViewModel()
) {
    val uiState by monthViewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.currentMonth?.displayName ?: "Monthly Budget",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            item {
                // Budget Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Budget Summary",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BudgetSummaryItem(
                                title = "Income",
                                amount = uiState.totalIncome,
                                color = MaterialTheme.colorScheme.primary
                            )
                            BudgetSummaryItem(
                                title = "Expenses",
                                amount = uiState.totalExpenses,
                                color = MaterialTheme.colorScheme.error
                            )
                            BudgetSummaryItem(
                                title = "Remaining",
                                amount = uiState.remainingBudget,
                                color = if (uiState.remainingBudget >= 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Income Section
            item {
                SectionHeader("Income")
            }

            if (uiState.incomeCategories.isEmpty()) {
                item {
                    CategoryPlaceholder("No income categories yet")
                }
            } else {
                items(uiState.incomeCategories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Fixed Expenses Section
            item {
                SectionHeader("Fixed Expenses")
            }

            if (uiState.fixedExpenseCategories.isEmpty()) {
                item {
                    CategoryPlaceholder("No fixed expense categories yet")
                }
            } else {
                items(uiState.fixedExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Variable Expenses Section
            item {
                SectionHeader("Variable Expenses")
            }

            if (uiState.variableExpenseCategories.isEmpty()) {
                item {
                    CategoryPlaceholder("No variable expense categories yet")
                }
            } else {
                items(uiState.variableExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Discretionary Expenses Section
            item {
                SectionHeader("Discretionary Expenses")
            }

            if (uiState.discretionaryExpenseCategories.isEmpty()) {
                item {
                    CategoryPlaceholder("No discretionary expense categories yet")
                }
            } else {
                items(uiState.discretionaryExpenseCategories) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }
    }

    // Transaction Entry Dialog
    selectedCategory?.let { category ->
        TransactionEntryDialog(
            category = category,
            onDismiss = { selectedCategory = null },
            onTransactionAdded = {
                selectedCategory = null
                monthViewModel.refreshMonth()
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (category.description.isNotEmpty()) {
                        Text(
                            text = category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", category.targetAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "budgeted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress indicator with real data
            val progress = if (category.targetAmount > 0) {
                (category.actualAmount / category.targetAmount).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    category.type == CategoryType.INCOME -> {
                        if (category.actualAmount >= category.targetAmount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    }
                    category.isOverBudget -> MaterialTheme.colorScheme.error
                    progress > 0.8f -> MaterialTheme.colorScheme.tertiary // Warning color
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (category.type == CategoryType.INCOME) "Earned" else "Spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%.2f", category.actualAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (category.type == CategoryType.INCOME) "Still needed" else "Remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$${String.format("%.2f", kotlin.math.abs(category.remainingAmount))}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (category.type == CategoryType.INCOME) {
                        if (category.actualAmount >= category.targetAmount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    } else {
                        if (category.actualAmount <= category.targetAmount)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    }
                )
            }

            // Budget status indicator
            if (category.actualAmount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        category.type == CategoryType.INCOME && category.actualAmount >= category.targetAmount ->
                            "✅ Income goal reached!"
                        category.type == CategoryType.INCOME ->
                            "⏳ ${String.format("%.1f", (category.actualAmount / category.targetAmount) * 100.0)}% of income goal"
                        category.isOverBudget ->
                            "⚠️ Over budget by $${String.format("%.2f", category.actualAmount - category.targetAmount)}"
                        progress > 0.8f ->
                            "⚡ ${String.format("%.1f", progress.toDouble() * 100.0)}% of budget used"
                        else ->
                            "👍 ${String.format("%.1f", progress.toDouble() * 100.0)}% of budget used"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        category.type == CategoryType.INCOME && category.actualAmount >= category.targetAmount ->
                            MaterialTheme.colorScheme.primary
                        category.type == CategoryType.INCOME ->
                            MaterialTheme.colorScheme.error
                        category.isOverBudget ->
                            MaterialTheme.colorScheme.error
                        progress > 0.8f ->
                            MaterialTheme.colorScheme.tertiary
                        else ->
                            MaterialTheme.colorScheme.primary
                    }
                )
            }

            // Tap hint
            if (category.actualAmount == 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Tap to add ${if (category.type == CategoryType.INCOME) "income" else "expense"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun BudgetSummaryItem(
    title: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun CategoryPlaceholder(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to create your first category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}