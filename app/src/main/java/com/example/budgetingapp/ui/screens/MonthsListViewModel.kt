package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonthsListUiState(
    val months: List<Month> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MonthsListViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthsListUiState())
    val uiState: StateFlow<MonthsListUiState> = _uiState.asStateFlow()

    init {
        loadMonths()
    }

    private fun loadMonths() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val months = repository.getAllMonths()

                // Update each month with actual remaining budget
                val monthsWithActualAmounts = months.map { month ->
                    val actualIncome = getActualIncomeForMonth(month.id)
                    val actualExpenses = getActualExpensesForMonth(month.id)

                    // Debug logging
                    println("📊 Month: ${month.displayName}")
                    println("   Income: ${actualIncome}")
                    println("   Expenses: ${actualExpenses}")
                    println("   Remaining: ${actualIncome - actualExpenses}")

                    // Create month with updated totals
                    val updatedMonth = month.copy(
                        totalIncome = actualIncome,
                        totalExpenses = actualExpenses
                    )

                    println("   Month.remainingBudget: ${updatedMonth.remainingBudget}")

                    updatedMonth
                }

                _uiState.value = _uiState.value.copy(
                    months = monthsWithActualAmounts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load months: ${e.message}"
                )
            }
        }
    }

    private suspend fun getActualIncomeForMonth(monthId: String): Double {
        val categories = repository.getCategoriesForMonth(monthId)
        val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
        return incomeCategories.sumOf { it.actualAmount } // Use the calculated amount from transactions
    }

    private suspend fun getActualExpensesForMonth(monthId: String): Double {
        val categories = repository.getCategoriesForMonth(monthId)
        val expenseCategories = categories.filter { it.type != CategoryType.INCOME }
        return expenseCategories.sumOf { it.actualAmount } // Use the calculated amount from transactions
    }

    fun deleteMonth(monthId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.deleteMonth(monthId)
                loadMonths() // Refresh the list
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete month: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}