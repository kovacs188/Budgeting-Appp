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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        Column(modifier = Modifier.padding(16.dp)) {
            yearlyOverview?.let {
                YearlySummarySection(overview = it)
            } ?: Text(
                "Yearly Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // You can add the monthly breakdown bar chart here if needed
        }
    }
}

@Composable
private fun YearlySummarySection(overview: YearlyOverview) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            GoalVsActualCard(
                title = "Income",
                goal = overview.plannedIncome,
                actual = overview.actualIncome,
                color = MaterialTheme.colorScheme.primary
            )
            GoalVsActualCard(
                title = "Expenses",
                goal = overview.plannedExpenses,
                actual = overview.actualExpenses,
                color = MaterialTheme.colorScheme.error
            )
            GoalVsActualCard(
                title = "Net Savings",
                goal = overview.plannedSavings,
                actual = overview.actualSavings,
                color = if (overview.actualSavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun GoalVsActualCard(
    title: String,
    goal: Double,
    actual: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%,.0f", actual)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = "Goal: $${String.format("%,.0f", goal)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
