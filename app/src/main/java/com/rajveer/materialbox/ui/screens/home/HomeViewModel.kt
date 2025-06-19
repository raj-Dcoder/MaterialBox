package com.rajveer.materialbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.RECENTLY_ADDED)
    val viewMode: StateFlow<ViewMode> = _viewMode

    val subjects = subjectRepository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val materials: StateFlow<List<Material>> = combine(
        _viewMode,
        materialRepository.getRecentlyAddedMaterials(),
        materialRepository.getMostViewedMaterials()
    ) { mode, recent: List<Material>, mostViewed: List<Material> ->
        when (mode) {
            ViewMode.RECENTLY_ADDED -> recent
            ViewMode.MOST_VIEWED -> mostViewed
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun incrementViewCount(materialId: Long) {
        viewModelScope.launch {
            materialRepository.incrementViewCount(materialId)
        }
    }

    enum class ViewMode {
        RECENTLY_ADDED,
        MOST_VIEWED
    }
}