package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthUiState(
    val currentMonth: Month? = null,
    val categories: List<Category> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val incomeCategories: List<Category> = emptyList(),
    val fixedExpenseCategories: List<Category> = emptyList(),
    val variableExpenseCategories: List<Category> = emptyList(),
    val discretionaryExpenseCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MonthViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthUiState())
    val uiState: StateFlow<MonthUiState> = _uiState.asStateFlow()

    init {
        // Combine current month and categories flows for reactive updates
        viewModelScope.launch {
            combine(
                repository.currentMonthFlow,
                repository.categoriesFlow,
                repository.transactionsFlow // Add transactions flow to trigger updates
            ) { month, categories, transactions ->
                Triple(month, categories, transactions)
            }.collect { (month, categories, transactions) ->
                updateUiState(month)
            }
        }
    }

    private suspend fun updateUiState(month: Month?) {
        try {
            if (month == null) {
                _uiState.value = _uiState.value.copy(
                    currentMonth = null,
                    categories = emptyList(),
                    totalIncome = 0.0,
                    totalExpenses = 0.0,
                    remainingBudget = 0.0,
                    incomeCategories = emptyList(),
                    fixedExpenseCategories = emptyList(),
                    variableExpenseCategories = emptyList(),
                    discretionaryExpenseCategories = emptyList(),
                    isLoading = false
                )
                return
            }

            // Get categories with calculated actual amounts
            val monthCategories = repository.getCategoriesForCurrentMonth()

            val incomeCategories = monthCategories.filter { it.type == CategoryType.INCOME }
            val fixedExpenseCategories = monthCategories.filter { it.type == CategoryType.FIXED_EXPENSE }
            val variableExpenseCategories = monthCategories.filter { it.type == CategoryType.VARIABLE_EXPENSE }
            val discretionaryExpenseCategories = monthCategories.filter { it.type == CategoryType.DISCRETIONARY_EXPENSE }

            val totalIncome = repository.getTotalIncome()
            val totalExpenses = repository.getTotalExpenses()
            val remainingBudget = totalIncome - totalExpenses

            _uiState.value = _uiState.value.copy(
                currentMonth = month,
                categories = monthCategories,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                remainingBudget = remainingBudget,
                incomeCategories = incomeCategories,
                fixedExpenseCategories = fixedExpenseCategories,
                variableExpenseCategories = variableExpenseCategories,
                discretionaryExpenseCategories = discretionaryExpenseCategories,
                isLoading = false
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to update UI: ${e.message}"
            )
        }
    }

    // MONTH NAVIGATION FUNCTIONS
    fun navigateToPreviousMonth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.navigateToPreviousMonth()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to navigate to previous month: ${e.message}"
                )
            }
        }
    }

    fun navigateToNextMonth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.navigateToNextMonth()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to navigate to next month: ${e.message}"
                )
            }
        }
    }

    fun onCreateCategoryClicked() {
        // Navigation is handled in the UI layer
    }

    fun refreshMonth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val currentMonth = repository.getCurrentMonth()
                updateUiState(currentMonth)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to refresh data: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Convenience methods for accessing repository data
    fun getCategoriesByType(type: CategoryType): List<Category> {
        return when (type) {
            CategoryType.INCOME -> _uiState.value.incomeCategories
            CategoryType.FIXED_EXPENSE -> _uiState.value.fixedExpenseCategories
            CategoryType.VARIABLE_EXPENSE -> _uiState.value.variableExpenseCategories
            CategoryType.DISCRETIONARY_EXPENSE -> _uiState.value.discretionaryExpenseCategories
        }
    }
}