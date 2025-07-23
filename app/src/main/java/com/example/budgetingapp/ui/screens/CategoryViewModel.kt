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
    val isSuccess: Boolean = false,
    val isEditMode: Boolean = false
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun initialize(categoryId: String?, defaultCategoryType: String?) {
        if (categoryId != null) {
            // Editing an existing category
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val category = repository.getCategoryById(categoryId)
                if (category != null) {
                    initializeForEdit(category)
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Category not found.", isLoading = false)
                }
            }
        } else {
            // Creating a new category
            initializeForCreate(defaultCategoryType)
        }
    }

    private fun initializeForEdit(category: Category) {
        val formState = CategoryFormState(
            name = category.name,
            type = category.type,
            targetAmount = category.targetAmount.toString(),
            description = category.description,
            isRolloverEnabled = category.isRolloverEnabled
        ).validate()

        _uiState.value = _uiState.value.copy(
            formState = formState,
            originalCategory = category,
            isEditMode = true,
            isLoading = false
        )
    }

    private fun initializeForCreate(defaultCategoryType: String?) {
        // ** THE FIX IS HERE **
        // It now defaults to INCOME when no specific type is provided.
        val defaultType = try {
            defaultCategoryType?.let { CategoryType.valueOf(it) } ?: CategoryType.INCOME
        } catch (e: IllegalArgumentException) {
            CategoryType.INCOME
        }
        _uiState.value = CategoryUiState(formState = CategoryFormState(type = defaultType))
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(name = name).validate()
        )
    }

    fun updateType(type: com.example.budgetingapp.data.model.CategoryType) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(type = type).validate()
        )
    }

    fun updateTargetAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(targetAmount = amount).validate()
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(description = description).validate()
        )
    }

    fun updateIsRolloverEnabled(isEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(isRolloverEnabled = isEnabled).validate()
        )
    }

    fun saveCategory() {
        val formState = _uiState.value.formState
        if (!formState.isValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = if (_uiState.value.isEditMode) {
                updateCategory(formState)
            } else {
                createCategory(formState)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "An unknown error occurred"
                )
            }
        }
    }

    private suspend fun createCategory(formState: CategoryFormState): Result<Unit> {
        val category = Category(
            name = formState.name.trim(),
            type = formState.type,
            targetAmount = formState.targetAmount.toDouble(),
            description = formState.description.trim(),
            isRolloverEnabled = formState.isRolloverEnabled,
            monthId = ""
        )
        return repository.createCategory(category)
    }

    private suspend fun updateCategory(formState: CategoryFormState): Result<Unit> {
        val originalCategory = _uiState.value.originalCategory ?: return Result.failure(Exception("Original category not found"))
        val updatedCategory = originalCategory.copy(
            name = formState.name.trim(),
            type = formState.type,
            targetAmount = formState.targetAmount.toDouble(),
            description = formState.description.trim(),
            isRolloverEnabled = formState.isRolloverEnabled
        )
        return repository.updateCategory(updatedCategory)
    }

    fun operationHandled() {
        _uiState.value = _uiState.value.copy(isSuccess = false, errorMessage = null)
    }
}
