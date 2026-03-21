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
import javax.inject.Inject

data class DocumentInfo(val uri: Uri, val title: String, val type: MaterialType)

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

    fun saveDocumentMaterials(documents: List<DocumentInfo>, onResult: (added: Int, skipped: Int) -> Unit) {
        viewModelScope.launch {
            var added = 0
            var skipped = 0
            for (doc in documents) {
                val existing = materialRepository.findMaterialByUriOrTitle(topicId, doc.uri.toString(), doc.title)
                if (existing != null) {
                    skipped++
                    continue
                }
                
                val material = Material(
                    title = doc.title,
                    pathOrUrl = doc.uri.toString(),
                    type = doc.type,
                    topicId = topicId,
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
        }
    }
}