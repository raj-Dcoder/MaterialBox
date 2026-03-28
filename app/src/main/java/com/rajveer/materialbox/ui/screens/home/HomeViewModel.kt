package com.rajveer.materialbox.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.RoadmapRepository
import com.rajveer.materialbox.data.repository.StreakRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import com.rajveer.materialbox.data.entity.SubjectStreak
import com.rajveer.materialbox.data.dao.RoadmapProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
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
    private val topicRepository: TopicRepository,
    private val streakRepository: StreakRepository,
    private val roadmapRepository: RoadmapRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.RECENTLY_ADDED)
    val viewMode: StateFlow<ViewMode> = _viewMode

    init {
        viewModelScope.launch {
            cleanOrphanedFiles()
        }
    }

    private suspend fun cleanOrphanedFiles() {
        try {
            val validFiles = materialRepository.getAllLocalFilePaths().toSet()
            val filesDir = context.filesDir
            val files = filesDir.listFiles() ?: return
            
            var deletedCount = 0
            files.forEach { file ->
                if (file.isFile && file.name !in validFiles) {
                    if (file.name != "profile") {
                        file.delete()
                        deletedCount++
                        android.util.Log.d("HomeViewModel", "Deleted orphaned ghost file: ${file.name}")
                    }
                }
            }
            if (deletedCount > 0) {
                android.util.Log.d("HomeViewModel", "Cleaned up $deletedCount orphaned files.")
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Failed to clean orphaned files", e)
        }
    }

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
        started = SharingStarted.WhileSubscribed(0),  // Never serve stale cached list
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

    fun updateSubject(subject: Subject) {
        viewModelScope.launch {
            subjectRepository.updateSubject(subject)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            val filePaths = materialRepository.getLocalFilePathsForSubject(subject.id)
            filePaths.forEach { path ->
                val file = java.io.File(context.filesDir, path)
                if (file.exists()) file.delete()
            }
            subjectRepository.deleteSubject(subject)
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            val success = com.rajveer.materialbox.util.BackupManager.exportDatabaseToMbox(context, uri)
            if (success) {
                android.widget.Toast.makeText(context, "Export successful", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            val success = com.rajveer.materialbox.util.BackupManager.importDatabaseFromMbox(context, uri)
            if (success) {
                android.widget.Toast.makeText(context, "Import successful. Please completely close and restart the app.", android.widget.Toast.LENGTH_LONG).show()
            } else {
                android.widget.Toast.makeText(context, "Import failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getTopicCountForSubject(subjectId: Long): Flow<Int> =
        topicRepository.getTopicCountForSubject(subjectId)

    fun getStreakForSubject(subjectId: Long): Flow<SubjectStreak?> =
        streakRepository.getStreak(subjectId)

    fun getRoadmapProgressForSubject(subjectId: Long): Flow<RoadmapProgress> =
        roadmapRepository.getProgress(subjectId)

    enum class ViewMode {
        RECENTLY_ADDED,
        MOST_VIEWED
    }
}