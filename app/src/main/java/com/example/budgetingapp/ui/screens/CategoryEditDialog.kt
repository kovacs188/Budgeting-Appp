package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
fun CategoryEditDialog(
    category: Category,
    onDismiss: () -> Unit,
    onCategoryUpdated: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize form with category data
    LaunchedEffect(category.id) {
        viewModel.initializeWithCategory(category)
    }

    // Handle successful category update
    LaunchedEffect(uiState.categoryUpdated) {
        if (uiState.categoryUpdated) {
            onCategoryUpdated()
            viewModel.markCategoryUpdatedHandled()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Category Name Input
            OutlinedTextField(
                value = uiState.formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Category Name") },
                placeholder = { Text("e.g., Groceries, Salary, Rent") },
                isError = uiState.formState.nameError != null,
                supportingText = uiState.formState.nameError?.let { { Text(it) } },
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
                                selected = uiState.formState.type == type,
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
                value = uiState.formState.targetAmount,
                onValueChange = { viewModel.updateTargetAmount(it) },
                label = { Text(getAmountLabel(uiState.formState.type)) },
                placeholder = { Text("0.00") },
                leadingIcon = { Text("$") },
                isError = uiState.formState.amountError != null,
                supportingText = uiState.formState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Description Input (Optional)
            OutlinedTextField(
                value = uiState.formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Add notes about this category...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

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

            // Save Button
            Button(
                onClick = { viewModel.updateCategory() },
                enabled = uiState.formState.isValid && !uiState.isLoading,
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
                        text = "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Add some bottom padding
            Spacer(modifier = Modifier.height(16.dp))
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