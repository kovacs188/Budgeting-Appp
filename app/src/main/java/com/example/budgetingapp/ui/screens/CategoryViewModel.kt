package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryFormState
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.repository.BudgetRepository
import javax.inject.Inject

data class CategoryUiState(
    val formState: CategoryFormState = CategoryFormState(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val categoryCreated: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // Expose repository's categories flow
    val categoriesFlow = repository.categoriesFlow

    fun updateName(name: String) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(name = name).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun updateType(type: CategoryType) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(type = type).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun updateTargetAmount(amount: String) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(targetAmount = amount).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun updateDescription(description: String) {
        val currentForm = _uiState.value.formState
        val updatedForm = currentForm.copy(description = description).validate()
        _uiState.value = _uiState.value.copy(formState = updatedForm)
    }

    fun createCategory() {
        val formState = _uiState.value.formState.validate()
        _uiState.value = _uiState.value.copy(formState = formState)

        if (!formState.isValid) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val category = Category(
                    name = formState.name.trim(),
                    type = formState.type,
                    targetAmount = formState.targetAmount.toDouble(),
                    description = formState.description.trim(),
                    monthId = repository.getCurrentMonth()?.id ?: "default_month"
                )

                val result = repository.createCategory(category)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        categoryCreated = true,
                        formState = CategoryFormState() // Reset form
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to create category: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create category: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            formState = CategoryFormState(),
            categoryCreated = false
        )
    }

    fun markCategoryCreatedHandled() {
        _uiState.value = _uiState.value.copy(categoryCreated = false)
    }

    // Delegate to repository for data access
    fun getCategoriesByType(type: CategoryType) = repository.getCategoriesForCurrentMonthByType(type)
    fun getAllCategories() = repository.getCategoriesForCurrentMonth()
    fun getTotalIncome() = repository.getTotalIncome()
    fun getTotalExpenses() = repository.getTotalExpenses()
    fun getTotalByType(type: CategoryType) = repository.getTotalByType(type)
    fun getRemainingBudget() = repository.getRemainingBudget()
}