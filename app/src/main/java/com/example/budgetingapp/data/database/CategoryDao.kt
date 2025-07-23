package com.example.budgetingapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY displayOrder ASC")
    fun getAllActiveCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE monthId = :monthId AND isActive = 1 ORDER BY displayOrder ASC")
    fun getCategoriesForMonth(monthId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE monthId = :monthId AND isActive = 1 ORDER BY displayOrder ASC")
    suspend fun getCategoriesForMonthOnce(monthId: String): List<Category>

    @Query("SELECT * FROM categories WHERE monthId = :monthId AND type = :type AND isActive = 1 ORDER BY displayOrder ASC")
    suspend fun getCategoriesByMonthAndType(monthId: String, type: CategoryType): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id AND isActive = 1 LIMIT 1")
    suspend fun getCategoryById(id: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("UPDATE categories SET isActive = 0 WHERE id = :categoryId")
    suspend fun deleteCategory(categoryId: String)

    @Query("UPDATE categories SET isActive = 0 WHERE monthId = :monthId")
    suspend fun deleteCategoriesForMonth(monthId: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
