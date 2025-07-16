package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.repository.BudgetRepository
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
                repository.categoriesFlow
            ) { month, categories ->
                updateUiState(month, categories)
            }.collect { /* State is updated in updateUiState */ }
        }
    }

    private fun updateUiState(month: Month?, allCategories: List<Category>) {
        val currentMonthId = month?.id ?: ""
        val monthCategories = allCategories.filter { it.monthId == currentMonthId && it.isActive }

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
    }

    fun onCreateCategoryClicked() {
        // Navigation is handled in the UI layer
    }

    fun refreshMonth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Repository data is already reactive, so just trigger update
                val currentMonth = repository.getCurrentMonth()
                val categories = repository.getCategoriesForCurrentMonth()
                updateUiState(currentMonth, categories)
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