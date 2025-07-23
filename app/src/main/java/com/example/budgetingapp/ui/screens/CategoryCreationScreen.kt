package com.example.budgetingapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.CategoryType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCreationScreen(
    categoryId: String?,
    defaultCategoryType: String?,
    isProject: Boolean = false,
    onNavigateBack: () -> Unit,
    onCategorySaved: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState = uiState.formState

    LaunchedEffect(categoryId, defaultCategoryType, isProject) {
        viewModel.initialize(categoryId, defaultCategoryType, isProject)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onCategorySaved()
            viewModel.operationHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit ${if (formState.isProject) "Project" else "Category"}"
                        else "Create ${if (formState.isProject) "Project" else "Category"}"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveCategory() }, enabled = formState.isValid && !uiState.isLoading) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project Toggle
                ProjectToggleCard(
                    isProject = formState.isProject,
                    onToggle = { viewModel.updateIsProject(it) }
                )

                // Name
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text(if (formState.isProject) "Project Name" else "Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formState.nameError != null,
                    supportingText = { formState.nameError?.let { Text(it) } },
                    singleLine = true
                )

                // Project Total Budget (only for projects)
                if (formState.isProject) {
                    OutlinedTextField(
                        value = formState.projectTotalBudget,
                        onValueChange = { viewModel.updateProjectTotalBudget(it) },
                        label = { Text("Total Project Budget") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$") },
                        isError = formState.projectBudgetError != null,
                        supportingText = {
                            formState.projectBudgetError?.let { Text(it) }
                                ?: Text("Total amount you expect this project to cost")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                } else {
                    // Monthly Budget (only for regular categories)
                    OutlinedTextField(
                        value = formState.targetAmount,
                        onValueChange = { viewModel.updateTargetAmount(it) },
                        label = {
                            Text(
                                when (formState.type) {
                                    CategoryType.INCOME -> "Expected Income"
                                    else -> "Monthly Budget"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Text("$") },
                        isError = formState.amountError != null,
                        supportingText = { formState.amountError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                // Category Type (not for projects)
                if (!formState.isProject) {
                    CategoryTypeSelector(
                        selectedType = formState.type,
                        onTypeSelected = { viewModel.updateType(it) }
                    )

                    RolloverSelector(
                        isEnabled = formState.isRolloverEnabled,
                        onToggle = { viewModel.updateIsRolloverEnabled(it) }
                    )
                }

                // Description
                OutlinedTextField(
                    value = formState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.weight(1f))

                // Save Button
                Button(
                    onClick = { viewModel.saveCategory() },
                    enabled = formState.isValid && !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            if (uiState.isEditMode) {
                                if (formState.isProject) "Save Project Changes" else "Save Category Changes"
                            } else {
                                if (formState.isProject) "Create Project" else "Create Category"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectToggleCard(
    isProject: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProject)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isProject)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isProject) "✅ Creating a Project" else "📋 Creating a Category",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isProject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isProject) {
                            "One-time expense with total budget tracking"
                        } else {
                            "Toggle the switch to create a project instead"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isProject) "Project" else "Category",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isProject) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isProject,
                        onCheckedChange = onToggle,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTypeSelector(
    selectedType: CategoryType,
    onTypeSelected: (CategoryType) -> Unit
) {
    Column {
        Text("Category Type", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // Exclude PROJECT_EXPENSE from selection since it's handled by the project toggle
        CategoryType.values().filter { it != CategoryType.PROJECT_EXPENSE }.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeSelected(type) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(selected = selectedType == type, onClick = { onTypeSelected(type) })
                Spacer(Modifier.width(8.dp))
                Text(type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@Composable
private fun RolloverSelector(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Rollover", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Carry over unused funds to the next month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isEnabled, onCheckedChange = onToggle)
        }
    }
}