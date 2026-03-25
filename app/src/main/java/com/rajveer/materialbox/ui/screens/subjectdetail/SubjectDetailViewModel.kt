package com.rajveer.materialbox.ui.screens.subjectdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.YoutubeFeed
import com.rajveer.materialbox.data.repository.MaterialRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val materialRepository: MaterialRepository,
    private val youtubeFeedRepository: YoutubeFeedRepository,
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

    val youtubeFeeds = youtubeFeedRepository.getYoutubeFeedsForSubject(subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun getMaterialCountForTopic(topicId: Long): Flow<Int> =
        materialRepository.getMaterialCountForTopic(topicId)
} 