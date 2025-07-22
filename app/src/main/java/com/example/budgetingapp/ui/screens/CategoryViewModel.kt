package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryFormState
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val formState: CategoryFormState = CategoryFormState(),
    val originalCategory: Category? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val categoryCreated: Boolean = false,
    val categoryUpdated: Boolean = false,
    val isEditMode: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // Expose repository's categories flow
    val categoriesFlow = repository.categoriesFlow

    // Initialize for editing an existing category
    fun initializeForEdit(category: Category) {
        val formState = CategoryFormState(
            name = category.name,
            type = category.type,
            targetAmount = category.targetAmount.toString(),
            description = category.description
        ).validate()

        _uiState.value = _uiState.value.copy(
            formState = formState,
            originalCategory = category,
            isEditMode = true
        )
    }

    // Initialize for creating a new category
    fun initializeForCreate() {
        _uiState.value = _uiState.value.copy(
            formState = CategoryFormState(),
            originalCategory = null,
            isEditMode = false
        )
    }

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

    fun saveCategory() {
        if (_uiState.value.isEditMode) {
            updateCategory()
        } else {
            createCategory()
        }
    }

    private fun createCategory() {
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
                    monthId = repository.getCurrentMonth()?.id ?: "default_month",
                    // Auto-set actualAmount for fixed expenses
                    actualAmount = if (formState.type == CategoryType.FIXED_EXPENSE) {
                        formState.targetAmount.toDouble()
                    } else {
                        0.0
                    }
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

    private fun updateCategory() {
        val formState = _uiState.value.formState.validate()
        _uiState.value = _uiState.value.copy(formState = formState)

        if (!formState.isValid) {
            return
        }

        val originalCategory = _uiState.value.originalCategory
        if (originalCategory == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Original category not found"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                val updatedCategory = originalCategory.copy(
                    name = formState.name.trim(),
                    type = formState.type,
                    targetAmount = formState.targetAmount.toDouble(),
                    description = formState.description.trim()
                )

                val result = repository.updateCategory(updatedCategory)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        categoryUpdated = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to update category: ${result.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update category: ${e.message}"
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
            categoryCreated = false,
            categoryUpdated = false,
            isEditMode = false,
            originalCategory = null
        )
    }

    fun markCategoryCreatedHandled() {
        _uiState.value = _uiState.value.copy(categoryCreated = false)
    }

    fun markCategoryUpdatedHandled() {
        _uiState.value = _uiState.value.copy(categoryUpdated = false)
    }

    // Delegate to repository for data access
    suspend fun getCategoriesByType(type: CategoryType) = repository.getCategoriesForCurrentMonthByType(type)
    suspend fun getAllCategories() = repository.getCategoriesForCurrentMonth()
    suspend fun getTotalIncome() = repository.getTotalIncome()
    suspend fun getTotalExpenses() = repository.getTotalExpenses()
    suspend fun getTotalByType(type: CategoryType) = repository.getTotalByType(type)
    suspend fun getRemainingBudget() = repository.getRemainingBudget()
}