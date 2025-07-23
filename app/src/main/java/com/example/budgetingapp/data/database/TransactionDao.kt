package com.example.budgetingapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgetingapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isActive = 1 ORDER BY date DESC")
    fun getAllActiveTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND isActive = 1 ORDER BY date DESC")
    fun getTransactionsForCategory(categoryId: String): Flow<List<Transaction>>

    // ** THE FIX IS HERE **
    // New function to get a one-time list of transactions, needed for the update logic.
    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND isActive = 1")
    suspend fun getTransactionsForCategoryOnce(categoryId: String): List<Transaction>

    @Query("SELECT * FROM transactions WHERE categoryId IN (:categoryIds) AND isActive = 1 ORDER BY date DESC")
    suspend fun getTransactionsForCategories(categoryIds: List<String>): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id AND isActive = 1 LIMIT 1")
    suspend fun getTransactionById(id: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET isActive = 0 WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("UPDATE transactions SET isActive = 0 WHERE categoryId IN (:categoryIds)")
    suspend fun deleteTransactionsForCategories(categoryIds: List<String>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId AND isActive = 1")
    suspend fun getTotalAmountForCategory(categoryId: String): Double?
}
