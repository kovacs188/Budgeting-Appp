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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.ui.components.CategoryCard

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

    LaunchedEffect(categoryId) {
        viewModel.loadTransactions(categoryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.category?.name ?: "History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                // ** THE FIX IS HERE **
                // Actions have been removed from the top app bar
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTransactionDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Updated Summary Card
            item {
                uiState.category?.let { category ->
                    // ** THE FIX IS HERE **
                    // We now pass the new parameters to the CategoryCard
                    CategoryCard(
                        category = category,
                        isOnHistoryScreen = true,
                        onEditCategory = { onNavigateToEditCategory(category.id) },
                        onDeleteCategory = { showDeleteCategoryDialog = true },
                        onQuickAddTransaction = { showAddTransactionDialog = true },
                        onViewTransactions = { /* Not used on this screen */ }
                    )
                }
            }

            // Transactions List
            if (uiState.transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions yet. Tap '+' to add one.",
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(uiState.transactions) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onEditClick = { selectedTransactionToEdit = transaction },
                        onDeleteClick = { selectedTransactionToDelete = transaction }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // Space for FAB
        }
    }

    // Dialogs
    if (showAddTransactionDialog) {
        uiState.category?.let {
            TransactionEntryDialog(
                category = it,
                onDismiss = { showAddTransactionDialog = false },
                onTransactionAdded = { viewModel.refreshTransactions() }
            )
        }
    }

    selectedTransactionToEdit?.let { transaction ->
        uiState.category?.let { category ->
            TransactionEntryDialog(
                category = category,
                existingTransaction = transaction,
                onDismiss = { selectedTransactionToEdit = null },
                onTransactionAdded = { viewModel.refreshTransactions() }
            )
        }
    }

    selectedTransactionToDelete?.let { transaction ->
        DeleteConfirmationDialog(
            title = "Delete Transaction",
            text = "Are you sure you want to delete this $${String.format("%,.2f", transaction.amount)} transaction?",
            onConfirm = {
                viewModel.deleteTransaction(transaction.id)
                selectedTransactionToDelete = null
            },
            onDismiss = { selectedTransactionToDelete = null }
        )
    }

    if (showDeleteCategoryDialog) {
        DeleteConfirmationDialog(
            title = "Delete Category",
            text = "Are you sure? This will delete the category and all its transactions.",
            onConfirm = {
                uiState.category?.let { viewModel.deleteCategory(it.id) }
                showDeleteCategoryDialog = false
                onNavigateBack()
            },
            onDismiss = { showDeleteCategoryDialog = false }
        )
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$${String.format("%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (transaction.description.isNotEmpty()) {
                    Text(text = transaction.description, style = MaterialTheme.typography.bodyMedium)
                }
                Text(text = transaction.displayDate, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEditClick) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
