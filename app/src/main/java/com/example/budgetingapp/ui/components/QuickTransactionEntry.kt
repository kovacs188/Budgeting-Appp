package com.example.budgetingapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.screens.HomeUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun QuickTransactionEntry(
    uiState: HomeUiState,
    onToggleExpanded: () -> Unit,
    onCategoryTypeChange: (CategoryType) -> Unit,
    onCategoryChange: (com.example.budgetingapp.data.model.Category) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSubmit: () -> Unit,
    onShowDatePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Transaction Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (uiState.quickEntryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (uiState.quickEntryExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = uiState.quickEntryExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Type Dropdown (First level)
                    var categoryTypeExpanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedTextField(
                            value = getCategoryTypeDisplayName(uiState.selectedCategoryType),
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Category Type") },
                            trailingIcon = {
                                IconButton(onClick = { categoryTypeExpanded = !categoryTypeExpanded }) {
                                    Icon(
                                        imageVector = if (categoryTypeExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select Category Type"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = categoryTypeExpanded,
                            onDismissRequest = { categoryTypeExpanded = false }
                        ) {
                            CategoryType.values().forEach { categoryType ->
                                DropdownMenuItem(
                                    text = { Text(getCategoryTypeDisplayName(categoryType)) },
                                    onClick = {
                                        onCategoryTypeChange(categoryType)
                                        categoryTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Individual Category Dropdown (Second level)
                    if (uiState.availableCategories.isNotEmpty()) {
                        var categoryExpanded by remember { mutableStateOf(false) }

                        Box {
                            OutlinedTextField(
                                value = uiState.selectedCategory?.name ?: "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Specific Category") },
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                                        Icon(
                                            imageVector = if (categoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Category"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                uiState.availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            onCategoryChange(category)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Show message if no categories exist for selected type
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "No categories found for ${getCategoryTypeDisplayName(uiState.selectedCategoryType)}.\nCreate categories first in the month view.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Amount and Date Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Amount
                        OutlinedTextField(
                            value = uiState.quickEntryForm.amount,
                            onValueChange = onAmountChange,
                            label = { Text("Amount") },
                            placeholder = { Text("0.00") },
                            leadingIcon = { Text("$") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = uiState.quickEntryForm.amountError != null
                        )

                        // Date
                        OutlinedTextField(
                            value = uiState.quickEntryForm.date.format(DateTimeFormatter.ofPattern("MM/dd")),
                            onValueChange = { },
                            label = { Text("Date") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = onShowDatePicker) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Pick Date"
                                    )
                                }
                            },
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Description
                    OutlinedTextField(
                        value = uiState.quickEntryForm.description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("What was this for?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Submit Button
                    Button(
                        onClick = onSubmit,
                        enabled = uiState.quickEntryForm.isValid && uiState.selectedCategory != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Transaction")
                    }
                }
            }
        }
    }
}

private fun getCategoryTypeDisplayName(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income"
        CategoryType.FIXED_EXPENSE -> "Fixed Expenses"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expenses"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expenses"
    }
}