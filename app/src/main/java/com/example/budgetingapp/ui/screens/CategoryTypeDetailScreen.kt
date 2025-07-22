package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.components.CategoryCard
import com.example.budgetingapp.ui.components.CategoryPlaceholder
import com.example.budgetingapp.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTypeDetailsScreen(
    categoryType: CategoryType,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionHistory: (String) -> Unit,
    onNavigateToCategoryCreator: () -> Unit,
    viewModel: CategoryTypeDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategoryForQuickAdd by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(categoryType) {
        viewModel.loadCategories(categoryType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = getCategoryTypeDisplayName(categoryType),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
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
                onClick = onNavigateToCategoryCreator,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Category"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message
            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(getCategoryTypeDisplayName(categoryType))
                }

                if (uiState.categories.isEmpty()) {
                    item {
                        CategoryPlaceholder("No ${getCategoryTypeDisplayName(categoryType).lowercase()} categories yet")
                    }
                } else {
                    items(uiState.categories) { category ->
                        CategoryCard(
                            category = category,
                            onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                            onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
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
                viewModel.refreshCategories(categoryType)
            }
        )
    }
}

// Helper function to get display names for category types
private fun getCategoryTypeDisplayName(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income Categories"
        CategoryType.FIXED_EXPENSE -> "Fixed Expense Categories"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expense Categories"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense Categories"
    }
}