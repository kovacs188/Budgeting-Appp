package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.model.CategoryType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetCalculationService @Inject constructor(
    private val monthRepository: MonthRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {

    // Budget calculations with target amounts
    suspend fun getTotalIncome(): Double {
        val currentMonthId = monthRepository.getCurrentMonth()?.id ?: return 0.0
        val categories = getCategoriesForCurrentMonthByType(CategoryType.INCOME)
        return categories.sumOf { it.targetAmount }
    }

    suspend fun getTotalExpenses(): Double {
        val currentMonthId = monthRepository.getCurrentMonth()?.id ?: return 0.0
        val categories = getCategoriesForCurrentMonth()
        return categories.filter { it.type != CategoryType.INCOME }.sumOf { it.targetAmount }
    }

    suspend fun getTotalByType(type: CategoryType): Double {
        val categories = getCategoriesForCurrentMonthByType(type)
        return categories.sumOf { it.targetAmount }
    }

    suspend fun getRemainingBudget(): Double {
        return getTotalIncome() - getTotalExpenses()
    }

    // Actual spending calculations from transactions
    suspend fun getActualIncomeEarned(): Double {
        val incomeCategories = getCategoriesForCurrentMonthByType(CategoryType.INCOME)
        var total = 0.0
        for (category in incomeCategories) {
            val spendingResult = transactionRepository.getActualSpendingForCategory(category.id)
            total += spendingResult.getOrElse { 0.0 }
        }
        return total
    }

    suspend fun getActualExpensesSpent(): Double {
        val expenseCategories = getCategoriesForCurrentMonth().filter { it.type != CategoryType.INCOME }
        var total = 0.0
        for (category in expenseCategories) {
            val spendingResult = transactionRepository.getActualSpendingForCategory(category.id)
            total += spendingResult.getOrElse { 0.0 }
        }
        return total
    }

    suspend fun getActualRemainingBudget(): Double {
        return getActualIncomeEarned() - getActualExpensesSpent()
    }

    // Budget vs Actual Analysis
    suspend fun getBudgetVariance(): Double {
        val budgetedRemaining = getRemainingBudget()
        val actualRemaining = getActualRemainingBudget()
        return actualRemaining - budgetedRemaining
    }

    suspend fun getIncomeVariance(): Double {
        val budgetedIncome = getTotalIncome()
        val actualIncome = getActualIncomeEarned()
        return actualIncome - budgetedIncome
    }

    suspend fun getExpenseVariance(): Double {
        val budgetedExpenses = getTotalExpenses()
        val actualExpenses = getActualExpensesSpent()
        return budgetedExpenses - actualExpenses // Positive means under budget
    }

    // Helper methods to get categories for current month
    private suspend fun getCategoriesForCurrentMonth() =
        categoryRepository.getCategoriesForMonth(
            monthRepository.getCurrentMonth()?.id ?: ""
        )

    private suspend fun getCategoriesForCurrentMonthByType(type: CategoryType) =
        categoryRepository.getCategoriesByMonthAndType(
            monthRepository.getCurrentMonth()?.id ?: "",
            type
        )

    // Monthly financial health score (0-100)
    suspend fun getFinancialHealthScore(): Int {
        val actualRemaining = getActualRemainingBudget()
        val totalIncome = getActualIncomeEarned()

        if (totalIncome <= 0) return 0

        val savingsRate = (actualRemaining / totalIncome).coerceIn(-1.0, 1.0)
        return ((savingsRate + 1.0) * 50).toInt() // Convert to 0-100 scale
    }

    // Category-specific calculations
    suspend fun getCategorySpendingPercentage(categoryId: String): Double {
        val category = categoryRepository.getCategoryById(categoryId) ?: return 0.0
        if (category.targetAmount <= 0) return 0.0

        return (category.actualAmount / category.targetAmount).coerceIn(0.0, 2.0) // Cap at 200%
    }

    suspend fun getCategoryRemainingDays(categoryId: String): Int? {
        val category = categoryRepository.getCategoryById(categoryId) ?: return null

        if (category.type == CategoryType.INCOME || category.remainingAmount <= 0) return null

        val transactionsResult = transactionRepository.getTransactionsForCategory(categoryId)
        val transactions = transactionsResult.getOrElse { return null }

        val sevenDaysAgo = LocalDate.now().minusDays(7)
        val recentTransactions = transactions.filter { transaction ->
            transaction.date.isAfter(sevenDaysAgo)
        }

        if (recentTransactions.isEmpty()) return null

        val totalAmount = recentTransactions.sumOf { it.amount }
        val dailySpendRate = totalAmount / 7.0
        if (dailySpendRate <= 0) return null

        return (category.remainingAmount / dailySpendRate).toInt()
    }
}