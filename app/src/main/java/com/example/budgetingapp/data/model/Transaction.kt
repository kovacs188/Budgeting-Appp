package com.example.budgetingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = generateTransactionId(),
    val categoryId: String,
    val amount: Double,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val isActive: Boolean = true
) {
    val displayDate: String
        get() = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

    companion object {
        private fun generateTransactionId(): String {
            return "transaction_${System.currentTimeMillis()}"
        }
    }
}

// Helper data class for transaction creation form
data class TransactionFormState(
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val amountError: String? = null,
    val isValid: Boolean = false
) {
    fun validate(): TransactionFormState {
        val amountErrorMsg = when {
            amount.isBlank() -> "Amount is required"
            amount.toDoubleOrNull() == null -> "Enter a valid amount"
            amount.toDoubleOrNull()!! <= 0 -> "Amount must be greater than 0"
            amount.toDoubleOrNull()!! > 999999.99 -> "Amount is too large"
            else -> null
        }

        return copy(
            amountError = amountErrorMsg,
            isValid = amountErrorMsg == null
        )
    }
}

// Data class for transaction summaries
data class TransactionSummary(
    val categoryId: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val lastTransactionDate: LocalDate?
)

// Enum for transaction sorting
enum class TransactionSortOrder {
    DATE_DESC,    // Newest first
    DATE_ASC,     // Oldest first
    AMOUNT_DESC,  // Highest amount first
    AMOUNT_ASC    // Lowest amount first
}