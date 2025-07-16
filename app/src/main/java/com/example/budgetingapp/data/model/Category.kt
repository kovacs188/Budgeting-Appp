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
    val targetAmount: Double,
    val actualAmount: Double = 0.0,
    val monthId: String,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val isActive: Boolean = true,
    val description: String = ""
) {
    val remainingAmount: Double
        get() = when (type) {
            CategoryType.INCOME -> actualAmount - targetAmount
            else -> targetAmount - actualAmount
        }

    val isOverBudget: Boolean
        get() = when (type) {
            CategoryType.INCOME -> actualAmount < targetAmount
            else -> actualAmount > targetAmount
        }

    val displayType: String
        get() = when (type) {
            CategoryType.INCOME -> "Income"
            CategoryType.FIXED_EXPENSE -> "Fixed Expense"
            CategoryType.VARIABLE_EXPENSE -> "Variable Expense"
            CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense"
        }

    companion object {
        private fun generateCategoryId(): String {
            return "category_${System.currentTimeMillis()}"
        }
    }
}

// Helper data class for category creation form
data class CategoryFormState(
    val name: String = "",
    val type: CategoryType = CategoryType.INCOME,
    val targetAmount: String = "",
    val description: String = "",
    val nameError: String? = null,
    val amountError: String? = null,
    val isValid: Boolean = false
) {
    fun validate(): CategoryFormState {
        val nameErrorMsg = when {
            name.isBlank() -> "Category name is required"
            name.length < 2 -> "Category name must be at least 2 characters"
            else -> null
        }

        val amountErrorMsg = when {
            targetAmount.isBlank() -> "Target amount is required"
            targetAmount.toDoubleOrNull() == null -> "Enter a valid amount"
            targetAmount.toDoubleOrNull()!! < 0 -> "Amount must be positive"
            else -> null
        }

        return copy(
            nameError = nameErrorMsg,
            amountError = amountErrorMsg,
            isValid = nameErrorMsg == null && amountErrorMsg == null
        )
    }
}