package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionHistoryUiState(
    val category: Category? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionHistoryUiState())
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    fun loadTransactions(categoryId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Load category details
                val category = repository.getCategoryById(categoryId)

                // Load transactions for this category
                val transactions = repository.getTransactionsForCategory(categoryId)

                // Update category with actual amount
                val updatedCategory = category?.let {
                    val actualAmount = repository.getActualSpendingForCategory(categoryId)
                    it.copy(actualAmount = actualAmount)
                }

                _uiState.value = _uiState.value.copy(
                    category = updatedCategory,
                    transactions = transactions,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = repository.deleteTransaction(transactionId)

                if (result.isSuccess) {
                    // Reload transactions after deletion
                    val categoryId = _uiState.value.category?.id
                    if (categoryId != null) {
                        loadTransactions(categoryId)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete transaction: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete transaction: ${e.message}"
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val result = repository.deleteCategory(categoryId)

                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete category: ${result.exceptionOrNull()?.message}"
                    )
                }
                // Note: If successful, the calling screen should navigate back

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete category: ${e.message}"
                )
            }
        }
    }

    fun refreshTransactions() {
        val categoryId = _uiState.value.category?.id
        if (categoryId != null) {
            loadTransactions(categoryId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}