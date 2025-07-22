package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionFormState
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MonthlyData(
    val monthName: String,
    val income: Double,
    val expenses: Double,
    val savings: Double
)

data class CategorySummary(
    val type: CategoryType,
    val budgeted: Double,
    val actual: Double,
    val categories: List<Category>
) {
    val remaining: Double get() = if (type == CategoryType.INCOME) actual - budgeted else budgeted - actual
    val isOverBudget: Boolean get() = if (type == CategoryType.INCOME) actual < budgeted else actual > budgeted
    val progress: Float get() = if (budgeted > 0) (actual / budgeted).toFloat().coerceIn(0f, 1f) else 0f
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val quickEntryExpanded: Boolean = false,
    val quickEntryForm: TransactionFormState = TransactionFormState(),
    val selectedCategoryType: CategoryType = CategoryType.DISCRETIONARY_EXPENSE,
    val availableCategories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val monthlyData: List<MonthlyData> = emptyList(),
    val transactionSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val currentMonth: Month? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.currentMonthFlow,
                repository.categoriesFlow,
                repository.transactionsFlow
            ) { currentMonthFromRepo, _, _ ->
                _uiState.value = _uiState.value.copy(currentMonth = currentMonthFromRepo)
                updateDashboardData()
            }.collect {}
        }
    }

    private suspend fun updateDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        try {
            val categorySummaries = loadCategorySummaries()
            val monthlyData = loadMonthlyData()

            val currentSelectedCategoryType = _uiState.value.selectedCategoryType
            val availableCategories = repository.getCategoriesForCurrentMonthByType(currentSelectedCategoryType)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                categorySummaries = categorySummaries,
                monthlyData = monthlyData,
                availableCategories = availableCategories,
                selectedCategory = _uiState.value.selectedCategory?.let { currentCat ->
                    availableCategories.find { it.id == currentCat.id }
                } ?: availableCategories.firstOrNull()
            )

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to load dashboard data: ${e.message}"
            )
        }
    }

    private suspend fun loadCategorySummaries(): List<CategorySummary> {
        return CategoryType.values().map { type ->
            val categories = repository.getCategoriesForCurrentMonthByType(type)
            CategorySummary(
                type = type,
                budgeted = categories.sumOf { it.targetAmount },
                actual = categories.sumOf { it.actualAmount },
                categories = categories
            )
        }
    }

    private suspend fun loadMonthlyData(): List<MonthlyData> {
        return try {
            val allMonths = repository.getAllMonths().take(12)

            allMonths.map { month ->
                val monthCategories = repository.getCategoriesForMonth(month.id)
                val income = monthCategories.filter { it.type == CategoryType.INCOME }.sumOf { it.actualAmount }
                val expenses = monthCategories.filter { it.type != CategoryType.INCOME }.sumOf { it.actualAmount }

                MonthlyData(
                    monthName = "${month.name} ${month.year}",
                    income = income,
                    expenses = expenses,
                    savings = income - expenses
                )
            }.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun navigateToPreviousMonth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.navigateToPreviousMonth()
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to navigate to next month: ${e.message}"
                )
            }
        }
    }

    fun toggleQuickEntry() {
        _uiState.value = _uiState.value.copy(
            quickEntryExpanded = !_uiState.value.quickEntryExpanded
        )
    }

    fun updateCategoryType(type: CategoryType) {
        viewModelScope.launch {
            val categories = repository.getCategoriesForCurrentMonthByType(type)
            _uiState.value = _uiState.value.copy(
                selectedCategoryType = type,
                availableCategories = categories,
                selectedCategory = categories.firstOrNull()
            )
        }
    }

    fun updateSelectedCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateQuickEntryAmount(amount: String) {
        val currentForm = _uiState.value.quickEntryForm
        val updatedForm = currentForm.copy(amount = amount).validate()
        _uiState.value = _uiState.value.copy(quickEntryForm = updatedForm)
    }

    fun updateQuickEntryDescription(description: String) {
        val currentForm = _uiState.value.quickEntryForm
        val updatedForm = currentForm.copy(description = description).validate()
        _uiState.value = _uiState.value.copy(quickEntryForm = updatedForm) // CORRECTED LINE
    }

    fun updateQuickEntryDate(date: LocalDate) {
        val currentForm = _uiState.value.quickEntryForm
        val updatedForm = currentForm.copy(date = date).validate()
        _uiState.value = _uiState.value.copy(quickEntryForm = updatedForm)
    }

    fun submitQuickTransaction() {
        val formState = _uiState.value.quickEntryForm.validate()
        _uiState.value = _uiState.value.copy(quickEntryForm = formState)

        if (!formState.isValid || _uiState.value.selectedCategory == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = if (!formState.isValid) "Please fill in all required fields" else "Please select a category"
            )
            return
        }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    categoryId = _uiState.value.selectedCategory!!.id,
                    amount = formState.amount.toDouble(),
                    description = formState.description.trim(),
                    date = formState.date
                )

                val result = repository.createTransaction(transaction)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        quickEntryForm = TransactionFormState(),
                        transactionSubmitted = true,
                        quickEntryExpanded = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add transaction: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add transaction: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun markTransactionSubmittedHandled() {
        _uiState.value = _uiState.value.copy(transactionSubmitted = false)
    }
}