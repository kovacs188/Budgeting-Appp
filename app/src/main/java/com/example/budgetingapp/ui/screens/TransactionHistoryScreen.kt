package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditCategory: (String) -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }

    // Load transactions when screen opens
    LaunchedEffect(categoryId) {
        viewModel.loadTransactions(categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.category?.name ?: "Transaction History",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.category?.let { category ->
                            Text(
                                text = "${category.displayType} • $${String.format("%.2f", category.targetAmount)} budgeted",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Month"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTransactionDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Summary Card
            uiState.category?.let { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Budgeted",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${String.format("%.2f", category.targetAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = if (category.type == CategoryType.INCOME) "Earned" else "Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${String.format("%.2f", category.actualAmount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (category.isOverBudget && category.type != CategoryType.INCOME)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = if (category.type == CategoryType.INCOME) "Needed" else "Remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${String.format("%.2f", kotlin.math.abs(category.remainingAmount))}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit and Delete buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToEditCategory(category.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit Category")
                            }

                            OutlinedButton(
                                onClick = { showDeleteCategoryDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete Category")
                            }
                        }
                    }
                }
            }

            // Transactions List
            if (uiState.transactions.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Transactions Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button to add your first transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onEditClick = { selectedTransactionToEdit = transaction },
                            onDeleteClick = { selectedTransactionToDelete = transaction }
                        )
                    }

                    // Add some space at the bottom for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Delete Category Confirmation Dialog
    if (showDeleteCategoryDialog) {
        uiState.category?.let { category ->
            AlertDialog(
                onDismissRequest = { showDeleteCategoryDialog = false },
                title = { Text("Delete Category") },
                text = {
                    Text(buildString {
                        append("Are you sure you want to delete \"${category.name}\"?")
                        if (uiState.transactions.isNotEmpty()) {
                            append("\n\nThis will also delete ${uiState.transactions.size} transaction(s) totaling $${String.format("%.2f", uiState.transactions.sumOf { it.amount })}.")
                        }
                    })
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(category.id)
                            showDeleteCategoryDialog = false
                            onNavigateBack() // Navigate back since category is deleted
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteCategoryDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Add Transaction Dialog
    if (showAddTransactionDialog) {
        uiState.category?.let { category ->
            TransactionEntryDialog(
                category = category,
                onDismiss = { showAddTransactionDialog = false },
                onTransactionAdded = {
                    showAddTransactionDialog = false
                    viewModel.refreshTransactions()
                }
            )
        }
    }

    // Edit Transaction Dialog
    if (selectedTransactionToEdit != null) {
        uiState.category?.let { category ->
            TransactionEntryDialog(
                category = category,
                existingTransaction = selectedTransactionToEdit,
                onDismiss = { selectedTransactionToEdit = null },
                onTransactionAdded = {
                    selectedTransactionToEdit = null
                    viewModel.refreshTransactions()
                }
            )
        }
    }

    // Delete Confirmation Dialog
    selectedTransactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { selectedTransactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to delete this $${String.format("%.2f", transaction.amount)} transaction?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        selectedTransactionToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTransactionToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionCard(
    transaction: Transaction,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (transaction.description.isNotEmpty()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = transaction.displayDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Transaction",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}