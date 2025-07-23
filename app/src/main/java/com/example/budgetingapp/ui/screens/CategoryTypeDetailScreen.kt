package com.example.budgetingapp.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.budgetingapp.data.model.Category
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.components.CategoryCard
import com.example.budgetingapp.ui.components.CategoryPlaceholder
import com.example.budgetingapp.ui.components.SectionHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// This class holds the state for the drag-and-drop operation.
class DragDropState(
    val listState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val scope: CoroutineScope
) {
    var isDragging by mutableStateOf(false)
        private set
    var draggingItemIndex by mutableStateOf<Int?>(null)
    private var draggingItemInitialOffset by mutableStateOf(0)
    var draggingItemOffset by mutableStateOf(0f)

    private val overscrollJob by lazy { mutableStateOf<Job?>(null) }

    private val currentDraggingItemInfo: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    fun onDragStart(offset: Offset, index: Int) {
        isDragging = true
        draggingItemIndex = index
        draggingItemInitialOffset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?.offset ?: 0
    }

    fun onDrag(offset: Offset) {
        draggingItemOffset += offset.y

        val startOffset = draggingItemInitialOffset + draggingItemOffset
        val endOffset = startOffset + (currentDraggingItemInfo?.size ?: 0)

        val hoveredItem = listState.layoutInfo.visibleItemsInfo
            .filter { it.index != draggingItemIndex }
            .firstOrNull {
                val itemStartOffset = it.offset
                val itemEndOffset = itemStartOffset + it.size
                startOffset < itemEndOffset && endOffset > itemStartOffset
            }

        if (hoveredItem != null) {
            val from = draggingItemIndex!!
            val to = hoveredItem.index
            if (from != to) {
                onMove(from, to)
                draggingItemIndex = to
                draggingItemInitialOffset = hoveredItem.offset
            }
        }

        // Auto-scroll logic
        val listBounds = listState.layoutInfo.viewportEndOffset
        if (endOffset > listBounds - 100) { // Scroll down
            if (overscrollJob.value?.isActive != true) {
                overscrollJob.value = scope.launch { listState.scrollBy(20f) }
            }
        } else if (startOffset < 100) { // Scroll up
            if (overscrollJob.value?.isActive != true) {
                overscrollJob.value = scope.launch { listState.scrollBy(-20f) }
            }
        } else {
            overscrollJob.value?.cancel()
        }
    }

    fun onDragInterrupted() {
        isDragging = false
        draggingItemIndex = null
        draggingItemOffset = 0f
        overscrollJob.value?.cancel()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryTypeDetailsScreen(
    categoryType: CategoryType,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionHistory: (String) -> Unit,
    onNavigateToCategoryCreator: () -> Unit,
    viewModel: CategoryTypeDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategoryForQuickAdd by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(categoryType) {
        viewModel.loadCategories(categoryType)
    }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dragDropState = remember(listState, scope) {
        DragDropState(listState = listState, onMove = { from, to -> viewModel.onCategoryMove(from, to) }, scope = scope)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = getCategoryTypeDisplayName(categoryType),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCategoryCreator,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Category"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = !dragDropState.isDragging
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(getCategoryTypeDisplayName(categoryType))
                }

                if (uiState.categories.isEmpty()) {
                    item {
                        CategoryPlaceholder("No ${getCategoryTypeDisplayName(categoryType).lowercase()} categories yet")
                    }
                } else {
                    itemsIndexed(uiState.categories, key = { _, category -> category.id }) { index, category ->
                        DraggableItem(
                            dragDropState = dragDropState,
                            index = index,
                            modifier = Modifier.animateItemPlacement()
                        ) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation_anim")
                            CategoryCard(
                                elevation = elevation,
                                category = category,
                                onViewTransactions = { onNavigateToTransactionHistory(category.id) },
                                onQuickAddTransaction = { selectedCategoryForQuickAdd = category }
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    selectedCategoryForQuickAdd?.let { category ->
        TransactionEntryDialog(
            category = category,
            onDismiss = { selectedCategoryForQuickAdd = null },
            onTransactionAdded = {
                selectedCategoryForQuickAdd = null
                viewModel.refreshCategories(categoryType)
            }
        )
    }
}

@Composable
fun DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = index == dragDropState.draggingItemIndex
    val draggingItemOffset = if (isDragging) dragDropState.draggingItemOffset else 0f

    Column(
        modifier = modifier
            .graphicsLayer { translationY = draggingItemOffset }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, offset ->
                        change.consume()
                        dragDropState.onDrag(offset = offset)
                    },
                    onDragStart = { offset -> dragDropState.onDragStart(offset, index) },
                    onDragEnd = { dragDropState.onDragInterrupted() },
                    onDragCancel = { dragDropState.onDragInterrupted() }
                )
            }
    ) {
        content(isDragging)
    }
}

private fun getCategoryTypeDisplayName(type: CategoryType): String {
    return when (type) {
        CategoryType.INCOME -> "Income Categories"
        CategoryType.FIXED_EXPENSE -> "Fixed Expense Categories"
        CategoryType.VARIABLE_EXPENSE -> "Variable Expense Categories"
        CategoryType.DISCRETIONARY_EXPENSE -> "Discretionary Expense Categories"
    }
}
