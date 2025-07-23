package com.example.budgetingapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.Category

@Composable
fun ProjectSummaryCard(
    activeProjects: List<Category>,
    onClick: () -> Unit
) {
    if (activeProjects.isEmpty()) {
        return // Don't show the card if there are no active projects
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "Active Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${activeProjects.size} projects",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary stats
            val totalBudget = activeProjects.sumOf { it.projectTotalBudget }
            val totalSpent = activeProjects.sumOf { it.projectTotalSpent }
            val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f

            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxWidth(),
                color = if (totalSpent > totalBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProjectStatColumn("Total Spent", totalSpent)
                ProjectStatColumn("Total Budget", totalBudget)
                ProjectStatColumn(
                    label = "Remaining",
                    amount = totalBudget - totalSpent,
                    color = if ((totalBudget - totalSpent) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Show a few project names if there's space
            if (activeProjects.size <= 3) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    activeProjects.forEach { project ->
                        Text(
                            text = "• ${project.name}: $${String.format("%,.0f", project.projectTotalSpent)}/$${String.format("%,.0f", project.projectTotalBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectStatColumn(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}