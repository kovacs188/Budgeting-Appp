package com.example.budgetingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class CategoryType {
    INCOME,
    FIXED_EXPENSE,
    VARIABLE_EXPENSE,
    DISCRETIONARY_EXPENSE
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = generateCategoryId(),
    val name: String,
    val type: CategoryType,
    val targetAmount: Double, // The amount budgeted for this month
    val actualAmount: Double = 0.0, // How much has been spent/earned this month
    val monthId: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,
    val description: String = "",
    // New fields for rollover functionality
    val isRolloverEnabled: Boolean = false, // Does this category carry a balance?
    val rolloverBalance: Double = 0.0, // The balance carried over from the previous month
    val displayOrder: Int = 0 // New field for display order
) {
    // Total funds available for this category in the current month
    val totalBudgeted: Double
        get() = targetAmount + rolloverBalance

    // How much is left to spend/earn from the total available funds
    val amountAvailable: Double
        get() = totalBudgeted - actualAmount

    // Progress against the total available funds
    val progress: Float
        get() = if (totalBudgeted > 0) (actualAmount / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f

    // A category is over budget only if the user has spent more than the total funds available.
    val isOverBudget: Boolean
        get() = actualAmount > totalBudgeted

    val displayType: String
        get() = when (type) {
            CategoryType.INCOME -> "Income"
            CategoryType.FIXED_EXPENSE -> "Fixed Expense"
            CategoryType.VARIABLE_EXPENSE -> "Variable Expense"
            CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense"
        }

    companion object {
        internal fun generateCategoryId(): String {
            return "category_${System.currentTimeMillis()}"
        }
    }
}

// Updated form state to include the rollover flag
data class CategoryFormState(
    val name: String = "",
    val type: CategoryType = CategoryType.DISCRETIONARY_EXPENSE,
    val targetAmount: String = "",
    val description: String = "",
    val isRolloverEnabled: Boolean = false, // New field for the form
    val nameError: String? = null,
    val amountError: String? = null,
    val isValid: Boolean = false
) {
    fun validate(): CategoryFormState {
        val nameErrorMsg = when {
            name.isBlank() -> "Category name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }

        val amountErrorMsg = when {
            targetAmount.isBlank() -> "Budget amount is required"
            targetAmount.toDoubleOrNull() == null -> "Enter a valid amount"
            targetAmount.toDoubleOrNull()!! < 0 -> "Amount must not be negative"
            else -> null
        }

        return copy(
            nameError = nameErrorMsg,
            amountError = amountErrorMsg,
            isValid = nameErrorMsg == null && amountErrorMsg == null
        )
    }
}
