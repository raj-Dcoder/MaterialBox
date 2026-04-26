package com.rajveer.materialbox.ui.screens.subjectdetail

import android.util.Log
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.entity.YoutubeFeed
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.RoadmapRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import com.rajveer.materialbox.data.repository.StreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DocumentInfo(val uri: Uri, val title: String, val type: MaterialType)

enum class FabMode {
    NORMAL, // Shows Topic, Roadmap, Feed
    MATERIALS // Shows Add Link, Add Note, etc.
}

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val materialRepository: MaterialRepository,
    private val youtubeFeedRepository: YoutubeFeedRepository,
    private val roadmapRepository: RoadmapRepository,
    private val streakRepository: StreakRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject

    val topics = topicRepository.getTopicsForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val materials = materialRepository.getMaterialsForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val youtubeFeeds = youtubeFeedRepository.getYoutubeFeedsForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _fabMode = MutableStateFlow(FabMode.NORMAL)
    val fabMode: StateFlow<FabMode> = _fabMode.asStateFlow()

    fun setFabMode(mode: FabMode) {
        _fabMode.value = mode
    }

    init {
        viewModelScope.launch {
            _subject.value = subjectRepository.getSubjectById(subjectId)
        }
    }

    fun deleteSubject() {
        viewModelScope.launch {
            _subject.value?.let { subject ->
                val filePaths = materialRepository.getLocalFilePathsForSubject(subject.id)
                filePaths.forEach { path ->
                    val file = java.io.File(context.filesDir, path)
                    if (file.exists()) file.delete()
                }
                subjectRepository.deleteSubject(subject)
                Log.d("SubjectViewModel", "Subject deleted: ${subject.id}")
            }
        }
    }

    fun updateTopic(topic: Topic) {
        viewModelScope.launch {
            topicRepository.updateTopic(topic)
        }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            val filePaths = materialRepository.getLocalFilePathsForTopic(topic.id)
            filePaths.forEach { path ->
                val file = java.io.File(context.filesDir, path)
                if (file.exists()) file.delete()
            }
            topicRepository.deleteTopic(topic)
            Log.d("SubjectViewModel", "Topic deleted: ${topic.id}")
        }
    }

    fun deleteYoutubeFeed(feed: YoutubeFeed) {
        viewModelScope.launch {
            youtubeFeedRepository.deleteYoutubeFeed(feed)
            Log.d("SubjectViewModel", "Youtube feed deleted: ${feed.id}")
        }
    }

    fun deleteMaterial(material: Material) {
        viewModelScope.launch {
            if (material.type != MaterialType.NOTE &&
                material.type != MaterialType.LINK &&
                !material.pathOrUrl.startsWith("content://") &&
                !material.pathOrUrl.startsWith("http")
            ) {
                val file = File(context.filesDir, material.pathOrUrl)
                if (file.exists()) file.delete()
            }
            materialRepository.deleteMaterial(material)
        }
    }

    fun updateMaterial(material: Material) {
        viewModelScope.launch {
            materialRepository.updateMaterial(material)
        }
    }

    fun saveDocumentMaterials(documents: List<DocumentInfo>, onResult: (added: Int, skipped: Int) -> Unit) {
        viewModelScope.launch {
            var added = 0
            var skipped = 0
            for (doc in documents) {
                var finalPath = doc.uri.toString()
                if (doc.uri.scheme == "file") {
                    val fileName = "${System.currentTimeMillis()}_${doc.title}.pdf"
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/MaterialBox")
                        }
                        val collection = android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        val destUri = context.contentResolver.insert(collection, contentValues)
                        
                        if (destUri != null) {
                            try {
                                context.contentResolver.openOutputStream(destUri)?.use { out ->
                                    context.contentResolver.openInputStream(doc.uri)?.use { input ->
                                        input.copyTo(out)
                                    }
                                }
                                finalPath = destUri.toString()
                            } catch(e: Exception) {
                                e.printStackTrace()
                                continue
                            }
                        } else {
                            continue
                        }
                    } else {
                        val destFile = File(context.filesDir, fileName)
                        try {
                            FileOutputStream(destFile).use { out ->
                                context.contentResolver.openInputStream(doc.uri)?.use { input ->
                                    input.copyTo(out)
                                }
                            }
                            finalPath = fileName 
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continue 
                        }
                    }
                }

                val existing = materialRepository.findSubjectMaterialByUriOrTitle(subjectId, finalPath, doc.title)
                if (existing != null) {
                    skipped++
                    continue
                }
                
                val material = Material(
                    title = doc.title,
                    pathOrUrl = finalPath,
                    type = doc.type,
                    subjectId = subjectId,
                    originalFileUri = doc.uri.toString()
                )
                materialRepository.insertMaterial(material)
                added++
            }
            onResult(added, skipped)
        }
    }

    fun incrementViewCount(materialId: Long) {
        viewModelScope.launch {
            materialRepository.incrementViewCount(materialId)
            streakRepository.recordActivity(subjectId)
        }
    }

    fun getMaterialCountForTopic(topicId: Long): Flow<Int> =
        materialRepository.getMaterialCountForTopic(topicId)

    fun getRoadmapItemCount(): Flow<Int> =
        roadmapRepository.getItemCountForSubject(subjectId)
} 
