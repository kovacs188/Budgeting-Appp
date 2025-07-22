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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetingapp.ui.screens.MonthlyData
import com.example.budgetingapp.ui.screens.YearlyOverview

@Composable
fun MonthlyOverviewChart(
    monthlyData: List<MonthlyData>,
    yearlyOverview: YearlyOverview? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Yearly Summary Header
            yearlyOverview?.let { overview ->
                YearlySummarySection(overview = overview)

                if (monthlyData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Monthly Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Monthly Data
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
private fun YearlySummarySection(overview: YearlyOverview) {
    Column {
        // Planned vs Actual comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PlannedVsActualCard(
                title = "Income",
                planned = overview.plannedIncome,
                actual = overview.actualIncome,
                color = MaterialTheme.colorScheme.primary
            )

            PlannedVsActualCard(
                title = "Expenses",
                planned = overview.plannedExpenses,
                actual = overview.actualExpenses,
                color = MaterialTheme.colorScheme.error
            )

            PlannedVsActualCard(
                title = "Savings",
                planned = overview.plannedSavings,
                actual = overview.actualSavings,
                color = if (overview.actualSavings >= 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall performance summary
        val savingsVariance = overview.actualSavings - overview.plannedSavings
        val performanceText = when {
            savingsVariance > 100 -> "🎉 Exceeding savings goals!"
            savingsVariance > 0 -> "✅ On track with savings"
            savingsVariance > -100 -> "⚠️ Slightly behind savings goal"
            else -> "📉 Behind savings target"
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    savingsVariance > 0 -> MaterialTheme.colorScheme.primaryContainer
                    savingsVariance > -100 -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = performanceText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (savingsVariance != 0.0) {
                    Text(
                        text = if (savingsVariance > 0)
                            "+$${String.format("%.0f", savingsVariance)} ahead of plan"
                        else
                            "$${String.format("%.0f", kotlin.math.abs(savingsVariance))} behind plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedVsActualCard(
    title: String,
    planned: Double,
    actual: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Planned amount - FIXED: Added dollar sign and changed to "Planned"
        Text(
            text = "Planned: $${String.format("%.0f", kotlin.math.abs(planned))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Actual amount - FIXED: Added dollar sign
        Text(
            text = "$${String.format("%.0f", kotlin.math.abs(actual))}",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )

        // Variance indicator - FIXED: Added dollar signs
        if (planned != 0.0) {
            val variance = actual - planned
            val percentVariance = (variance / planned) * 100

            Text(
                text = when {
                    kotlin.math.abs(variance) < 1 -> "On target"
                    variance > 0 -> "+$${String.format("%.0f", variance)} (+${String.format("%.0f%%", percentVariance)})"
                    else -> "-$${String.format("%.0f", kotlin.math.abs(variance))} (${String.format("%.0f%%", percentVariance)})"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    kotlin.math.abs(variance) < 1 -> MaterialTheme.colorScheme.primary
                    title == "Expenses" && variance < 0 -> MaterialTheme.colorScheme.primary // Under budget on expenses is good
                    title == "Expenses" && variance > 0 -> MaterialTheme.colorScheme.error   // Over budget on expenses is bad
                    title != "Expenses" && variance > 0 -> MaterialTheme.colorScheme.primary // Over plan on income/savings is good
                    else -> MaterialTheme.colorScheme.error // Under plan on income/savings is bad
                },
                textAlign = TextAlign.Center
            )
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