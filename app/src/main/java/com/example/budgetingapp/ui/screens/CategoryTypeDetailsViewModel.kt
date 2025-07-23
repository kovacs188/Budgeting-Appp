package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryTypeDetailsUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CategoryTypeDetailsViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryTypeDetailsUiState())
    val uiState: StateFlow<CategoryTypeDetailsUiState> = _uiState.asStateFlow()

    fun loadCategories(categoryType: CategoryType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val categories = repository.getCategoriesForCurrentMonthByType(categoryType)
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load categories: ${e.message}"
                )
            }
        }
    }

    fun onCategoryMove(from: Int, to: Int) {
        viewModelScope.launch {
            val currentList = _uiState.value.categories.toMutableList()
            currentList.add(to, currentList.removeAt(from))
            // Update UI state immediately for responsiveness
            _uiState.value = _uiState.value.copy(categories = currentList)
            // Persist the new order in the database
            repository.updateCategoryOrder(currentList)
        }
    }

    fun refreshCategories(categoryType: CategoryType) {
        loadCategories(categoryType)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
