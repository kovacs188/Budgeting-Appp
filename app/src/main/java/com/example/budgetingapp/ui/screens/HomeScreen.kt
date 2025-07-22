package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.components.DraggableCategorySummaryCard
import com.example.budgetingapp.ui.components.MonthlyOverviewChart
import com.example.budgetingapp.ui.components.QuickTransactionEntry
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMonthView: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var draggedItem by remember { mutableStateOf<CategoryType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budget Dashboard",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Exit Edit Mode" else "Edit Order",
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    TextButton(
                        onClick = onNavigateToMonthView
                    ) {
                        Text(
                            text = "View Months",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Transaction Entry
            item {
                QuickTransactionEntry(
                    uiState = uiState,
                    onToggleExpanded = { viewModel.toggleQuickEntry() },
                    onCategoryTypeChange = { viewModel.updateCategoryType(it) },
                    onCategoryChange = { viewModel.updateSelectedCategory(it) },
                    onAmountChange = { viewModel.updateQuickEntryAmount(it) },
                    onDescriptionChange = { viewModel.updateQuickEntryDescription(it) },
                    onDateChange = { viewModel.updateQuickEntryDate(it) },
                    onSubmit = { viewModel.submitQuickTransaction() },
                    onShowDatePicker = { showDatePicker = true }
                )
            }

            // Current Month Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Month Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (isEditMode) {
                        Text(
                            text = "Drag to reorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Category Cards with drag and drop
            itemsIndexed(
                items = uiState.categoryCardOrder,
                key = { _, categoryType -> categoryType }
            ) { index, categoryType ->
                val summary = uiState.categorySummaries.find { it.type == categoryType }
                if (summary != null) {
                    DraggableCategorySummaryCard(
                        summary = summary,
                        isEditMode = isEditMode,
                        isDragging = draggedItem == categoryType,
                        onDragStart = { draggedItem = categoryType },
                        onDragEnd = { draggedItem = null },
                        onMove = { fromIndex, toIndex ->
                            val newOrder = uiState.categoryCardOrder.toMutableList()
                            val item = newOrder.removeAt(fromIndex)
                            newOrder.add(toIndex, item)
                            viewModel.reorderCategoryCards(newOrder)
                        },
                        currentIndex = index
                    )
                }
            }

            // 12-Month Overview
            item {
                Text(
                    text = "12-Month Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MonthlyOverviewChart(monthlyData = uiState.monthlyData)
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.quickEntryForm.date.toEpochDay() * 24 * 60 * 60 * 1000
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            viewModel.updateQuickEntryDate(selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Show error message
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            viewModel.clearError()
        }
    }
}