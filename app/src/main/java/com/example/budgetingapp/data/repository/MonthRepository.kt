package com.example.budgetingapp.data.repository

import com.example.budgetingapp.data.database.MonthDao
import com.example.budgetingapp.data.model.Month
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthRepository @Inject constructor(
    private val monthDao: MonthDao
) {

    // Create a coroutine scope for repository operations
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentMonthFlow = MutableStateFlow<Month?>(null)
    val currentMonthFlow: StateFlow<Month?> = _currentMonthFlow.asStateFlow()

    init {
        // Auto-create current month on app start
        repositoryScope.launch {
            ensureCurrentMonthExists()
        }
    }

    // Auto-create current month if it doesn't exist
    private suspend fun ensureCurrentMonthExists() {
        val now = LocalDate.now()
        val existingMonth = monthDao.getMonthByYearAndMonth(now.year, now.monthValue)

        if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
        } else {
            val currentMonth = Month.createCurrentMonth()
            monthDao.insertMonth(currentMonth)
            _currentMonthFlow.value = currentMonth
        }
    }

    // Get or create a month for specific year/month
    suspend fun getOrCreateMonth(year: Int, monthValue: Int): Month {
        val existingMonth = monthDao.getMonthByYearAndMonth(year, monthValue)

        return if (existingMonth != null) {
            _currentMonthFlow.value = existingMonth
            existingMonth
        } else {
            val newMonth = Month(
                name = LocalDate.of(year, monthValue, 1).format(DateTimeFormatter.ofPattern("MMMM")),
                year = year,
                month = monthValue
            )
            monthDao.insertMonth(newMonth)
            _currentMonthFlow.value = newMonth
            newMonth
        }
    }

    // Month operations
    fun getCurrentMonth(): Month? {
        return _currentMonthFlow.value
    }

    // Navigate to previous month
    suspend fun navigateToPreviousMonth(): Month {
        val current = _currentMonthFlow.value
        return if (current != null) {
            val prevDate = LocalDate.of(current.year, current.month, 1).minusMonths(1)
            getOrCreateMonth(prevDate.year, prevDate.monthValue)
        } else {
            getOrCreateMonth(LocalDate.now().year, LocalDate.now().monthValue)
        }
    }

    // Navigate to next month
    suspend fun navigateToNextMonth(): Month {
        val current = _currentMonthFlow.value
        return if (current != null) {
            val nextDate = LocalDate.of(current.year, current.month, 1).plusMonths(1)
            getOrCreateMonth(nextDate.year, nextDate.monthValue)
        } else {
            getOrCreateMonth(LocalDate.now().year, LocalDate.now().monthValue)
        }
    }

    suspend fun getAllMonths(): List<Month> {
        return monthDao.getAllActiveMonths().first()
    }

    suspend fun deleteMonth(monthId: String): Result<Unit> {
        return try {
            monthDao.deleteMonth(monthId)

            // If we deleted the current month, clear it
            if (_currentMonthFlow.value?.id == monthId) {
                _currentMonthFlow.value = null
                ensureCurrentMonthExists() // Create current month again
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMonth(month: Month): Result<Month> {
        return try {
            monthDao.updateMonth(month)

            // Update current month if this is the one being updated
            if (_currentMonthFlow.value?.id == month.id) {
                _currentMonthFlow.value = month
            }

            Result.success(month)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}