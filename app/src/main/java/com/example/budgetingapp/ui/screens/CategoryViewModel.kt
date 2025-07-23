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

    fun initialize(categoryId: String?, defaultCategoryType: String?, isProject: Boolean = false) {
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
            initializeForCreate(defaultCategoryType, isProject)
        }
    }

    private fun initializeForEdit(category: Category) {
        val formState = CategoryFormState(
            name = category.name,
            type = category.type,
            targetAmount = category.targetAmount.toString(),
            description = category.description,
            isRolloverEnabled = category.isRolloverEnabled,
            isProject = category.isProject,
            projectTotalBudget = if (category.isProject) category.projectTotalBudget.toString() else "",
            parentProjectId = category.parentProjectId
        ).validate()

        _uiState.value = _uiState.value.copy(
            formState = formState,
            originalCategory = category,
            isEditMode = true,
            isLoading = false
        )
    }

    private fun initializeForCreate(defaultCategoryType: String?, isProject: Boolean = false) {
        val defaultType = try {
            defaultCategoryType?.let { CategoryType.valueOf(it) } ?: CategoryType.INCOME
        } catch (e: IllegalArgumentException) {
            CategoryType.INCOME
        }
        _uiState.value = CategoryUiState(
            formState = CategoryFormState(
                type = if (isProject) CategoryType.PROJECT_EXPENSE else defaultType,
                isProject = isProject
            )
        )
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(name = name).validate()
        )
    }

    fun updateType(type: CategoryType) {
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

    // New project-specific methods
    fun updateIsProject(isProject: Boolean) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(
                isProject = isProject,
                type = if (isProject) CategoryType.PROJECT_EXPENSE else CategoryType.DISCRETIONARY_EXPENSE
            ).validate()
        )
    }

    fun updateProjectTotalBudget(budget: String) {
        _uiState.value = _uiState.value.copy(
            formState = _uiState.value.formState.copy(projectTotalBudget = budget).validate()
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
        return if (formState.isProject) {
            // Create as project
            val result = repository.createProject(
                name = formState.name.trim(),
                description = formState.description.trim(),
                totalBudget = formState.projectTotalBudget.toDouble(),
                parentProjectId = formState.parentProjectId
            )
            if (result.isSuccess) {
                // If there's a monthly allocation, create a regular category linked to this project
                if (formState.targetAmount.isNotBlank() && formState.targetAmount.toDoubleOrNull() != null && formState.targetAmount.toDouble() > 0) {
                    val project = result.getOrNull()!!
                    val monthlyCategory = Category(
                        name = "${formState.name.trim()} (Monthly)",
                        type = CategoryType.DISCRETIONARY_EXPENSE,
                        targetAmount = formState.targetAmount.toDouble(),
                        description = "Monthly allocation for ${formState.name.trim()} project",
                        parentProjectId = project.id,
                        monthId = ""
                    )
                    repository.createCategory(monthlyCategory)
                }
                Result.success(Unit)
            } else {
                result.map { }
            }
        } else {
            // Create as regular category
            val category = Category(
                name = formState.name.trim(),
                type = formState.type,
                targetAmount = formState.targetAmount.toDouble(),
                description = formState.description.trim(),
                isRolloverEnabled = formState.isRolloverEnabled,
                monthId = ""
            )
            repository.createCategory(category)
        }
    }

    private suspend fun updateCategory(formState: CategoryFormState): Result<Unit> {
        val originalCategory = _uiState.value.originalCategory ?: return Result.failure(Exception("Original category not found"))

        val updatedCategory = if (formState.isProject) {
            originalCategory.copy(
                name = formState.name.trim(),
                type = CategoryType.PROJECT_EXPENSE,
                targetAmount = if (formState.targetAmount.isNotBlank()) formState.targetAmount.toDouble() else 0.0,
                description = formState.description.trim(),
                isProject = true,
                projectTotalBudget = formState.projectTotalBudget.toDouble(),
                parentProjectId = formState.parentProjectId
            )
        } else {
            originalCategory.copy(
                name = formState.name.trim(),
                type = formState.type,
                targetAmount = formState.targetAmount.toDouble(),
                description = formState.description.trim(),
                isRolloverEnabled = formState.isRolloverEnabled,
                isProject = false,
                projectTotalBudget = 0.0,
                projectTotalSpent = 0.0,
                parentProjectId = null
            )
        }

        return repository.updateCategory(updatedCategory)
    }

    fun operationHandled() {
        _uiState.value = _uiState.value.copy(isSuccess = false, errorMessage = null)
    }
}