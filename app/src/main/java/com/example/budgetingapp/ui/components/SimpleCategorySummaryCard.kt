package com.example.budgetingapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.screens.CategorySummary

@Composable
fun SimpleCategorySummaryCard(
    summary: CategorySummary,
    onClick: (CategoryType) -> Unit
) {
    if (summary.categories.isEmpty()) {
        // Don't show the card if there are no categories of this type
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(summary.type) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = getCategoryTypeDisplayName(summary.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.categories.size} categories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { summary.progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (summary.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Amount details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Budgeted", summary.budgeted)
                AmountColumn(if (summary.type == CategoryType.INCOME) "Earned" else "Spent", summary.actual)
                AmountColumn(
                    label = "Available",
                    amount = summary.available,
                    color = if (summary.available < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AmountColumn(label: String, amount: Double, color: androidx.compose.ui.graphics.Color? = null) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color ?: LocalContentColor.current
        )
    }
}

private fun getCategoryTypeDisplayName(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income"
        CategoryType.FIXED_EXPENSE -> "Fixed Expenses"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expenses"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expenses"
        CategoryType.PROJECT_EXPENSE -> "Projects"
    }
}