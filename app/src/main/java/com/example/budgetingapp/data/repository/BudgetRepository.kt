package com.example.budgetingapp.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionSortOrder
import com.example.budgetingapp.data.model.TransactionSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor() {

    // In-memory storage for now (will be replaced with Room database later)
    private val categories = mutableListOf<Category>()
    private val months = mutableListOf<Month>()
    private val transactions = mutableListOf<Transaction>()

    // StateFlow for reactive updates
    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    private val _currentMonthFlow = MutableStateFlow<Month?>(null)
    val currentMonthFlow: StateFlow<Month?> = _currentMonthFlow.asStateFlow()

    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    val transactionsFlow: StateFlow<List<Transaction>> = _transactionsFlow.asStateFlow()

    init {
        // Create initial current month
        createCurrentMonth()
    }

    // Month operations
    fun createCurrentMonth(): Month {
        val currentMonth = Month.createCurrentMonth()
        months.add(currentMonth)
        _currentMonthFlow.value = currentMonth
        return currentMonth
    }

    fun getCurrentMonth(): Month? {
        return _currentMonthFlow.value
    }

    // Category operations
    suspend fun createCategory(category: Category): Result<Category> {
        return try {
            val categoryWithMonthId = category.copy(
                monthId = getCurrentMonth()?.id ?: "default_month"
            )
            categories.add(categoryWithMonthId)
            updateCategoriesFlow()
            Result.success(categoryWithMonthId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllCategories(): List<Category> {
        return categories.filter { it.isActive }
    }

    fun getCategoriesByType(type: CategoryType): List<Category> {
        return categories.filter { it.type == type && it.isActive }
    }

    fun getCategoriesForCurrentMonth(): List<Category> {
        val currentMonthId = getCurrentMonth()?.id ?: return emptyList()
        return categories.filter { it.monthId == currentMonthId && it.isActive }
    }

    fun getCategoriesForCurrentMonthByType(type: CategoryType): List<Category> {
        val currentMonthId = getCurrentMonth()?.id ?: return emptyList()
        return categories.filter {
            it.monthId == currentMonthId && it.type == type && it.isActive
        }
    }

    fun getCategoryById(categoryId: String): Category? {
        return categories.find { it.id == categoryId && it.isActive }
    }

    // Transaction operations
    suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            transactions.add(transaction)
            updateTransactionsFlow()
            updateCategoriesFlow() // Update categories to reflect new transaction amounts
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTransactionsForCategory(categoryId: String): List<Transaction> {
        return transactions.filter { it.categoryId == categoryId && it.isActive }
    }

    fun getTransactionsForCurrentMonth(): List<Transaction> {
        val currentMonthCategories = getCategoriesForCurrentMonth().map { it.id }
        return transactions.filter { it.categoryId in currentMonthCategories && it.isActive }
    }

    fun getTransactionsSorted(
        categoryId: String? = null,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC
    ): List<Transaction> {
        val filteredTransactions = if (categoryId != null) {
            getTransactionsForCategory(categoryId)
        } else {
            getTransactionsForCurrentMonth()
        }

        return when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> filteredTransactions.sortedByDescending { it.date }
            TransactionSortOrder.DATE_ASC -> filteredTransactions.sortedBy { it.date }
            TransactionSortOrder.AMOUNT_DESC -> filteredTransactions.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> filteredTransactions.sortedBy { it.amount }
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            val index = transactions.indexOfFirst { it.id == transactionId }
            if (index != -1) {
                transactions[index] = transactions[index].copy(isActive = false)
                updateTransactionsFlow()
                updateCategoriesFlow() // Update categories to reflect removed transaction
                Result.success(Unit)
            } else {
                Result.failure(Exception("Transaction not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Budget calculations with real transaction data
    fun getTotalIncome(): Double {
        val currentMonthId = getCurrentMonth()?.id ?: return 0.0
        return categories
            .filter {
                it.monthId == currentMonthId &&
                        it.type == CategoryType.INCOME &&
                        it.isActive
            }
            .sumOf { it.targetAmount }
    }

    fun getTotalExpenses(): Double {
        val currentMonthId = getCurrentMonth()?.id ?: return 0.0
        return categories
            .filter {
                it.monthId == currentMonthId &&
                        it.type != CategoryType.INCOME &&
                        it.isActive
            }
            .sumOf { it.targetAmount }
    }

    fun getTotalByType(type: CategoryType): Double {
        val currentMonthId = getCurrentMonth()?.id ?: return 0.0
        return categories
            .filter {
                it.monthId == currentMonthId &&
                        it.type == type &&
                        it.isActive
            }
            .sumOf { it.targetAmount }
    }

    fun getRemainingBudget(): Double {
        return getTotalIncome() - getTotalExpenses()
    }

    // Calculate actual spending from transactions
    fun getActualSpendingForCategory(categoryId: String): Double {
        return getTransactionsForCategory(categoryId).sumOf { it.amount }
    }

    fun getActualIncomeEarned(): Double {
        val incomeCategories = getCategoriesForCurrentMonthByType(CategoryType.INCOME)
        return incomeCategories.sumOf { category ->
            getActualSpendingForCategory(category.id)
        }
    }

    fun getActualExpensesSpent(): Double {
        val expenseCategories = getCategoriesForCurrentMonth().filter { it.type != CategoryType.INCOME }
        return expenseCategories.sumOf { category ->
            getActualSpendingForCategory(category.id)
        }
    }

    fun getActualRemainingBudget(): Double {
        return getActualIncomeEarned() - getActualExpensesSpent()
    }

    // Update category with actual amounts from transactions
    private fun updateCategoriesFlow() {
        val updatedCategories = categories.map { category ->
            if (category.isActive) {
                val actualAmount = getActualSpendingForCategory(category.id)
                category.copy(actualAmount = actualAmount)
            } else {
                category
            }
        }
        categories.clear()
        categories.addAll(updatedCategories)
        _categoriesFlow.value = categories.filter { it.isActive }
    }

    private fun updateTransactionsFlow() {
        _transactionsFlow.value = transactions.filter { it.isActive }
    }

    // Transaction summaries
    fun getTransactionSummaryForCategory(categoryId: String): TransactionSummary {
        val categoryTransactions = getTransactionsForCategory(categoryId)
        return TransactionSummary(
            categoryId = categoryId,
            totalAmount = categoryTransactions.sumOf { it.amount },
            transactionCount = categoryTransactions.size,
            lastTransactionDate = categoryTransactions.maxByOrNull { it.date }?.date
        )
    }

    // Update category (for future direct editing)
    suspend fun updateCategory(category: Category): Result<Category> {
        return try {
            val index = categories.indexOfFirst { it.id == category.id }
            if (index != -1) {
                categories[index] = category
                updateCategoriesFlow()
                Result.success(category)
            } else {
                Result.failure(Exception("Category not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete category
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val index = categories.indexOfFirst { it.id == categoryId }
            if (index != -1) {
                categories[index] = categories[index].copy(isActive = false)
                updateCategoriesFlow()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Category not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}