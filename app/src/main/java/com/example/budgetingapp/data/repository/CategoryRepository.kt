package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.database.CategoryDao
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    // StateFlow for reactive updates
    val categoriesFlow: Flow<List<Category>> = categoryDao.getAllActiveCategories()

    suspend fun createCategory(category: Category): Result<Category> {
        return try {
            categoryDao.insertCategory(category)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCategory(category: Category): Result<Category> {
        return try {
            categoryDao.updateCategory(category)
            Result.success(category)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            categoryDao.deleteCategory(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllActiveCategories().first()
    }

    suspend fun getCategoriesByType(type: CategoryType): List<Category> {
        return getAllCategories().filter { it.type == type }
    }

    suspend fun getCategoriesForMonth(monthId: String): List<Category> {
        return categoryDao.getCategoriesForMonth(monthId).first()
    }

    suspend fun getCategoriesByMonthAndType(monthId: String, type: CategoryType): List<Category> {
        return categoryDao.getCategoriesByMonthAndType(monthId, type)
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)
    }

    suspend fun deleteCategoriesForMonth(monthId: String): Result<Unit> {
        return try {
            categoryDao.deleteCategoriesForMonth(monthId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Auto-copy categories from the most recent month
    suspend fun copyPreviousMonthCategories(newMonthId: String, monthRepository: MonthRepository) {
        try {
            // Get all months ordered by most recent first
            val allMonths = monthRepository.getAllMonths()

            // Find the most recent month (excluding the one we just created)
            val previousMonth = allMonths.firstOrNull { it.id != newMonthId }

            if (previousMonth != null) {
                // Get categories from the previous month
                val previousCategories = categoryDao.getCategoriesForMonth(previousMonth.id).first()

                // Copy each category to the new month with smart spending logic
                previousCategories.forEach { category ->
                    val newActualAmount = when (category.type) {
                        CategoryType.FIXED_EXPENSE -> {
                            // Fixed expenses: carry over spending (rent, insurance, etc.)
                            category.actualAmount
                        }
                        CategoryType.INCOME,
                        CategoryType.VARIABLE_EXPENSE,
                        CategoryType.DISCRETIONARY_EXPENSE -> {
                            // All others: reset to $0 for fresh tracking
                            0.0
                        }
                    }

                    val newCategory = category.copy(
                        id = "category_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}",
                        monthId = newMonthId,
                        actualAmount = newActualAmount,
                        createdDate = LocalDateTime.now()
                    )
                    categoryDao.insertCategory(newCategory)
                }

                // Log for debugging (remove in production)
                val fixedExpenseCount = previousCategories.count { it.type == CategoryType.FIXED_EXPENSE }
                println("✅ Auto-copied ${previousCategories.size} categories from ${previousMonth.displayName}")
                println("   → Fixed expenses ($fixedExpenseCount): spending carried over")
                println("   → Other categories: spending reset to $0")
            } else {
                println("ℹ️ No previous month found - user will create categories manually")
            }

        } catch (e: Exception) {
            // Don't crash if auto-copy fails - user can still create categories manually
            println("⚠️ Auto-copy categories failed: ${e.message}")
        }
    }
}