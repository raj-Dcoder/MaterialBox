package com.rajveer.materialbox.ui.screens.addtopic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTopicViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun setName(newName: String) {
        _name.value = newName
    }

    fun saveTopic(onSuccess: () -> Unit) {
        if (_name.value.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val topic = Topic(
                    name = _name.value.trim(),
                    subjectId = subjectId
                )
                topicRepository.insertTopic(topic)
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 