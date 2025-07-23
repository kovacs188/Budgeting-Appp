package com.example.budgetingapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.Category

@Composable
fun CategoryCard(
    category: Category,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp, // Add elevation parameter
    onViewTransactions: () -> Unit,
    onQuickAddTransaction: () -> Unit,
    isOnHistoryScreen: Boolean = false,
    onEditCategory: () -> Unit = {},
    onDeleteCategory: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation) // Use the elevation parameter
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Name and Type
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (category.description.isNotEmpty()) {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Progress Indicator
            LinearProgressIndicator(
                progress = { category.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (category.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Financial Breakdown
            FinancialRow("Budgeted this month:", category.targetAmount)
            if (category.isRolloverEnabled) {
                FinancialRow("Rollover balance:", category.rolloverBalance)
                FinancialRow("Total Budgeted:", category.totalBudgeted, isBold = true)
            }
            FinancialRow("Spent this month:", category.actualAmount)

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Available Amount
            FinancialRow(
                label = "Available:",
                amount = category.amountAvailable,
                isBold = true,
                amountColor = if (category.amountAvailable < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons now change based on the context
            CategoryActionButtons(
                isOnHistoryScreen = isOnHistoryScreen,
                onViewTransactions = onViewTransactions,
                onEditCategory = onEditCategory,
                onDeleteCategory = onDeleteCategory,
                onQuickAddTransaction = onQuickAddTransaction
            )
        }
    }
}

@Composable
private fun CategoryActionButtons(
    isOnHistoryScreen: Boolean,
    onViewTransactions: () -> Unit,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onQuickAddTransaction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOnHistoryScreen) {
            // Buttons for the Transaction History screen
            OutlinedButton(onClick = onEditCategory, modifier = Modifier.weight(1f)) {
                Text("Edit")
            }
            OutlinedButton(
                onClick = onDeleteCategory,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        } else {
            // Default button for other screens
            OutlinedButton(onClick = onViewTransactions, modifier = Modifier.weight(1f)) {
                Text("View History")
            }
        }

        // "Add Transaction" button is always present
        Button(onClick = onQuickAddTransaction, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add")
        }
    }
}


@Composable
private fun FinancialRow(label: String, amount: Double, isBold: Boolean = false, amountColor: androidx.compose.ui.graphics.Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "$${String.format("%,.2f", amount)}",
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = amountColor ?: LocalContentColor.current
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun CategoryPlaceholder(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
