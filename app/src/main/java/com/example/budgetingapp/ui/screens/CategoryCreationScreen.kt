package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCreationScreen(
    onNavigateBack: () -> Unit,
    onCategoryCreated: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Category",
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
                        onClick = {
                            viewModel.createCategory()
                            if (formState.isValid) {
                                onCategoryCreated()
                            }
                        },
                        enabled = formState.isValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Category"
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
                onClick = {
                    viewModel.createCategory()
                    if (formState.isValid) {
                        onCategoryCreated()
                    }
                },
                enabled = formState.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Create Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
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
                            CategoryType.FIXED_EXPENSE -> "Fixed expenses stay the same each month (rent, subscriptions, loans)."
                            CategoryType.VARIABLE_EXPENSE -> "Variable expenses change monthly but are necessary (utilities, groceries)."
                            CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary expenses are optional spending you can control (dining out, entertainment)."
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