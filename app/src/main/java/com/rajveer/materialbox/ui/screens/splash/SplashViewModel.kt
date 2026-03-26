package com.rajveer.materialbox.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Preloads essential data from Room while the splash animation plays,
 * so the Home Screen appears instantly with data already ready.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val materialRepository: MaterialRepository
) : ViewModel() {

    init {
        preloadData()
    }

    private fun preloadData() {
        viewModelScope.launch {
            // Warm up Room by triggering initial queries in parallel
            launch { runCatching { subjectRepository.getAllSubjects().first() } }
            launch { runCatching { topicRepository.getTotalTopicCount().first() } }
            launch { runCatching { materialRepository.getTotalMaterialCount().first() } }
            launch { runCatching { materialRepository.getRecentlyAddedMaterials().first() } }
            launch { runCatching { materialRepository.getMostViewedMaterials().first() } }
        }
    }
}
