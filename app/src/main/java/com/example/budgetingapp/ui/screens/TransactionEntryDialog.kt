package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun TransactionEntryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onTransactionAdded: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reset form when dialog opens
    LaunchedEffect(category.id) {
        viewModel.resetForm()
        viewModel.setCategoryId(category.id)
    }

    // Handle successful transaction creation
    LaunchedEffect(uiState.transactionCreated) {
        if (uiState.transactionCreated) {
            onTransactionAdded()
            onDismiss()
            viewModel.markTransactionCreatedHandled()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
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
                Column {
                    Text(
                        text = getTransactionTitle(category.type),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Category info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", category.targetAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", category.actualAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (category.isOverBudget && category.type != CategoryType.INCOME)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = if (category.type == CategoryType.INCOME) "Needed" else "Remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", kotlin.math.abs(category.remainingAmount))}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
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
                }
            }

            // Amount input
            OutlinedTextField(
                value = uiState.formState.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text(getAmountLabel(category.type)) },
                placeholder = { Text("0.00") },
                leadingIcon = { Text("$") },
                isError = uiState.formState.amountError != null,
                supportingText = uiState.formState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Description input
            OutlinedTextField(
                value = uiState.formState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                placeholder = { Text("What was this for?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Quick amount buttons (for common amounts)
            if (category.type != CategoryType.INCOME) {
                Text(
                    text = "Quick Amounts",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAmountButton(
                        amount = "10",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateAmount("10.00") }
                    )
                    QuickAmountButton(
                        amount = "25",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateAmount("25.00") }
                    )
                    QuickAmountButton(
                        amount = "50",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateAmount("50.00") }
                    )
                    QuickAmountButton(
                        amount = "100",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateAmount("100.00") }
                    )
                }
            }

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

            // Add transaction button
            Button(
                onClick = { viewModel.createTransaction() },
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
                        text = getButtonText(category.type),
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

@Composable
private fun QuickAmountButton(
    amount: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(
            text = "$$amount",
            fontSize = 14.sp
        )
    }
}

private fun getTransactionTitle(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Add Income"
        CategoryType.FIXED_EXPENSE -> "Add Fixed Expense"
        CategoryType.VARIABLE_EXPENSE -> "Add Variable Expense"
        CategoryType.DISCRETIONARY_EXPENSE -> "Add Discretionary Expense"
    }
}

private fun getAmountLabel(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income Amount"
        else -> "Expense Amount"
    }
}

private fun getButtonText(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Add Income"
        else -> "Add Expense"
    }
}