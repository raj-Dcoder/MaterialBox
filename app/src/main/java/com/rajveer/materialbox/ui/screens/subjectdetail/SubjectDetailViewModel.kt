package com.rajveer.materialbox.ui.screens.subjectdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle
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

    init {
        viewModelScope.launch {
            _subject.value = subjectRepository.getSubjectById(subjectId)
        }
    }

    fun deleteSubject() {
        viewModelScope.launch {
            _subject.value?.let { subject ->
                subjectRepository.deleteSubject(subject)
                Log.d("SubjectViewModel", "Subject deleted: ${subject.id}")
            }
        }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            topicRepository.deleteTopic(topic)
            Log.d("SubjectViewModel", "Topic deleted: ${topic.id}")
        }
    }
} 