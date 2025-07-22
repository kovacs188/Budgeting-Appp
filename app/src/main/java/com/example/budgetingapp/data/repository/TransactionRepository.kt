package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.database.TransactionDao
import com.example.budgetingapp.data.model.Transaction
import com.example.budgetingapp.data.model.TransactionSortOrder
import com.example.budgetingapp.data.model.TransactionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Custom exception types for better error handling
sealed class BudgetException(message: String, cause: Throwable? = null) : Exception(message, cause)

class TransactionNotFoundException(transactionId: String) :
    BudgetException("Transaction '$transactionId' not found")

class CategoryNotFoundException(categoryId: String) :
    BudgetException("Category '$categoryId' not found")

class DatabaseException(operation: String, cause: Throwable) :
    BudgetException("Database operation failed: $operation", cause)

class ValidationException(field: String, reason: String) :
    BudgetException("Validation failed for $field: $reason")

// Extension for user-friendly messages
fun Throwable.userFriendlyMessage(): String = when (this) {
    is TransactionNotFoundException -> "Transaction not found. It may have been deleted."
    is CategoryNotFoundException -> "Category not found. Please refresh and try again."
    is ValidationException -> message ?: "Please check your input."
    is DatabaseException -> "Unable to save data. Please try again."
    else -> "An unexpected error occurred. Please try again."
}

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    // StateFlow for reactive updates
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllActiveTransactions()

    suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            // Validate transaction before creating
            validateTransaction(transaction)
                .onFailure { return Result.failure(it) }

            transactionDao.insertTransaction(transaction)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(DatabaseException("create transaction", e))
        }
    }

    suspend fun updateTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            // Validate transaction before updating
            validateTransaction(transaction)
                .onFailure { return Result.failure(it) }

            // Check if transaction exists
            val existingTransaction = transactionDao.getTransactionById(transaction.id)
            if (existingTransaction == null) {
                return Result.failure(TransactionNotFoundException(transaction.id))
            }

            transactionDao.updateTransaction(transaction)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(DatabaseException("update transaction", e))
        }
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> {
        return try {
            // Check if transaction exists before deleting
            val existingTransaction = transactionDao.getTransactionById(transactionId)
            if (existingTransaction == null) {
                return Result.failure(TransactionNotFoundException(transactionId))
            }

            transactionDao.deleteTransaction(transactionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("delete transaction", e))
        }
    }

    suspend fun getTransactionById(transactionId: String): Result<Transaction> {
        return try {
            val transaction = transactionDao.getTransactionById(transactionId)
            if (transaction != null) {
                Result.success(transaction)
            } else {
                Result.failure(TransactionNotFoundException(transactionId))
            }
        } catch (e: Exception) {
            Result.failure(DatabaseException("get transaction", e))
        }
    }

    suspend fun getTransactionsForCategory(categoryId: String): Result<List<Transaction>> {
        return try {
            val transactions = transactionDao.getTransactionsForCategory(categoryId).first()
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(DatabaseException("get transactions for category", e))
        }
    }

    suspend fun getTransactionsForCategories(categoryIds: List<String>): Result<List<Transaction>> {
        return try {
            val transactions = if (categoryIds.isNotEmpty()) {
                transactionDao.getTransactionsForCategories(categoryIds)
            } else {
                emptyList()
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(DatabaseException("get transactions for categories", e))
        }
    }

    suspend fun getTransactionsSorted(
        categoryId: String? = null,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC
    ): Result<List<Transaction>> {
        return try {
            val transactionsResult = if (categoryId != null) {
                getTransactionsForCategory(categoryId)
            } else {
                Result.success(transactionDao.getAllActiveTransactions().first())
            }

            transactionsResult.map { transactions ->
                when (sortOrder) {
                    TransactionSortOrder.DATE_DESC -> transactions.sortedByDescending { it.date }
                    TransactionSortOrder.DATE_ASC -> transactions.sortedBy { it.date }
                    TransactionSortOrder.AMOUNT_DESC -> transactions.sortedByDescending { it.amount }
                    TransactionSortOrder.AMOUNT_ASC -> transactions.sortedBy { it.amount }
                }
            }
        } catch (e: Exception) {
            Result.failure(DatabaseException("sort transactions", e))
        }
    }

    suspend fun deleteTransactionsForCategories(categoryIds: List<String>): Result<Unit> {
        return try {
            if (categoryIds.isNotEmpty()) {
                transactionDao.deleteTransactionsForCategories(categoryIds)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("delete transactions for categories", e))
        }
    }

    // ✅ FIXED: Now returns Result<Double> instead of nullable Double
    suspend fun getActualSpendingForCategory(categoryId: String): Result<Double> {
        return try {
            val amount = transactionDao.getTotalAmountForCategory(categoryId)
            Result.success(amount ?: 0.0)
        } catch (e: Exception) {
            Result.failure(DatabaseException("calculate spending for category", e))
        }
    }

    // Transaction summaries with better error handling
    suspend fun getTransactionSummaryForCategory(categoryId: String): Result<TransactionSummary> {
        return try {
            getTransactionsForCategory(categoryId)
                .map { transactions ->
                    TransactionSummary(
                        categoryId = categoryId,
                        totalAmount = transactions.sumOf { it.amount },
                        transactionCount = transactions.size,
                        lastTransactionDate = transactions.maxByOrNull { it.date }?.date
                    )
                }
        } catch (e: Exception) {
            Result.failure(DatabaseException("generate transaction summary", e))
        }
    }

    // Private validation method
    private fun validateTransaction(transaction: Transaction): Result<Unit> {
        return when {
            transaction.amount <= 0 ->
                Result.failure(ValidationException("amount", "Amount must be greater than 0"))

            transaction.categoryId.isBlank() ->
                Result.failure(ValidationException("category", "Category is required"))

            transaction.amount > 999999.99 ->
                Result.failure(ValidationException("amount", "Amount is too large"))

            else -> Result.success(Unit)
        }
    }
}