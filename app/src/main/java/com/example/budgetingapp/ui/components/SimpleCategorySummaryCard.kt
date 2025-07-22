package com.example.budgetingapp.ui.components

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
    summary: CategorySummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                color = when {
                    summary.type == CategoryType.INCOME -> {
                        if (summary.actual >= summary.budgeted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    }
                    summary.isOverBudget -> MaterialTheme.colorScheme.error
                    summary.progress > 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Amount details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Budgeted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.0f", summary.budgeted)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = if (summary.type == CategoryType.INCOME) "Earned" else "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.0f", summary.actual)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = if (summary.type == CategoryType.INCOME) "Needed" else "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.0f", kotlin.math.abs(summary.remaining))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (summary.type == CategoryType.INCOME) {
                            if (summary.actual >= summary.budgeted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        } else {
                            if (summary.isOverBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        }
                    )
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