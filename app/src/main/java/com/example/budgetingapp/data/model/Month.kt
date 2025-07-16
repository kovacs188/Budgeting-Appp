package com.example.budgetingapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Entity(tableName = "months")
data class Month(
    @PrimaryKey
    val id: String = generateMonthId(),
    val name: String,
    val year: Int,
    val month: Int,
    val createdDate: LocalDate = LocalDate.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val isActive: Boolean = true
) {
    val displayName: String
        get() = "$name $year"

    val remainingBudget: Double
        get() = totalIncome - totalExpenses

    companion object {
        private fun generateMonthId(): String {
            return "month_${System.currentTimeMillis()}"
        }

        fun createCurrentMonth(): Month {
            val now = LocalDate.now()
            val monthName = now.format(DateTimeFormatter.ofPattern("MMMM"))
            return Month(
                name = monthName,
                year = now.year,
                month = now.monthValue
            )
        }
    }
}