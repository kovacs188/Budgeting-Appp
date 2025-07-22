package com.example.budgetingapp.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.ui.screens.MonthlyData

@Composable
fun MonthlyOverviewChart(monthlyData: List<MonthlyData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (monthlyData.isEmpty()) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                // Simple bar chart representation
                monthlyData.forEach { data ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.monthName.take(7), // Show "Jan 2024" format
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            // Income bar
                            LinearProgressIndicator(
                                progress = { if (data.income > 0) 1f else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Expenses bar
                            LinearProgressIndicator(
                                progress = { if (data.income > 0) (data.expenses / data.income).toFloat().coerceIn(0f, 1f) else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Text(
                            text = if (data.savings >= 0) "+$${String.format("%.0f", data.savings)}"
                            else "-$${String.format("%.0f", kotlin.math.abs(data.savings))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (data.savings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Legend
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Income")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = MaterialTheme.colorScheme.error, label = "Expenses")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}