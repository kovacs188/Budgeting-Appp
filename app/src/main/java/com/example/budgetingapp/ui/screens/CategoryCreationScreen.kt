package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCreationScreen(
    onNavigateBack: () -> Unit,
    onCategoryCreated: () -> Unit,
    existingCategory: Category? = null,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState
    val isEditMode = existingCategory != null

    // Initialize the form
    LaunchedEffect(existingCategory) {
        if (existingCategory != null) {
            viewModel.initializeForEdit(existingCategory)
        } else {
            viewModel.initializeForCreate()
        }
    }

    // Handle success
    LaunchedEffect(uiState.categoryCreated, uiState.categoryUpdated) {
        if (uiState.categoryCreated || uiState.categoryUpdated) {
            onCategoryCreated()
            viewModel.markCategoryCreatedHandled()
            viewModel.markCategoryUpdatedHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Category" else "Create Category",
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
                actions = {
                    IconButton(
                        onClick = { viewModel.saveCategory() },
                        enabled = formState.isValid && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save Category"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            uiState.errorMessage?.let { errorMessage ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Category Name Input
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Category Name") },
                placeholder = { Text("e.g., Groceries, Salary, Rent") },
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Category Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CategoryType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formState.type == type,
                                onClick = { viewModel.updateType(type) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = getCategoryTypeDisplay(type),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = getCategoryTypeDescription(type),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Target Amount Input
            OutlinedTextField(
                value = formState.targetAmount,
                onValueChange = { viewModel.updateTargetAmount(it) },
                label = { Text(getAmountLabel(formState.type)) },
                placeholder = { Text("0.00") },
                leadingIcon = { Text("$") },
                isError = formState.amountError != null,
                supportingText = formState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Description Input (Optional)
            OutlinedTextField(
                value = formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add notes about this category...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Save Button
            Button(
                onClick = { viewModel.saveCategory() },
                enabled = formState.isValid && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isEditMode) "Update Category" else "Create Category",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Help Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Tip",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (formState.type) {
                            CategoryType.INCOME -> "Set your expected income amount. Earning more than this target is great!"
                            CategoryType.FIXED_EXPENSE -> "Fixed expenses are automatically marked as 'spent' since they're predictable and consistent (rent, subscriptions, loans)."
                            CategoryType.VARIABLE_EXPENSE -> "Variable expenses change monthly but are necessary (utilities, groceries). Track actual spending against your budget."
                            CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary expenses are optional spending you can control (dining out, entertainment). Track to stay within budget."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCategoryTypeDisplay(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income"
        CategoryType.FIXED_EXPENSE -> "Fixed Expense"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expense"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense"
    }
}

private fun getCategoryTypeDescription(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Salary, freelance, investments"
        CategoryType.FIXED_EXPENSE -> "Rent, subscriptions, loan payments"
        CategoryType.VARIABLE_EXPENSE -> "Utilities, groceries, maintenance"
        CategoryType.DISCRETIONARY_EXPENSE -> "Dining out, entertainment, hobbies"
    }
}

private fun getAmountLabel(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Expected Income Amount"
        else -> "Budget Amount"
    }
}