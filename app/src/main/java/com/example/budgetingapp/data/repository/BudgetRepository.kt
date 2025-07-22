package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.database.CategoryDao
import com.example.budgetingapp.data.database.MonthDao
import com.example.budgetingapp.data.database.TransactionDao
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionSortOrder
import com.example.budgetingapp.data.model.TransactionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val monthDao: MonthDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) {

    // Create a coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // StateFlow for reactive updates
    val categoriesFlow = categoryDao.getAllActiveCategories()
    val transactionsFlow = transactionDao.getAllActiveTransactions()

    private val _currentMonthFlow = MutableStateFlow<Month?>(null)
    val currentMonthFlow: StateFlow<Month?> = _currentMonthFlow.asStateFlow()

    init {
        // Auto-create current month on app start
        repositoryScope.launch {
            ensureCurrentMonthExists()
        }
    }

    // Auto-create current month if it doesn't exist
    private suspend fun ensureCurrentMonthExists() {
        val now = LocalDate.now()
        val existingMonth = monthDao.getMonthByYearAndMonth(now.year, now.monthValue)

        if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
        } else {
            val currentMonth = Month.createCurrentMonth()
            monthDao.insertMonth(currentMonth)

            // NEW: Auto-copy categories from most recent month
            copyPreviousMonthCategories(currentMonth.id)

            _currentMonthFlow.value = currentMonth
        }
    }

    // Get or create a month for specific year/month
    suspend fun getOrCreateMonth(year: Int, monthValue: Int): Month {
        val existingMonth = monthDao.getMonthByYearAndMonth(year, monthValue)

        return if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
            existingMonth
        } else {
            val newMonth = Month(
                name = LocalDate.of(year, monthValue, 1).format(DateTimeFormatter.ofPattern("MMMM")),
                year = year,
                month = monthValue
            )
            monthDao.insertMonth(newMonth)

            // NEW: Auto-copy categories from most recent month
            copyPreviousMonthCategories(newMonth.id)

            _currentMonthFlow.value = newMonth
            newMonth
        }
    }

    // NEW METHOD: Copy categories from the most recent month
    private suspend fun copyPreviousMonthCategories(newMonthId: String) {
        try {
            // Get all months ordered by most recent first
            val allMonths = monthDao.getAllActiveMonths().first()

            // Find the most recent month (excluding the one we just created)
            val previousMonth = allMonths.firstOrNull { it.id != newMonthId }

            if (previousMonth != null) {
                // Get categories from the previous month
                val previousCategories = categoryDao.getCategoriesForMonth(previousMonth.id).first()

                // Copy each category to the new month with smart spending logic
                previousCategories.forEach { category ->
                    val newActualAmount = when (category.type) {
                        CategoryType.FIXED_EXPENSE -> {
                            // Fixed expenses: carry over spending (rent, insurance, etc.)
                            category.actualAmount
                        }
                        CategoryType.INCOME,
                        CategoryType.VARIABLE_EXPENSE,
                        CategoryType.DISCRETIONARY_EXPENSE -> {
                            // All others: reset to $0 for fresh tracking
                            0.0
                        }
                    }

                    val newCategory = category.copy(
                        id = "category_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}",
                        monthId = newMonthId,
                        actualAmount = newActualAmount,
                        createdDate = LocalDateTime.now()
                    )
                    categoryDao.insertCategory(newCategory)
                }

                // Log for debugging (remove in production)
                val fixedExpenseCount = previousCategories.count { it.type == CategoryType.FIXED_EXPENSE }
                println("✅ Auto-copied ${previousCategories.size} categories from ${previousMonth.displayName}")
                println("   → Fixed expenses ($fixedExpenseCount): spending carried over")
                println("   → Other categories: spending reset to $0")
            } else {
                println("ℹ️ No previous month found - user will create categories manually")
            }

        } catch (e: Exception) {
            // Don't crash if auto-copy fails - user can still create categories manually
            println("⚠️ Auto-copy categories failed: ${e.message}")
        }
    }

    // Month operations
    fun getCurrentMonth(): Month? {
        return _currentMonthFlow.value
    }

    // Navigate to previous month
    suspend fun navigateToPreviousMonth(): Month {
        val current = _currentMonthFlow.value
        return if (current != null) {
            val prevDate = LocalDate.of(current.year, current.month, 1).minusMonths(1)
            getOrCreateMonth(prevDate.year, prevDate.monthValue)
        } else {
            getOrCreateMonth(LocalDate.now().year, LocalDate.now().monthValue)
        }
    }

    // Navigate to next month
    suspend fun navigateToNextMonth(): Month {
        val current = _currentMonthFlow.value
        return if (current != null) {
            val nextDate = LocalDate.of(current.year, current.month, 1).plusMonths(1)
            getOrCreateMonth(nextDate.year, nextDate.monthValue)
        } else {
            getOrCreateMonth(LocalDate.now().year, LocalDate.now().monthValue)
        }
    }

    suspend fun getAllMonths(): List<Month> {
        return monthDao.getAllActiveMonths().first()
    }

    suspend fun deleteMonth(monthId: String): Result<Unit> {
        return try {
            // Get categories for this month
            val categories = categoryDao.getCategoriesForMonth(monthId).first()
            val categoryIds = categories.map { it.id }

            // Delete transactions for these categories
            if (categoryIds.isNotEmpty()) {
                transactionDao.deleteTransactionsForCategories(categoryIds)
            }

            // Delete categories for this month
            categoryDao.deleteCategoriesForMonth(monthId)

            // Delete the month
            monthDao.deleteMonth(monthId)

            // If we deleted the current month, clear it
            if (_currentMonthFlow.value?.id == monthId) {
                _currentMonthFlow.value = null
                ensureCurrentMonthExists() // Create current month again
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Category operations
    suspend fun createCategory(category: Category): Result<Category> {
        return try {
            val categoryWithMonthId = category.copy(
                monthId = getCurrentMonth()?.id ?: "default_month"
            )
            categoryDao.insertCategory(categoryWithMonthId)
            Result.success(categoryWithMonthId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllActiveCategories().first()
    }

    suspend fun getCategoriesByType(type: CategoryType): List<Category> {
        return getAllCategories().filter { it.type == type }
    }

    suspend fun getCategoriesForCurrentMonth(): List<Category> {
        val currentMonthId = getCurrentMonth()?.id ?: return emptyList()
        return getCategoriesForMonth(currentMonthId)
    }

    suspend fun getCategoriesForMonth(monthId: String): List<Category> {
        val categories = categoryDao.getCategoriesForMonth(monthId).first()

        // Update each category with actual amount from transactions
        return categories.map { category ->
            val actualAmount = getActualSpendingForCategory(category.id)
            category.copy(actualAmount = actualAmount)
        }
    }

    suspend fun getCategoriesForCurrentMonthByType(type: CategoryType): List<Category> {
        val currentMonthId = getCurrentMonth()?.id ?: return emptyList()
        val categories = categoryDao.getCategoriesByMonthAndType(currentMonthId, type)

        // Update each category with actual amount from transactions
        return categories.map { category ->
            val actualAmount = getActualSpendingForCategory(category.id)
            category.copy(actualAmount = actualAmount)
        }
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)
    }

    // Transaction operations
    suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            transactionDao.insertTransaction(transaction)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            transactionDao.updateTransaction(transaction)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionById(transactionId: String): Transaction? {
        return transactionDao.getTransactionById(transactionId)
    }

    suspend fun getTransactionsForCategory(categoryId: String): List<Transaction> {
        return transactionDao.getTransactionsForCategory(categoryId).first()
    }

    suspend fun getTransactionsForCurrentMonth(): List<Transaction> {
        val currentMonthCategories = getCategoriesForCurrentMonth().map { it.id }
        return if (currentMonthCategories.isNotEmpty()) {
            transactionDao.getTransactionsForCategories(currentMonthCategories)
        } else {
            emptyList()
        }
    }

    suspend fun getTransactionsSorted(
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
            transactionDao.deleteTransaction(transactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Budget calculations with real transaction data
    suspend fun getTotalIncome(): Double {
        val currentMonthId = getCurrentMonth()?.id ?: return 0.0
        return getCategoriesForCurrentMonthByType(CategoryType.INCOME).sumOf { it.targetAmount }
    }

    suspend fun getTotalExpenses(): Double {
        val currentMonthId = getCurrentMonth()?.id ?: return 0.0
        val categories = getCategoriesForCurrentMonth()
        return categories.filter { it.type != CategoryType.INCOME }.sumOf { it.targetAmount }
    }

    suspend fun getTotalByType(type: CategoryType): Double {
        return getCategoriesForCurrentMonthByType(type).sumOf { it.targetAmount }
    }

    suspend fun getRemainingBudget(): Double {
        return getTotalIncome() - getTotalExpenses()
    }

    // Calculate actual spending from transactions
    suspend fun getActualSpendingForCategory(categoryId: String): Double {
        return transactionDao.getTotalAmountForCategory(categoryId) ?: 0.0
    }

    suspend fun getActualIncomeEarned(): Double {
        val incomeCategories = getCategoriesForCurrentMonthByType(CategoryType.INCOME)
        return incomeCategories.sumOf { category ->
            getActualSpendingForCategory(category.id)
        }
    }

    suspend fun getActualExpensesSpent(): Double {
        val expenseCategories = getCategoriesForCurrentMonth().filter { it.type != CategoryType.INCOME }
        return expenseCategories.sumOf { category ->
            getActualSpendingForCategory(category.id)
        }
    }

    suspend fun getActualRemainingBudget(): Double {
        return getActualIncomeEarned() - getActualExpensesSpent()
    }

    // Transaction summaries
    suspend fun getTransactionSummaryForCategory(categoryId: String): TransactionSummary {
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
            categoryDao.updateCategory(category)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete category
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoryDao.deleteCategory(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}