package com.example.budgetingapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val activeProjects: List<Category> = emptyList(),
    val completedProjects: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.activeProjectsFlow,
                repository.completedProjectsFlow
            ) { activeProjects, completedProjects ->
                _uiState.value = _uiState.value.copy(
                    activeProjects = activeProjects,
                    completedProjects = completedProjects,
                    isLoading = false
                )
            }.collect {}
        }
    }

    fun markProjectComplete(projectId: String) {
        viewModelScope.launch {
            val result = repository.markProjectComplete(projectId)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to mark project complete: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}