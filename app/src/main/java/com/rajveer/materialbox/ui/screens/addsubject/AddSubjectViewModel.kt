package com.rajveer.materialbox.ui.screens.addsubject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun setName(newName: String) {
        _name.value = newName
    }

    fun saveSubject(onSuccess: () -> Unit) {
        if (_name.value.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val subject = Subject(name = _name.value.trim())
                subjectRepository.insertSubject(subject)
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 