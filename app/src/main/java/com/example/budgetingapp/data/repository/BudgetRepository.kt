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
            // If current month doesn't exist, create it with full rollover
            val currentMonth = Month.createCurrentMonth()
            monthDao.insertMonth(currentMonth)
            _currentMonthFlow.value = currentMonth

            // Perform rollover from previous month if it exists
            val prevDate = now.minusMonths(1)
            val previousMonth = monthDao.getMonthByYearAndMonth(prevDate.year, prevDate.monthValue)
            if (previousMonth != null) {
                performBudgetRollover(currentMonth, now.withDayOfMonth(1))
            }
        }
    }

    // New: Check if a month is "preview only" (created but not committed)
    suspend fun isMonthPreviewOnly(monthId: String): Boolean {
        val month = monthDao.getMonthById(monthId) ?: return false
        val transactions = getTransactionsForMonth(monthId)
        val categories = getCategoriesForMonth(monthId)

        // Consider it preview-only if:
        // 1. No manual transactions (only auto-filled fixed expenses)
        // 2. Categories have preview descriptions or very few categories
        val hasManualTransactions = transactions.any {
            !it.description.contains("Auto-filled") && !it.description.contains("Preview")
        }
        val hasPreviewCategories = categories.any {
            it.description.contains("Preview - adjust as needed")
        }

        return !hasManualTransactions && (hasPreviewCategories || categories.size <= 2)
    }

    // Enhanced: Get or create month with preview mode detection
    suspend fun getOrCreateMonth(year: Int, monthValue: Int): Month {
        val existingMonth = monthDao.getMonthByYearAndMonth(year, monthValue)

        return if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
            existingMonth
        } else {
            val newMonth = createNewMonth(year, monthValue)
            _currentMonthFlow.value = newMonth
            newMonth
        }
    }

    private suspend fun createNewMonth(year: Int, monthValue: Int): Month {
        val newMonthDate = LocalDate.of(year, monthValue, 1)
        val currentDate = LocalDate.now()

        // Determine if this should be a preview month
        val isFutureMonth = newMonthDate.isAfter(currentDate.withDayOfMonth(1))

        val newMonth = Month(
            name = newMonthDate.format(DateTimeFormatter.ofPattern("MMMM")),
            year = newMonthDate.year,
            month = newMonthDate.monthValue
        )
        monthDao.insertMonth(newMonth)

        if (isFutureMonth) {
            // Create preview structure for future months
            createPreviewCategories(newMonth)
        } else {
            // For current/past months, do full rollover
            performBudgetRollover(newMonth, newMonthDate)
        }

        return newMonth
    }

    private suspend fun performBudgetRollover(newMonth: Month, newMonthDate: LocalDate) {
        val previousMonthDate = newMonthDate.minusMonths(1)
        val previousMonth = monthDao.getMonthByYearAndMonth(
            previousMonthDate.year,
            previousMonthDate.monthValue
        )

        if (previousMonth != null) {
            val previousCategories = categoryDao.getCategoriesForMonth(previousMonth.id).first()

            previousCategories.forEach { oldCategory ->
                val newCategory = oldCategory.copy(
                    id = Category.generateCategoryId(),
                    monthId = newMonth.id,
                    actualAmount = 0.0,
                    createdDate = java.time.LocalDateTime.now()
                )
                categoryDao.insertCategory(newCategory)

                // Auto-fill fixed expenses
                if (newCategory.type == CategoryType.FIXED_EXPENSE && newCategory.targetAmount > 0) {
                    val autoTransaction = Transaction(
                        categoryId = newCategory.id,
                        amount = newCategory.targetAmount,
                        description = "Auto-filled fixed expense for new month",
                        date = newMonthDate.withDayOfMonth(1)
                    )
                    transactionDao.insertTransaction(autoTransaction)
                }
            }
        }
    }

    private suspend fun createPreviewCategories(newMonth: Month) {
        // Create basic category structure for preview without auto-filling
        val basicCategories = listOf(
            Category(
                name = "Income",
                type = CategoryType.INCOME,
                targetAmount = 0.0,
                monthId = newMonth.id,
                description = "Preview - adjust as needed"
            ),
            Category(
                name = "Fixed Expenses",
                type = CategoryType.FIXED_EXPENSE,
                targetAmount = 0.0,
                monthId = newMonth.id,
                description = "Preview - adjust as needed"
            )
        )

        basicCategories.forEach { category ->
            categoryDao.insertCategory(category)
        }
    }

    // New: Convert preview month to committed month
    suspend fun commitPreviewMonth(monthId: String): Result<Unit> {
        return try {
            val month = monthDao.getMonthById(monthId)
            if (month != null && isMonthPreviewOnly(monthId)) {
                // Clear preview categories
                categoryDao.deleteCategoriesForMonth(monthId)

                // Perform full rollover now
                val monthDate = LocalDate.of(month.year, month.month, 1)
                performBudgetRollover(month, monthDate)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get or create a month for specific year/month (keeping original for compatibility)
    suspend fun getOrCreateMonthLegacy(year: Int, monthValue: Int): Month {
        val existingMonth = monthDao.getMonthByYearAndMonth(year, monthValue)

        return if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
            existingMonth
        } else {
            // New month, so create it and perform rollover
            val newMonthDate = LocalDate.of(year, monthValue, 1)
            val newMonth = Month(
                name = newMonthDate.format(DateTimeFormatter.ofPattern("MMMM")),
                year = newMonthDate.year,
                month = newMonthDate.monthValue
            )
            monthDao.insertMonth(newMonth) // Insert the new month first

            // --- Budget Rollover Logic ---
            val previousMonthDate = newMonthDate.minusMonths(1)
            val previousMonth = monthDao.getMonthByYearAndMonth(previousMonthDate.year, previousMonthDate.monthValue)

            if (previousMonth != null) {
                val previousMonthCategories = categoryDao.getCategoriesForMonth(previousMonth.id).first()

                previousMonthCategories.forEach { oldCategory ->
                    val newCategory = oldCategory.copy(
                        id = Category.generateCategoryId(), // Generate new ID for the copied category
                        monthId = newMonth.id,              // Link to the new month
                        actualAmount = 0.0,                 // Reset actual amount for the new month
                        createdDate = java.time.LocalDateTime.now() // Set new creation date
                    )
                    categoryDao.insertCategory(newCategory)

                    // --- Auto-fill Fixed Expenses ---
                    if (newCategory.type == CategoryType.FIXED_EXPENSE) {
                        if (newCategory.targetAmount > 0) { // Only pre-fill if there's a budget
                            val autoTransaction = Transaction(
                                categoryId = newCategory.id,
                                amount = newCategory.targetAmount,
                                description = "Auto-filled fixed expense for new month",
                                date = newMonthDate.withDayOfMonth(1) // Set date to the first of the new month
                            )
                            transactionDao.insertTransaction(autoTransaction)
                        }
                    }
                }
            }
            // --- End Budget Rollover Logic ---

            _currentMonthFlow.value = newMonth
            newMonth
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

    // Enhanced: Delete month with safety checks
    suspend fun deleteMonth(monthId: String, force: Boolean = false): Result<Unit> {
        return try {
            val month = monthDao.getMonthById(monthId)
            if (month == null) {
                return Result.failure(Exception("Month not found"))
            }

            val currentDate = LocalDate.now()
            val monthDate = LocalDate.of(month.year, month.month, 1)
            val isCurrentOrPastMonth = !monthDate.isAfter(currentDate.withDayOfMonth(1))

            // Safety checks
            if (!force && isCurrentOrPastMonth) {
                return Result.failure(Exception("Cannot delete current or past months without force flag"))
            }

            if (!force && !isMonthPreviewOnly(monthId)) {
                return Result.failure(Exception("Month has been modified. Use individual category deletion or force delete."))
            }

            // Proceed with deletion
            val categories = categoryDao.getCategoriesForMonth(monthId).first()
            val categoryIds = categories.map { it.id }

            if (categoryIds.isNotEmpty()) {
                transactionDao.deleteTransactionsForCategories(categoryIds)
            }

            categoryDao.deleteCategoriesForMonth(monthId)
            monthDao.deleteMonth(monthId)

            if (_currentMonthFlow.value?.id == monthId) {
                _currentMonthFlow.value = null
                ensureCurrentMonthExists()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper: Get months that can be safely deleted
    suspend fun getDeletableMonths(): List<Month> {
        val allMonths = getAllMonths()
        val currentDate = LocalDate.now()

        return allMonths.filter { month ->
            val monthDate = LocalDate.of(month.year, month.month, 1)
            val isFutureMonth = monthDate.isAfter(currentDate.withDayOfMonth(1))
            isFutureMonth && isMonthPreviewOnly(month.id)
        }
    }

    // Helper: Get transactions for a specific month
    private suspend fun getTransactionsForMonth(monthId: String): List<Transaction> {
        val categories = categoryDao.getCategoriesForMonth(monthId).first()
        val categoryIds = categories.map { it.id }
        return if (categoryIds.isNotEmpty()) {
            transactionDao.getTransactionsForCategories(categoryIds)
        } else {
            emptyList()
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

    suspend fun updateCategory(category: Category): Result<Category> {
        return try {
            categoryDao.updateCategory(category)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoryDao.deleteCategory(categoryId)
            Result.success(Unit)
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

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            transactionDao.deleteTransaction(transactionId)
            Result.success(Unit)
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

    // Budget calculations with real transaction data
    suspend fun getTotalIncome(): Double {
        return getCategoriesForCurrentMonthByType(CategoryType.INCOME).sumOf { it.targetAmount }
    }

    suspend fun getTotalExpenses(): Double {
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
}