package com.rajveer.materialbox.ui.screens.manageoriginals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageOriginalsViewModel @Inject constructor(
    private val materialRepository: MaterialRepository
) : ViewModel() {
    val materialsWithOriginalUri: StateFlow<List<Material>> =
        materialRepository.getMaterialsWithOriginalUri()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    fun toggleSelection(materialId: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (contains(materialId)) remove(materialId) else add(materialId)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun setSelection(materialId: Long, selected: Boolean) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (selected) add(materialId) else remove(materialId)
        }
    }

    fun deleteOriginals(onResult: (success: List<Long>, failed: List<Long>) -> Unit) {
        val selected = _selectedIds.value
        val materials = materialsWithOriginalUri.value.filter { it.id in selected }
        val success = mutableListOf<Long>()
        val failed = mutableListOf<Long>()
        viewModelScope.launch {
            materials.forEach { material ->
                val uriString = material.originalFileUri
                if (uriString != null) {
                    try {
                        // Deletion will be handled in the Composable via context
                        success.add(material.id)
                    } catch (e: Exception) {
                        failed.add(material.id)
                    }
                }
            }
            onResult(success, failed)
            clearSelection()
        }
    }
} 