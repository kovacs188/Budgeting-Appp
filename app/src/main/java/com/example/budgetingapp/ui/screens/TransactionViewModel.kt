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

    private fun saveTransaction() {
        viewModelScope.launch {
            val formState = _uiState.value.formState.validate()
            if (!formState.isValid) {
                _uiState.value = _uiState.value.copy(formState = formState)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = if (_uiState.value.editingTransactionId != null) {
                repository.updateTransaction(
                    Transaction(
                        id = _uiState.value.editingTransactionId!!,
                        categoryId = _uiState.value.categoryId,
                        amount = formState.amount.toDouble(),
                        description = formState.description.trim(),
                        date = formState.date
                    )
                )
            } else {
                repository.createTransaction(
                    Transaction(
                        categoryId = _uiState.value.categoryId,
                        amount = formState.amount.toDouble(),
                        description = formState.description.trim(),
                        date = formState.date
                    )
                )
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, transactionCreated = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "An error occurred"
                )
            }
        }
    }

    fun submitTransaction() {
        saveTransaction()
    }

    fun resetForm() {
        _uiState.value = TransactionUiState()
    }

    fun markTransactionCreatedHandled() {
        _uiState.value = _uiState.value.copy(transactionCreated = false)
    }
}
