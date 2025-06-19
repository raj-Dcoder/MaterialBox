package com.rajveer.materialbox.ui.screens.addmaterial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMaterialViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val topicId: Long = checkNotNull(savedStateHandle["topicId"])

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _type = MutableStateFlow(MaterialType.NOTE)
    val type: StateFlow<MaterialType> = _type

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun setContent(newContent: String) {
        _content.value = newContent
    }

    fun setType(newType: MaterialType) {
        _type.value = newType
    }

    fun saveMaterial(onSuccess: () -> Unit) {
        if (_title.value.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val material = Material(
                    title = _title.value.trim(),
                    pathOrUrl = _content.value.trim(), // For LINK type, content is the URL
                    type = _type.value,
                    topicId = topicId
                )
                materialRepository.insertMaterial(material)
                onSuccess()
            } finally {
                _isLoading.value = false
            }
        }
    }
}