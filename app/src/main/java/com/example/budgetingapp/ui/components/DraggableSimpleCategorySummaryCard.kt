package com.example.budgetingapp.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.budgetingapp.data.model.CategoryType
import com.example.budgetingapp.ui.screens.CategorySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Drag-and-drop state for category summary cards
class SummaryCardDragDropState(
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
                overscrollJob.value = scope.launch {
                    listState.animateScrollToItem(
                        listState.firstVisibleItemIndex + 1
                    )
                }
            }
        } else if (startOffset < 100) { // Scroll up
            if (overscrollJob.value?.isActive != true) {
                overscrollJob.value = scope.launch {
                    listState.animateScrollToItem(
                        (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    )
                }
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

@Composable
fun DraggableSimpleCategorySummaryCard(
    summary: CategorySummary,
    dragDropState: SummaryCardDragDropState,
    index: Int,
    onClick: (CategoryType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDragging = index == dragDropState.draggingItemIndex
    val draggingItemOffset = if (isDragging) dragDropState.draggingItemOffset else 0f
    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation_anim")

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
        SimpleCategorySummaryCard(
            summary = summary,
            onClick = onClick
        )
    }
}