package com.example.budgetingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class CategoryType {
    INCOME,
    FIXED_EXPENSE,
    VARIABLE_EXPENSE,
    DISCRETIONARY_EXPENSE,
    PROJECT_EXPENSE // New project type
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = generateCategoryId(),
    val name: String,
    val type: CategoryType,
    val targetAmount: Double, // For projects: the total budget
    val actualAmount: Double = 0.0, // How much has been spent/earned this month
    val monthId: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,
    val description: String = "",
    // Existing rollover functionality
    val isRolloverEnabled: Boolean = false,
    val rolloverBalance: Double = 0.0,
    val displayOrder: Int = 0,
    // New project fields
    val isProject: Boolean = false, // Is this a project category?
    val projectTotalBudget: Double = 0.0, // Total project budget (across all time)
    val projectTotalSpent: Double = 0.0, // Total spent on project (across all time)
    val isProjectComplete: Boolean = false, // Has the project been marked complete?
    val projectCompletedDate: LocalDateTime? = null, // When was it completed?
    val parentProjectId: String? = null // For sub-projects (like your carpet example)
) {
    // For regular categories: funds available this month
    val totalBudgeted: Double
        get() = if (isProject) projectTotalBudget else (targetAmount + rolloverBalance)

    // For projects: remaining budget. For regular: remaining this month
    val amountAvailable: Double
        get() = if (isProject) (projectTotalBudget - projectTotalSpent) else (totalBudgeted - actualAmount)

    // For projects: progress against total budget. For regular: progress this month
    val progress: Float
        get() = if (isProject) {
            if (projectTotalBudget > 0) (projectTotalSpent / projectTotalBudget).toFloat().coerceIn(0f, 1f) else 0f
        } else {
            if (totalBudgeted > 0) (actualAmount / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
        }

    // Over budget logic
    val isOverBudget: Boolean
        get() = if (isProject) (projectTotalSpent > projectTotalBudget) else (actualAmount > totalBudgeted)

    val displayType: String
        get() = when (type) {
            CategoryType.INCOME -> "Income"
            CategoryType.FIXED_EXPENSE -> "Fixed Expense"
            CategoryType.VARIABLE_EXPENSE -> "Variable Expense"
            CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense"
            CategoryType.PROJECT_EXPENSE -> if (isProject) "Project" else "One-Time Expense"
        }

    // Project-specific helpers
    val projectProgress: String
        get() = if (isProject) {
            "$${String.format("%,.0f", projectTotalSpent)} of $${String.format("%,.0f", projectTotalBudget)}"
        } else ""

    val isProjectOverBudget: Boolean
        get() = isProject && projectTotalSpent > projectTotalBudget

    val projectRemainingBudget: Double
        get() = if (isProject) (projectTotalBudget - projectTotalSpent) else 0.0

    companion object {
        internal fun generateCategoryId(): String {
            return "category_${System.currentTimeMillis()}"
        }
    }
}

// Updated form state to include project fields
data class CategoryFormState(
    val name: String = "",
    val type: CategoryType = CategoryType.DISCRETIONARY_EXPENSE,
    val targetAmount: String = "",
    val description: String = "",
    val isRolloverEnabled: Boolean = false,
    // New project fields
    val isProject: Boolean = false,
    val projectTotalBudget: String = "",
    val parentProjectId: String? = null,
    // Validation
    val nameError: String? = null,
    val amountError: String? = null,
    val projectBudgetError: String? = null,
    val isValid: Boolean = false
) {
    fun validate(): CategoryFormState {
        val nameErrorMsg = when {
            name.isBlank() -> "Category name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val amountErrorMsg = if (!isProject) {
            // Only validate targetAmount for regular categories, not projects
            when {
                targetAmount.isBlank() -> "Budget amount is required"
                targetAmount.toDoubleOrNull() == null -> "Enter a valid amount"
                targetAmount.toDoubleOrNull()!! < 0 -> "Amount must not be negative"
                else -> null
            }
        } else null

        val projectBudgetErrorMsg = if (isProject) {
            when {
                projectTotalBudget.isBlank() -> "Project total budget is required"
                projectTotalBudget.toDoubleOrNull() == null -> "Enter a valid project budget"
                projectTotalBudget.toDoubleOrNull()!! <= 0 -> "Project budget must be greater than 0"
                else -> null
            }
        } else null

        return copy(
            nameError = nameErrorMsg,
            amountError = amountErrorMsg,
            projectBudgetError = projectBudgetErrorMsg,
            isValid = nameErrorMsg == null && amountErrorMsg == null && projectBudgetErrorMsg == null
        )
    }
}