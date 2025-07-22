package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.ui.components.MonthlyOverviewChart
import com.example.budgetingapp.ui.components.QuickTransactionEntry
import com.example.budgetingapp.ui.components.SimpleCategorySummaryCard
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMonthView: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

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

            // Current Month Summary Header
            item {
                Text(
                    text = "Current Month Summary",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Category Summary Cards (simplified, no drag and drop)
            items(uiState.categorySummaries) { summary ->
                SimpleCategorySummaryCard(summary = summary)
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