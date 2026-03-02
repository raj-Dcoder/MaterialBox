package com.rajveer.materialbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val materialRepository: MaterialRepository,
    private val topicRepository: TopicRepository
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

    // Stats for the greeting header
    val topicCount: StateFlow<Int> = topicRepository.getTotalTopicCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val materialCount: StateFlow<Int> = materialRepository.getTotalMaterialCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good morning! ☀️"
            hour < 17 -> "Good afternoon! 👋"
            else -> "Good evening! 👋"
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun incrementViewCount(materialId: Long) {
        viewModelScope.launch {
            materialRepository.incrementViewCount(materialId)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            subjectRepository.deleteSubject(subject)
        }
    }

    enum class ViewMode {
        RECENTLY_ADDED,
        MOST_VIEWED
    }
}