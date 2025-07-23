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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CategorySummary(
    val type: CategoryType,
    val budgeted: Double,
    val actual: Double,
    val rollover: Double,
    val categories: List<Category>
) {
    val totalBudgeted: Double get() = budgeted + rollover
    val available: Double get() = totalBudgeted - actual
    val isOverBudget: Boolean get() = actual > totalBudgeted
    val progress: Float get() = if (totalBudgeted > 0) (actual / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
}

data class MonthlyData(
    val monthName: String,
    val income: Double,
    val expenses: Double,
    val savings: Double
)

data class YearlyOverview(
    val plannedIncome: Double,
    val actualIncome: Double,
    val plannedExpenses: Double,
    val actualExpenses: Double
) {
    val plannedSavings: Double get() = plannedIncome - plannedExpenses
    val actualSavings: Double get() = actualIncome - actualExpenses
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val quickEntryExpanded: Boolean = false,
    val quickEntryForm: TransactionFormState = TransactionFormState(),
    val selectedCategoryType: CategoryType = CategoryType.DISCRETIONARY_EXPENSE,
    val availableCategories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val monthlyData: List<MonthlyData> = emptyList(),
    val yearlyOverview: YearlyOverview? = null,
    val transactionSubmitted: Boolean = false,
    val errorMessage: String? = null,
    val currentMonth: Month? = null,
    val showMonthManagementDialog: Boolean = false
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
                repository.allMonthsFlow,
                repository.allCategoriesFlow,
                repository.transactionsFlow
            ) { currentMonth, allMonths, allCategories, allTransactions ->
                updateAllData(currentMonth, allMonths, allCategories, allTransactions)
            }.collect {}
        }
    }

    // ** THE FIX IS HERE **
    // This function has been rewritten to process data from a single source of truth,
    // ensuring all calculations are always in sync.
    private fun updateAllData(
        currentMonth: Month?,
        allMonths: List<Month>,
        allCategories: List<Category>,
        allTransactions: List<Transaction>
    ) {
        // Create a definitive map of transactions grouped by their category.
        val transactionsByCategoryId = allTransactions.groupBy { it.categoryId }

        // Create a definitive list of all categories, with their actual amounts correctly calculated from the map above.
        val allCategoriesWithActuals = allCategories.map { category ->
            val actualAmount = transactionsByCategoryId[category.id]?.sumOf { it.amount } ?: 0.0
            category.copy(actualAmount = actualAmount)
        }

        // Group these definitive categories by month ID. This is our single source of truth.
        val categoriesByMonthId = allCategoriesWithActuals.groupBy { it.monthId }

        // --- Current Month Calculation ---
        val currentCategories = categoriesByMonthId[currentMonth?.id] ?: emptyList()
        val summaries = CategoryType.values().map { type ->
            val typeCategories = currentCategories.filter { it.type == type }
            CategorySummary(
                type = type,
                budgeted = typeCategories.sumOf { it.targetAmount },
                actual = typeCategories.sumOf { it.actualAmount },
                rollover = typeCategories.sumOf { it.rolloverBalance },
                categories = typeCategories
            )
        }

        // --- Yearly and Monthly Calculation (derived from the same source of truth) ---
        val yearlyOverview = calculateYearlyOverview(allMonths, categoriesByMonthId)
        val monthlyData = allMonths.take(12).map { month ->
            val categoriesForMonth = categoriesByMonthId[month.id] ?: emptyList()
            val income = categoriesForMonth.filter { it.type == CategoryType.INCOME }.sumOf { it.actualAmount }
            val expenses = categoriesForMonth.filter { it.type != CategoryType.INCOME }.sumOf { it.actualAmount }
            MonthlyData(
                monthName = month.displayName,
                income = income,
                expenses = expenses,
                savings = income - expenses
            )
        }.reversed()

        // --- Quick Entry Calculation ---
        val currentSelectedType = _uiState.value.selectedCategoryType
        val available = currentCategories.filter { it.type == currentSelectedType }

        // --- Final State Update ---
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentMonth = currentMonth,
            categorySummaries = summaries,
            monthlyData = monthlyData,
            yearlyOverview = yearlyOverview,
            availableCategories = available,
            selectedCategory = _uiState.value.selectedCategory?.let { current -> available.find { it.id == current.id } } ?: available.firstOrNull()
        )
    }

    private fun calculateYearlyOverview(
        allMonths: List<Month>,
        categoriesByMonthId: Map<String, List<Category>>
    ): YearlyOverview {
        var plannedIncome = 0.0
        var plannedExpenses = 0.0
        var actualIncome = 0.0
        var actualExpenses = 0.0

        allMonths.take(12).forEach { month ->
            val categoriesForMonth = categoriesByMonthId[month.id] ?: emptyList()

            plannedIncome += categoriesForMonth.filter { it.type == CategoryType.INCOME }.sumOf { it.targetAmount }
            plannedExpenses += categoriesForMonth.filter { it.type != CategoryType.INCOME }.sumOf { it.targetAmount }

            actualIncome += categoriesForMonth.filter { it.type == CategoryType.INCOME }.sumOf { it.actualAmount }
            actualExpenses += categoriesForMonth.filter { it.type != CategoryType.INCOME }.sumOf { it.actualAmount }
        }

        return YearlyOverview(
            plannedIncome = plannedIncome,
            actualIncome = actualIncome,
            plannedExpenses = plannedExpenses,
            actualExpenses = actualExpenses
        )
    }

    fun showMonthManagementDialog() {
        _uiState.value = _uiState.value.copy(showMonthManagementDialog = true)
    }

    fun hideMonthManagementDialog() {
        _uiState.value = _uiState.value.copy(showMonthManagementDialog = false)
    }

    fun deleteCurrentMonth() {
        viewModelScope.launch {
            val monthId = _uiState.value.currentMonth?.id ?: return@launch
            val result = repository.deleteMonth(monthId)
            if (result.isSuccess) {
                hideMonthManagementDialog()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete month: ${result.exceptionOrNull()?.message}",
                    showMonthManagementDialog = false
                )
            }
        }
    }

    fun navigateToPreviousMonth() = viewModelScope.launch { repository.navigateToPreviousMonth() }
    fun navigateToNextMonth() = viewModelScope.launch { repository.navigateToNextMonth() }

    fun toggleQuickEntry() {
        _uiState.value = _uiState.value.copy(quickEntryExpanded = !_uiState.value.quickEntryExpanded)
    }

    fun updateCategoryType(type: CategoryType) {
        _uiState.value = _uiState.value.copy(selectedCategoryType = type)
        viewModelScope.launch {
            val allCategories = repository.currentMonthCategoriesFlow.first()
            val available = allCategories.filter { it.type == type }
            _uiState.value = _uiState.value.copy(
                availableCategories = available,
                selectedCategory = available.firstOrNull()
            )
        }
    }

    fun updateSelectedCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateQuickEntryAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            quickEntryForm = _uiState.value.quickEntryForm.copy(amount = amount).validate()
        )
    }

    fun updateQuickEntryDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            quickEntryForm = _uiState.value.quickEntryForm.copy(description = description).validate()
        )
    }

    fun updateQuickEntryDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            quickEntryForm = _uiState.value.quickEntryForm.copy(date = date).validate()
        )
    }

    fun submitQuickTransaction() {
        val formState = _uiState.value.quickEntryForm
        val selectedCategory = _uiState.value.selectedCategory

        if (!formState.isValid || selectedCategory == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill out the form and select a category.")
            return
        }

        viewModelScope.launch {
            val transaction = Transaction(
                categoryId = selectedCategory.id,
                amount = formState.amount.toDouble(),
                description = formState.description.trim(),
                date = formState.date
            )
            val result = repository.createTransaction(transaction)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    transactionSubmitted = true,
                    quickEntryForm = TransactionFormState()
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun transactionHandled() {
        _uiState.value = _uiState.value.copy(transactionSubmitted = false, errorMessage = null)
    }
}
