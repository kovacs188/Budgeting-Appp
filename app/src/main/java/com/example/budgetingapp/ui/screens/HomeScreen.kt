package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.ui.components.DraggableSimpleCategorySummaryCard
import com.example.budgetingapp.ui.components.MonthlyOverviewChart
import com.example.budgetingapp.ui.components.ProjectSummaryCard
import com.example.budgetingapp.ui.components.QuickTransactionEntry
import com.example.budgetingapp.ui.components.SummaryCardDragDropState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToCategoryTypeDetails: (CategoryType) -> Unit,
    onNavigateToCategoryCreator: () -> Unit,
    onNavigateToProjects: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Drag and drop setup
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dragDropState = remember(listState, scope) {
        SummaryCardDragDropState(
            listState = listState,
            onMove = { from, to -> viewModel.onSummaryCardMove(from, to) },
            scope = scope
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.transactionHandled()
        }
    }

    LaunchedEffect(uiState.transactionSubmitted) {
        if(uiState.transactionSubmitted) {
            snackbarHostState.showSnackbar("Transaction added!")
            viewModel.transactionHandled()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LomlBudgetTopBar(
                currentMonth = uiState.currentMonth,
                onPreviousMonth = { viewModel.navigateToPreviousMonth() },
                onNextMonth = { viewModel.navigateToNextMonth() },
                onMonthNameClick = { viewModel.showMonthManagementDialog() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCategoryCreator) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = !dragDropState.isDragging
        ) {
            item { Spacer(Modifier.height(0.dp)) }

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

            item {
                Text(
                    text = "Budget Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.categorySummaries.all { it.categories.isEmpty() }) {
                item {
                    EmptyBudgetState(onAddCategoryClick = onNavigateToCategoryCreator)
                }
            } else {
                itemsIndexed(
                    uiState.categorySummaries.filter { it.categories.isNotEmpty() },
                    key = { _, summary -> summary.type }
                ) { index, summary ->
                    DraggableSimpleCategorySummaryCard(
                        summary = summary,
                        dragDropState = dragDropState,
                        index = index,
                        onClick = onNavigateToCategoryTypeDetails,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }

            // Projects section
            if (uiState.activeProjects.isNotEmpty()) {
                item {
                    ProjectSummaryCard(
                        activeProjects = uiState.activeProjects,
                        onClick = onNavigateToProjects
                    )
                }
            }

            item {
                Text(
                    text = "Monthly & Yearly Overview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                MonthlyOverviewChart(
                    monthlyData = uiState.monthlyData,
                    yearlyOverview = uiState.yearlyOverview,
                    monthlyOverview = uiState.monthlyOverview
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (uiState.showMonthManagementDialog) {
        MonthManagementDialog(
            monthName = uiState.currentMonth?.displayName ?: "",
            onDismiss = { viewModel.hideMonthManagementDialog() },
            onDelete = { viewModel.deleteCurrentMonth() }
        )
    }

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
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LomlBudgetTopBar(
    currentMonth: Month?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthNameClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "LOML's Budget",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = currentMonth?.displayName ?: "Loading...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMonthNameClick() }
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthManagementDialog(
    monthName: String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage $monthName") },
        text = { Text("Are you sure you want to delete this month and all of its associated data? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Month")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmptyBudgetState(onAddCategoryClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome! Let's set up your budget.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create categories for your income and expenses to start tracking your money.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAddCategoryClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text("Create Your First Category")
            }
        }
    }
}