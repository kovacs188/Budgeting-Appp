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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType

@Composable
fun BudgetSummaryCard(
    totalIncome: Double,
    totalExpenses: Double,
    remainingBudget: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Budget Summary",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetSummaryItem(
                    title = "Income",
                    amount = totalIncome,
                    color = MaterialTheme.colorScheme.primary
                )
                BudgetSummaryItem(
                    title = "Expenses",
                    amount = totalExpenses,
                    color = MaterialTheme.colorScheme.error
                )
                BudgetSummaryItem(
                    title = "Remaining",
                    amount = remainingBudget,
                    color = if (remainingBudget >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun BudgetSummaryItem(
    title: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onViewTransactions: () -> Unit,
    onQuickAddTransaction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            CategoryHeader(category = category)
            Spacer(modifier = Modifier.height(12.dp))
            CategoryProgressIndicator(category = category)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryAmountInfo(category = category)
            CategoryStatusIndicator(category = category)
            Spacer(modifier = Modifier.height(12.dp))
            CategoryActionButtons(
                onViewTransactions = onViewTransactions,
                onQuickAddTransaction = onQuickAddTransaction
            )
        }
    }
}

@Composable
private fun CategoryHeader(category: Category) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (category.description.isNotEmpty()) {
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${String.format("%.2f", category.targetAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "budgeted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryProgressIndicator(category: Category) {
    val progress = if (category.targetAmount > 0) {
        (category.actualAmount / category.targetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = when {
            category.type == CategoryType.INCOME -> {
                if (category.actualAmount >= category.targetAmount)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            }
            category.isOverBudget -> MaterialTheme.colorScheme.error
            progress > 0.8f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        },
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun CategoryAmountInfo(category: Category) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (category.type == CategoryType.INCOME) "Earned" else "Spent",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${String.format("%.2f", category.actualAmount)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (category.type == CategoryType.INCOME) "Still needed" else "Remaining",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${String.format("%.2f", kotlin.math.abs(category.remainingAmount))}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
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

@Composable
private fun CategoryStatusIndicator(category: Category) {
    if (category.actualAmount > 0) {
        val progress = if (category.targetAmount > 0) {
            (category.actualAmount / category.targetAmount).toFloat()
        } else 0f

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                category.type == CategoryType.INCOME && category.actualAmount >= category.targetAmount ->
                    "✅ Income goal reached!"
                category.type == CategoryType.INCOME ->
                    "⏳ ${String.format("%.1f", progress * 100.0)}% of income goal"
                category.isOverBudget ->
                    "⚠️ Over budget by $${String.format("%.2f", category.actualAmount - category.targetAmount)}"
                progress > 0.8f ->
                    "⚡ ${String.format("%.1f", progress * 100.0)}% of budget used"
                else ->
                    "👍 ${String.format("%.1f", progress * 100.0)}% of budget used"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                category.type == CategoryType.INCOME && category.actualAmount >= category.targetAmount ->
                    MaterialTheme.colorScheme.primary
                category.type == CategoryType.INCOME ->
                    MaterialTheme.colorScheme.error
                category.isOverBudget ->
                    MaterialTheme.colorScheme.error
                progress > 0.8f ->
                    MaterialTheme.colorScheme.tertiary
                else ->
                    MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
private fun CategoryActionButtons(
    onViewTransactions: () -> Unit,
    onQuickAddTransaction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onViewTransactions,
            modifier = Modifier.weight(1f)
        ) {
            Text("View History")
        }

        Button(
            onClick = onQuickAddTransaction,
            modifier = Modifier.width(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
        }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to create your first category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}