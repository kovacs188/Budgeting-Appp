package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.database.CategoryDao
import com.example.budgetingapp.data.database.MonthDao
import com.example.budgetingapp.data.database.TransactionDao
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentMonthFlow = MutableStateFlow<Month?>(null)
    val currentMonthFlow: StateFlow<Month?> = _currentMonthFlow.asStateFlow()

    val allMonthsFlow: Flow<List<Month>> = monthDao.getAllActiveMonths()
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllActiveTransactions()
    val allCategoriesFlow: Flow<List<Category>> = categoryDao.getAllActiveCategories()

    // New project flows
    val activeProjectsFlow: Flow<List<Category>> = categoryDao.getActiveProjects()
    val completedProjectsFlow: Flow<List<Category>> = categoryDao.getCompletedProjects()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthCategoriesFlow: Flow<List<Category>> = _currentMonthFlow.flatMapLatest { month ->
        if (month == null) {
            flowOf(emptyList())
        } else {
            categoryDao.getCategoriesForMonth(month.id).map { categories ->
                categories.map { category ->
                    if (category.isProject) {
                        // For projects, calculate total spent across all transactions
                        val projectTotalSpent = calculateProjectTotalSpent(category.id)
                        category.copy(
                            projectTotalSpent = projectTotalSpent,
                            actualAmount = transactionDao.getTotalAmountForCategory(category.id) ?: 0.0
                        )
                    } else {
                        val actualAmount = transactionDao.getTotalAmountForCategory(category.id) ?: 0.0
                        category.copy(actualAmount = actualAmount)
                    }
                }
            }
        }
    }

    init {
        repositoryScope.launch {
            ensureCurrentMonthExists()
            // Update project totals periodically
            updateAllProjectTotals()
        }
    }

    private suspend fun ensureCurrentMonthExists() {
        val now = LocalDate.now()
        getOrCreateMonth(now.year, now.monthValue)
    }

    private suspend fun calculateProjectTotalSpent(projectId: String): Double {
        // Get all transactions for this project (including sub-projects)
        val projectCategories = categoryDao.getProjectWithSubProjects(projectId)
        val allProjectCategoryIds = projectCategories.map { it.id }

        return if (allProjectCategoryIds.isNotEmpty()) {
            transactionDao.getTransactionsForCategories(allProjectCategoryIds).sumOf { it.amount }
        } else {
            0.0
        }
    }

    private suspend fun updateAllProjectTotals() {
        val activeProjects = categoryDao.getActiveProjects().first()
        activeProjects.forEach { project ->
            val totalSpent = calculateProjectTotalSpent(project.id)
            categoryDao.updateProjectTotalSpent(project.id, totalSpent)
        }
    }

    suspend fun getOrCreateMonth(year: Int, monthValue: Int): Month {
        val existingMonth = monthDao.getMonthByYearAndMonth(year, monthValue)
        if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
            return existingMonth
        }

        val newMonthDate = LocalDate.of(year, monthValue, 1)
        val newMonth = Month(
            name = newMonthDate.format(DateTimeFormatter.ofPattern("MMMM")),
            year = newMonthDate.year,
            month = newMonthDate.monthValue
        )
        monthDao.insertMonth(newMonth)
        performBudgetRollover(newMonth, newMonthDate)
        _currentMonthFlow.value = newMonth
        return newMonth
    }

    private suspend fun performBudgetRollover(newMonth: Month, newMonthDate: LocalDate) {
        val prevMonthDate = newMonthDate.minusMonths(1)
        val previousMonth = monthDao.getMonthByYearAndMonth(prevMonthDate.year, prevMonthDate.monthValue) ?: return
        val previousCategories = getCategoriesForMonth(previousMonth.id)

        previousCategories.forEach { oldCategory ->
            // Don't rollover completed projects
            if (oldCategory.isProject && oldCategory.isProjectComplete) return@forEach

            val newRolloverBalance = if (oldCategory.isRolloverEnabled) oldCategory.amountAvailable else 0.0
            val newCategory = oldCategory.copy(
                id = Category.generateCategoryId(),
                monthId = newMonth.id,
                actualAmount = 0.0,
                rolloverBalance = newRolloverBalance,
                createdDate = LocalDateTime.now()
            )
            categoryDao.insertCategory(newCategory)

            if (newCategory.type == CategoryType.FIXED_EXPENSE && newCategory.targetAmount > 0) {
                val transaction = Transaction(
                    categoryId = newCategory.id,
                    amount = newCategory.targetAmount,
                    description = "Automatic transaction for fixed expense",
                    date = LocalDate.of(newMonth.year, newMonth.month, 1)
                )
                transactionDao.insertTransaction(transaction)
            }
        }
    }

    suspend fun navigateToPreviousMonth() {
        val current = _currentMonthFlow.value ?: return
        val prevDate = LocalDate.of(current.year, current.month, 1).minusMonths(1)
        getOrCreateMonth(prevDate.year, prevDate.monthValue)
    }

    suspend fun navigateToNextMonth() {
        val current = _currentMonthFlow.value ?: return
        val nextDate = LocalDate.of(current.year, current.month, 1).plusMonths(1)
        getOrCreateMonth(nextDate.year, nextDate.monthValue)
    }

    suspend fun createCategory(category: Category): Result<Unit> {
        return try {
            val currentMonth = _currentMonthFlow.value ?: return Result.failure(Exception("No active month found"))

            val categoryWithMonthId = if (category.isProject) {
                // Projects don't need month association and get special handling
                val maxOrder = categoryDao.getCategoriesForMonthOnce(currentMonth.id).maxOfOrNull { it.displayOrder } ?: -1
                category.copy(
                    monthId = currentMonth.id, // Still associate with current month for now
                    displayOrder = maxOrder + 1,
                    type = CategoryType.PROJECT_EXPENSE
                )
            } else {
                val maxOrder = categoryDao.getCategoriesForMonthOnce(currentMonth.id).maxOfOrNull { it.displayOrder } ?: -1
                category.copy(
                    monthId = currentMonth.id,
                    displayOrder = maxOrder + 1
                )
            }

            categoryDao.insertCategory(categoryWithMonthId)

            if (categoryWithMonthId.type == CategoryType.FIXED_EXPENSE && categoryWithMonthId.targetAmount > 0) {
                val transaction = Transaction(
                    categoryId = categoryWithMonthId.id,
                    amount = categoryWithMonthId.targetAmount,
                    description = "Automatic transaction for fixed expense",
                    date = LocalDate.of(currentMonth.year, currentMonth.month, 1)
                )
                transactionDao.insertTransaction(transaction)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(updatedCategory: Category): Result<Unit> {
        return try {
            val originalCategory = categoryDao.getCategoryById(updatedCategory.id)
                ?: return Result.failure(Exception("Original category not found"))

            categoryDao.updateCategory(updatedCategory)

            // Handle fixed expense auto-transactions (existing logic)
            if (!updatedCategory.isProject) {
                val autoTransactionDescription = "Automatic transaction for fixed expense"
                val existingAutoTransaction = transactionDao.getTransactionsForCategoryOnce(updatedCategory.id)
                    .find { it.description == autoTransactionDescription }

                val wasFixed = originalCategory.type == CategoryType.FIXED_EXPENSE
                val isNowFixed = updatedCategory.type == CategoryType.FIXED_EXPENSE

                if (isNowFixed) {
                    if (existingAutoTransaction != null) {
                        if (existingAutoTransaction.amount != updatedCategory.targetAmount) {
                            transactionDao.updateTransaction(
                                existingAutoTransaction.copy(amount = updatedCategory.targetAmount)
                            )
                        }
                    } else {
                        val currentMonth = _currentMonthFlow.value ?: return Result.failure(Exception("No active month"))
                        val transaction = Transaction(
                            categoryId = updatedCategory.id,
                            amount = updatedCategory.targetAmount,
                            description = autoTransactionDescription,
                            date = LocalDate.of(currentMonth.year, currentMonth.month, 1)
                        )
                        transactionDao.insertTransaction(transaction)
                    }
                } else if (wasFixed && !isNowFixed) {
                    existingAutoTransaction?.let {
                        transactionDao.deleteTransaction(it.id)
                    }
                }
            }

            // Update project totals if this is a project
            if (updatedCategory.isProject) {
                updateAllProjectTotals()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategoryOrder(categories: List<Category>) {
        categories.forEachIndexed { index, category ->
            categoryDao.updateCategory(category.copy(displayOrder = index))
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = try {
        transactionDao.deleteTransactionsForCategories(listOf(categoryId))
        categoryDao.deleteCategory(categoryId)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteMonth(monthId: String): Result<Unit> {
        return try {
            val categories = getCategoriesForMonth(monthId)
            val categoryIds = categories.map { it.id }

            if (categoryIds.isNotEmpty()) {
                transactionDao.deleteTransactionsForCategories(categoryIds)
            }
            categoryDao.deleteCategoriesForMonth(monthId)
            monthDao.deleteMonth(monthId)

            if (_currentMonthFlow.value?.id == monthId) {
                navigateToPreviousMonth()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Project-specific methods
    suspend fun createProject(
        name: String,
        description: String,
        totalBudget: Double,
        parentProjectId: String? = null
    ): Result<Category> {
        return try {
            val currentMonth = _currentMonthFlow.value ?: return Result.failure(Exception("No active month found"))

            val project = Category(
                name = name,
                type = CategoryType.PROJECT_EXPENSE,
                targetAmount = 0.0, // Projects don't use monthly target
                monthId = currentMonth.id,
                description = description,
                isProject = true,
                projectTotalBudget = totalBudget,
                parentProjectId = parentProjectId
            )

            categoryDao.insertCategory(project)
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markProjectComplete(projectId: String): Result<Unit> {
        return try {
            val completedDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            categoryDao.markProjectComplete(projectId, completedDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectWithSubProjects(projectId: String): List<Category> {
        return categoryDao.getProjectWithSubProjects(projectId)
    }

    // Existing methods continue...
    suspend fun getCategoriesForMonth(monthId: String): List<Category> {
        val categories = categoryDao.getCategoriesForMonthOnce(monthId)
        return categories.map { category ->
            if (category.isProject) {
                val projectTotalSpent = calculateProjectTotalSpent(category.id)
                category.copy(
                    projectTotalSpent = projectTotalSpent,
                    actualAmount = transactionDao.getTotalAmountForCategory(category.id) ?: 0.0
                )
            } else {
                val actualAmount = transactionDao.getTotalAmountForCategory(category.id) ?: 0.0
                category.copy(actualAmount = actualAmount)
            }
        }
    }

    suspend fun getCategoriesForCurrentMonthByType(type: CategoryType): List<Category> {
        val categories = currentMonthCategoriesFlow.first()
        return categories.filter { it.type == type }
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        val category = categoryDao.getCategoryById(categoryId)
        return if (category?.isProject == true) {
            val projectTotalSpent = calculateProjectTotalSpent(category.id)
            category.copy(
                projectTotalSpent = projectTotalSpent,
                actualAmount = transactionDao.getTotalAmountForCategory(categoryId) ?: 0.0
            )
        } else {
            category?.copy(
                actualAmount = transactionDao.getTotalAmountForCategory(categoryId) ?: 0.0
            )
        }
    }

    suspend fun createTransaction(transaction: Transaction): Result<Unit> = try {
        transactionDao.insertTransaction(transaction)

        // Update project totals if this transaction is for a project
        val category = categoryDao.getCategoryById(transaction.categoryId)
        if (category?.isProject == true) {
            updateAllProjectTotals()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Unit> = try {
        transactionDao.updateTransaction(transaction)

        // Update project totals if this transaction is for a project
        val category = categoryDao.getCategoryById(transaction.categoryId)
        if (category?.isProject == true) {
            updateAllProjectTotals()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = try {
        val transaction = transactionDao.getTransactionById(transactionId)
        transactionDao.deleteTransaction(transactionId)

        // Update project totals if this transaction was for a project
        if (transaction != null) {
            val category = categoryDao.getCategoryById(transaction.categoryId)
            if (category?.isProject == true) {
                updateAllProjectTotals()
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTransactionsForCategory(categoryId: String): List<Transaction> {
        return transactionDao.getTransactionsForCategory(categoryId).first()
    }

    fun getCurrentMonth(): Month? = _currentMonthFlow.value
}