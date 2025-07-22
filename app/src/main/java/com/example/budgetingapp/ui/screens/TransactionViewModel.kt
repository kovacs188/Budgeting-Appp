package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionFormState
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionUiState(
    val formState: TransactionFormState = TransactionFormState(),
    val categoryId: String = "",
    val editingTransactionId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val transactionCreated: Boolean = false
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    fun setCategoryId(categoryId: String) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun initializeForEdit(transaction: Transaction) {
        val formState = TransactionFormState(
            amount = transaction.amount.toString(),
            description = transaction.description,
            date = transaction.date
        ).validate()

        _uiState.value = _uiState.value.copy(
            formState = formState,
            categoryId = transaction.categoryId,
            editingTransactionId = transaction.id
        )
    }

    fun updateAmount(amount: String) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(amount = amount).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun updateDescription(description: String) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(description = description).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun updateDate(date: LocalDate) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(date = date).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun createTransaction() {
        val formState = _uiState.value.formState.validate()
        _uiState.value = _uiState.value.copy(formState = formState)

        if (!formState.isValid) {
            return
        }

        val categoryId = _uiState.value.categoryId
        if (categoryId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Category not selected"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val transaction = Transaction(
                    categoryId = categoryId,
                    amount = formState.amount.toDouble(),
                    description = formState.description.trim(),
                    date = formState.date
                )

                val result = repository.createTransaction(transaction)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactionCreated = true,
                        formState = TransactionFormState() // Reset form
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create transaction: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create transaction: ${e.message}"
                )
            }
        }
    }

    fun updateTransaction() {
        val formState = _uiState.value.formState.validate()
        _uiState.value = _uiState.value.copy(formState = formState)

        if (!formState.isValid) {
            return
        }

        val transactionId = _uiState.value.editingTransactionId
        val categoryId = _uiState.value.categoryId

        if (transactionId == null || categoryId.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Transaction ID or Category not found"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val updatedTransaction = Transaction(
                    id = transactionId,
                    categoryId = categoryId,
                    amount = formState.amount.toDouble(),
                    description = formState.description.trim(),
                    date = formState.date
                )

                val result = repository.updateTransaction(updatedTransaction)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactionCreated = true // Reuse the same flag for success
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update transaction: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update transaction: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            formState = TransactionFormState(),
            editingTransactionId = null,
            transactionCreated = false,
            errorMessage = null
        )
    }

    fun markTransactionCreatedHandled() {
        _uiState.value = _uiState.value.copy(transactionCreated = false)
    }

    // Get transactions for a category (for future transaction history view)
    suspend fun getTransactionsForCategory(categoryId: String) =
        repository.getTransactionsForCategory(categoryId)

    suspend fun getTransactionSummary(categoryId: String) =
        repository.getTransactionSummaryForCategory(categoryId)
}