package com.example.budgetingapp.di

import android.content.Context
import androidx.room.Room
import com.example.budgetingapp.data.database.BudgetDatabase
import com.example.budgetingapp.data.database.CategoryDao
import com.example.budgetingapp.data.database.MonthDao
import com.example.budgetingapp.data.database.TransactionDao
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBudgetDatabase(@ApplicationContext context: Context): BudgetDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BudgetDatabase::class.java,
            "budget_database"
        ).build()
    }

    @Provides
    fun provideMonthDao(database: BudgetDatabase): MonthDao {
        return database.monthDao()
    }

    @Provides
    fun provideCategoryDao(database: BudgetDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideTransactionDao(database: BudgetDatabase): TransactionDao {
        return database.transactionDao()
    }

    // Only provide BudgetRepository - it uses all the DAOs internally
    @Provides
    @Singleton
    fun provideBudgetRepository(
        monthDao: MonthDao,
        categoryDao: CategoryDao,
        transactionDao: TransactionDao
    ): BudgetRepository {
        return BudgetRepository(monthDao, categoryDao, transactionDao)
    }
}