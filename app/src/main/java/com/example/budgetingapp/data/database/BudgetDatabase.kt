package com.example.budgetingapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction

@Database(
    entities = [Month::class, Category::class, Transaction::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun monthDao(): MonthDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}