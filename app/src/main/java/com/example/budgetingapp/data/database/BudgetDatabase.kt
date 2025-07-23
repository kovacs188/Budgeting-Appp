package com.example.budgetingapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.Month
import com.example.budgetingapp.data.model.Transaction

@Database(
    entities = [Month::class, Category::class, Transaction::class],
    version = 2, // Incremented for project fields
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

        // Migration from version 1 to 2 (adding project fields)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN isProject INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN projectTotalBudget REAL NOT NULL DEFAULT 0.0
                """)
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN projectTotalSpent REAL NOT NULL DEFAULT 0.0
                """)
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN isProjectComplete INTEGER NOT NULL DEFAULT 0
                """)
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN projectCompletedDate TEXT
                """)
                database.execSQL("""
                    ALTER TABLE categories ADD COLUMN parentProjectId TEXT
                """)
            }
        }

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}