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

data class CategoryEditUiState(
    val formState: CategoryFormState = CategoryFormState(),
    val originalCategory: Category? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val categoryUpdated: Boolean = false
)

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryEditUiState())
    val uiState: StateFlow<CategoryEditUiState> = _uiState.asStateFlow()

    fun initializeWithCategory(category: Category) {
        val formState = CategoryFormState(
            name = category.name,
            type = category.type,
            targetAmount = category.targetAmount.toString(),
            description = category.description
        ).validate()

        _uiState.value = _uiState.value.copy(
            formState = formState,
            originalCategory = category
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

    fun updateCategory() {
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

    fun markCategoryUpdatedHandled() {
        _uiState.value = _uiState.value.copy(categoryUpdated = false)
    }
}