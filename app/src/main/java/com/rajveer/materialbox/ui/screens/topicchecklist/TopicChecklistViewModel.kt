package com.rajveer.materialbox.ui.screens.topicchecklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.TopicChecklistItem
import com.rajveer.materialbox.data.repository.StreakRepository
import com.rajveer.materialbox.data.repository.TopicChecklistRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicChecklistListItem(
    val item: TopicChecklistItem,
    val isChild: Boolean,
    val totalChildren: Int = 0,
    val completedChildren: Int = 0
)

@HiltViewModel
class TopicChecklistViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val topicChecklistRepository: TopicChecklistRepository,
    private val streakRepository: StreakRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    val topic: StateFlow<Topic?> = topicRepository.getTopicById(topicId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _localItems = MutableStateFlow<List<TopicChecklistListItem>>(emptyList())
    val items: StateFlow<List<TopicChecklistListItem>> = _localItems.asStateFlow()

    init {
        viewModelScope.launch {
            topicChecklistRepository.getItemsForTopic(topicId)
                .map(::buildListItems)
                .collect {
                    _localItems.value = it
                }
        }
    }

    val progress = topicChecklistRepository.getProgress(topicId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addItem(text: String, parentId: Long? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val allItems = topicChecklistRepository.getItemsForTopic(topicId).first()
            val position = allItems.count { it.parentId == parentId }
            val item = TopicChecklistItem(
                topicId = topicId,
                parentId = parentId,
                text = trimmed,
                position = position
            )
            topicChecklistRepository.insertItem(item)

            if (parentId != null) {
                val parent = allItems.find { it.id == parentId }
                if (parent != null && parent.isCollapsed) {
                    topicChecklistRepository.updateItem(parent.copy(isCollapsed = false))
                }
            }
        }
    }

    fun toggleParentExpansion(parentId: Long) {
        viewModelScope.launch {
            val parent = topicChecklistRepository.getItemsForTopic(topicId).first().find { it.id == parentId } ?: return@launch
            topicChecklistRepository.updateItem(parent.copy(isCollapsed = !parent.isCollapsed))
        }
    }

    fun toggleItemCompletion(item: TopicChecklistItem) {
        viewModelScope.launch {
            val newCompleted = !item.isCompleted
            topicChecklistRepository.updateItem(item.copy(isCompleted = newCompleted))

            if (item.parentId != null) {
                val allItems = topicChecklistRepository.getItemsForTopic(topicId).first()
                val children = allItems.filter { it.parentId == item.parentId }
                val parent = allItems.find { it.id == item.parentId }
                val updatedChildren = children.map { current ->
                    if (current.id == item.id) current.copy(isCompleted = newCompleted) else current
                }
                val allChildrenCompleted = updatedChildren.isNotEmpty() && updatedChildren.all { it.isCompleted }

                if (parent != null && parent.isCompleted != allChildrenCompleted) {
                    topicChecklistRepository.updateItem(parent.copy(isCompleted = allChildrenCompleted))
                }
            }

            // Do not record streak activity when checking off checklist items.
        }
    }

    fun deleteItem(item: TopicChecklistItem) {
        viewModelScope.launch {
            topicChecklistRepository.deleteItem(item)
        }
    }

    fun editItem(item: TopicChecklistItem, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            topicChecklistRepository.updateItem(item.copy(text = newText))
        }
    }

    fun onDragMove(fromIndex: Int, toIndex: Int) {
        val list = _localItems.value.toMutableList()
        val fromNode = list.getOrNull(fromIndex) ?: return
        val toNode = list.getOrNull(toIndex) ?: return
        if (fromNode.item.parentId != toNode.item.parentId) return

        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        _localItems.value = list
    }

    fun onDragEnd() {
        viewModelScope.launch {
            val updates = mutableListOf<TopicChecklistItem>()
            val groupedList = _localItems.value.map { it.item }.groupBy { it.parentId }

            for ((_, group) in groupedList) {
                group.forEachIndexed { index, item ->
                    if (item.position != index) {
                        updates.add(item.copy(position = index))
                    }
                }
            }

            if (updates.isNotEmpty()) {
                topicChecklistRepository.updateItems(updates)
            }
        }
    }

    private fun buildListItems(allItems: List<TopicChecklistItem>): List<TopicChecklistListItem> {
        val grouped = allItems.groupBy { it.parentId }
        val topLevel = grouped[null].orEmpty().sortedForChecklist()
        val result = mutableListOf<TopicChecklistListItem>()

        for (parent in topLevel) {
            val children = grouped[parent.id].orEmpty().sortedForChecklist()
            val total = children.size
            val completed = children.count { it.isCompleted }

            result.add(
                TopicChecklistListItem(
                    item = parent,
                    isChild = false,
                    totalChildren = total,
                    completedChildren = completed
                )
            )

            if (!parent.isCollapsed) {
                children.forEach { child ->
                    result.add(TopicChecklistListItem(item = child, isChild = true))
                }
            }
        }

        return result
    }

    private fun List<TopicChecklistItem>.sortedForChecklist(): List<TopicChecklistItem> =
        sortedWith(
            compareBy<TopicChecklistItem> { it.isCompleted }
                .thenBy { it.position }
                .thenBy { it.createdAt.time }
        )
}
