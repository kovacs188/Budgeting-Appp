package com.example.budgetingapp.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.screens.CategorySummary

@Composable
fun DraggableCategorySummaryCard(
    summary: CategorySummary,
    isEditMode: Boolean,
    isDragging: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit,
    currentIndex: Int
) {
    var dragOffset by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .shadow(
                elevation = if (isDragging) 8.dp else 2.dp,
                shape = CardDefaults.shape
            )
            .offset(y = dragOffset.dp)
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = {
                                dragOffset = 0f
                                onDragEnd()
                            }
                        ) { _, dragAmount ->
                            dragOffset += dragAmount.y / density

                            // Simple reordering logic
                            val cardHeight = 120 // Approximate card height in dp
                            val threshold = cardHeight / 2

                            if (dragOffset > threshold && currentIndex < 3) {
                                onMove(currentIndex, currentIndex + 1)
                                dragOffset = 0f
                            } else if (dragOffset < -threshold && currentIndex > 0) {
                                onMove(currentIndex, currentIndex - 1)
                                dragOffset = 0f
                            }
                        }
                    }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle (only visible in edit mode)
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // Category content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
}

private fun getCategoryTypeDisplayName(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income"
        CategoryType.FIXED_EXPENSE -> "Fixed Expenses"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expenses"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expenses"
    }
}