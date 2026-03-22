package com.rajveer.materialbox.ui.screens.materialdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.repository.MaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaterialDetailViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val materialId: Long = checkNotNull(savedStateHandle["materialId"])

    private val _material = MutableStateFlow<Material?>(null)
    val material: StateFlow<Material?> = _material

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        viewModelScope.launch {
            val initialMaterial = materialRepository.getMaterialById(materialId).first()
            if (initialMaterial?.type == MaterialType.NOTE) {
                materialRepository.incrementViewCount(materialId)
            }

            materialRepository.getMaterialById(materialId).collect { mat ->
                _material.value = mat
                _isLoading.value = false
            }
        }
    }

    fun deleteMaterial() {
        viewModelScope.launch {
            _material.value?.let { material ->
                if (!material.pathOrUrl.startsWith("content://") && !material.pathOrUrl.startsWith("http")) {
                    val file = java.io.File(context.filesDir, material.pathOrUrl)
                    if (file.exists()) file.delete()
                }
                materialRepository.deleteMaterial(material)
            }
        }
    }

    fun updateMaterial(
        title: String,
        content: String,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            _material.value?.let { material ->
                val updatedMaterial = material.copy(
                    title = title.trim(),
                    pathOrUrl = content.trim(),
                    originalFileUri = material.originalFileUri
                )
                materialRepository.updateMaterial(updatedMaterial)
                onSuccess()
            }
        }
    }
} 