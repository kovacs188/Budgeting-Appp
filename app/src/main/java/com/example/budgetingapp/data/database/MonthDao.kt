package com.example.budgetingapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.budgetingapp.data.model.Month
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthDao {

    @Query("SELECT * FROM months WHERE isActive = 1 ORDER BY year DESC, month DESC")
    fun getAllActiveMonths(): Flow<List<Month>>

    @Query("SELECT * FROM months WHERE year = :year AND month = :month AND isActive = 1 LIMIT 1")
    suspend fun getMonthByYearAndMonth(year: Int, month: Int): Month?

    @Query("SELECT * FROM months WHERE id = :id AND isActive = 1 LIMIT 1")
    suspend fun getMonthById(id: String): Month?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonth(month: Month)

    @Update
    suspend fun updateMonth(month: Month)

    // ** THE FIX IS HERE **
    // This now performs a hard delete, which is better for testing purposes.
    @Query("DELETE FROM months WHERE id = :monthId")
    suspend fun deleteMonth(monthId: String)

    @Query("DELETE FROM months")
    suspend fun deleteAllMonths()
}
