package com.rajveer.materialbox.ui.screens.roadmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.RoadmapItem
import com.rajveer.materialbox.data.repository.RoadmapRepository
import com.rajveer.materialbox.data.repository.StreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoadmapListItem(
    val item: RoadmapItem,
    val isChild: Boolean,
    val totalChildren: Int = 0,
    val completedChildren: Int = 0
)

@HiltViewModel
class RoadmapViewModel @Inject constructor(
    private val roadmapRepository: RoadmapRepository,
    private val streakRepository: StreakRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _localItems = MutableStateFlow<List<RoadmapListItem>>(emptyList())
    val items: StateFlow<List<RoadmapListItem>> = _localItems.asStateFlow()

    private val _collapsedParentIds = MutableStateFlow<Set<Long>>(emptySet())
    val collapsedParentIds: StateFlow<Set<Long>> = _collapsedParentIds.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                roadmapRepository.getItemsForSubject(subjectId),
                _collapsedParentIds
            ) { allItems, collapsedIds ->
                val grouped = allItems.groupBy { it.parentId }
                val topLevel = grouped[null]?.sortedBy { it.position } ?: emptyList()
                
                val result = mutableListOf<RoadmapListItem>()
                for (parent in topLevel) {
                    val children = grouped[parent.id]?.sortedBy { it.position } ?: emptyList()
                    val total = children.size
                    val completed = children.count { it.isCompleted }
                    
                    result.add(RoadmapListItem(parent, isChild = false, totalChildren = total, completedChildren = completed))
                    
                    if (!collapsedIds.contains(parent.id)) {
                        for (child in children) {
                            result.add(RoadmapListItem(child, isChild = true))
                        }
                    }
                }
                result
            }.collect {
                _localItems.value = it
            }
        }
    }

    val progress = roadmapRepository.getProgress(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addItem(text: String, parentId: Long? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        
        viewModelScope.launch {
            val allItems = roadmapRepository.getItemsForSubject(subjectId).first()
            val position = allItems.count { it.parentId == parentId }
            
            val item = RoadmapItem(
                subjectId = subjectId,
                parentId = parentId,
                text = trimmed,
                position = position
            )
            roadmapRepository.insertItem(item)
            
            // Auto-expand if adding a sub-goal
            if (parentId != null) {
                _collapsedParentIds.update { it - parentId }
            }
        }
    }

    fun toggleParentExpansion(parentId: Long) {
        _collapsedParentIds.update { if (it.contains(parentId)) it - parentId else it + parentId }
    }

    fun toggleItemCompletion(item: RoadmapItem) {
        viewModelScope.launch {
            val newCompleted = !item.isCompleted
            roadmapRepository.updateItem(item.copy(isCompleted = newCompleted))
            
            if (item.parentId != null) {
                // Check siblings
                val allItems = roadmapRepository.getItemsForSubject(subjectId).first()
                val children = allItems.filter { it.parentId == item.parentId }
                val parent = allItems.find { it.id == item.parentId }
                
                val updatedChildren = children.map { if (it.id == item.id) it.copy(isCompleted = newCompleted) else it }
                val allChildrenCompleted = updatedChildren.isNotEmpty() && updatedChildren.all { it.isCompleted }
                
                if (parent != null && parent.isCompleted != allChildrenCompleted) {
                    roadmapRepository.updateItem(parent.copy(isCompleted = allChildrenCompleted))
                }
            }
            
            // Record streak activity if checking off an item
            if (newCompleted) {
                streakRepository.recordActivity(subjectId)
            }
        }
    }

    fun deleteItem(item: RoadmapItem) {
        viewModelScope.launch {
            roadmapRepository.deleteItem(item)
        }
    }

    fun onDragMove(fromIndex: Int, toIndex: Int) {
        val list = _localItems.value.toMutableList()
        val fromNode = list.getOrNull(fromIndex) ?: return
        val toNode = list.getOrNull(toIndex) ?: return
        
        // Constraint: Can only swap if both are top-level or both are children of the SAME parent.
        if (fromNode.item.parentId != toNode.item.parentId) return
        
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        _localItems.value = list
    }

    fun onDragEnd() {
        viewModelScope.launch {
            // Save the current flat list ordering to DB.
            val updates = mutableListOf<RoadmapItem>()
            val groupedList = _localItems.value.map { it.item }.groupBy { it.parentId }
            
            for ((_, group) in groupedList) {
                group.forEachIndexed { index, item ->
                    if (item.position != index) {
                        updates.add(item.copy(position = index))
                    }
                }
            }
            
            if (updates.isNotEmpty()) {
                roadmapRepository.updateItems(updates)
            }
        }
    }
}
