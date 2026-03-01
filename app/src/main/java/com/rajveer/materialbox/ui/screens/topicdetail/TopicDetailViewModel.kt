package com.rajveer.materialbox.ui.screens.topicdetail

import android.content.Context
import android.net.Uri

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

data class TopicDetailUiState(
    val topic: Topic? = null,
    val materials: List<Material> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TopicDetailViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val materialRepository: MaterialRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    val uiState: StateFlow<TopicDetailUiState> = topicRepository.getTopicWithMaterials(topicId)
        .map { topicWithMaterials ->
            TopicDetailUiState(
                topic = topicWithMaterials.topic,
                materials = topicWithMaterials.materials,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TopicDetailUiState(isLoading = true)
        )

    fun deleteTopic() {
        viewModelScope.launch {
            uiState.value.topic?.let { topicRepository.deleteTopic(it) }
        }
    }

    fun deleteMaterial(material: Material) {
        viewModelScope.launch {
            materialRepository.deleteMaterial(material)
        }
    }

    fun saveDocumentMaterial(uri: Uri, title: String, type: MaterialType, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val internalFileName = copyFileToInternalStorage(uri, title)
            if (internalFileName != null) {
                val material = Material(
                    title = title,
                    pathOrUrl = internalFileName,
                    type = type,
                    topicId = topicId,
                    originalFileUri = uri.toString()
                )
                materialRepository.insertMaterial(material)
                onSuccess()
            }
            // Consider adding error handling to notify the user
        }
    }

    private fun copyFileToInternalStorage(uri: Uri, originalFileName: String): String? {
        return try {
            val sanitizedFileName = originalFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val uniqueFileName = "${System.currentTimeMillis()}_${sanitizedFileName}"
            val file = File(context.filesDir, uniqueFileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.name
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun incrementViewCount(materialId: Long) {
        viewModelScope.launch {
            materialRepository.incrementViewCount(materialId)
        }
    }
}