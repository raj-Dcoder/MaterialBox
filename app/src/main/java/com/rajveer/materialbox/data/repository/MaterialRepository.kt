package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.MaterialDao
import com.rajveer.materialbox.data.entity.Material
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(
    private val materialDao: MaterialDao
) {
    fun getMaterialsForTopic(topicId: Long): Flow<List<Material>> =
        materialDao.getMaterialsForTopic(topicId)

    fun getMaterialById(id: Long): Flow<Material?> =
        materialDao.getMaterialById(id)

    fun getRecentMaterials(limit: Int = 3): Flow<List<Material>> =
        materialDao.getRecentMaterials(limit)

    fun getRecentlyAddedMaterials(limit: Int = 3): Flow<List<Material>> =
        materialDao.getRecentMaterials(limit)

    fun getMostViewedMaterials(limit: Int = 3): Flow<List<Material>> =
        materialDao.getMostViewedMaterials(limit)

    suspend fun insertMaterial(material: Material): Long =
        materialDao.insertMaterial(material)

    suspend fun updateMaterial(material: Material) =
        materialDao.updateMaterial(material)

    suspend fun deleteMaterial(material: Material) =
        materialDao.deleteMaterial(material)

    suspend fun incrementViewCount(materialId: Long) = 
        materialDao.incrementViewCount(materialId)
} 