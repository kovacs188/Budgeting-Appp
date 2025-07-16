package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionFormState
import com.example.budgetingapp.data.repository.BudgetRepository
import javax.inject.Inject

data class TransactionUiState(
    val formState: TransactionFormState = TransactionFormState(),
    val categoryId: String = "",
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
                    description = formState.description.trim()
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            formState = TransactionFormState(),
            transactionCreated = false,
            errorMessage = null
        )
    }

    fun markTransactionCreatedHandled() {
        _uiState.value = _uiState.value.copy(transactionCreated = false)
    }

    // Quick amount helpers
    fun setQuickAmount(amount: Double) {
        updateAmount(String.format("%.2f", amount))
    }

    // Get transactions for a category (for future transaction history view)
    fun getTransactionsForCategory(categoryId: String) =
        repository.getTransactionsForCategory(categoryId)

    fun getTransactionSummary(categoryId: String) =
        repository.getTransactionSummaryForCategory(categoryId)
}