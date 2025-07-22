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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEntryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onTransactionAdded: () -> Unit,
    existingTransaction: Transaction? = null,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val isEditing = existingTransaction != null

    // Reset form when dialog opens
    LaunchedEffect(category.id, existingTransaction?.id) {
        if (isEditing && existingTransaction != null) {
            viewModel.initializeForEdit(existingTransaction)
        } else {
            viewModel.resetForm()
            viewModel.setCategoryId(category.id)
        }
    }

    // Handle successful transaction creation/update
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
                        text = if (isEditing) "Edit Transaction" else getTransactionTitle(category.type),
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

            // Date picker
            OutlinedTextField(
                value = uiState.formState.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onValueChange = { },
                label = { Text("Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick Date"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
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

            // Add/Update transaction button
            Button(
                onClick = {
                    if (isEditing) {
                        viewModel.updateTransaction()
                    } else {
                        viewModel.createTransaction()
                    }
                },
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
                        text = if (isEditing) "Update Transaction" else getButtonText(category.type),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Add some bottom padding
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.formState.date.toEpochDay() * 24 * 60 * 60 * 1000
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            viewModel.updateDate(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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