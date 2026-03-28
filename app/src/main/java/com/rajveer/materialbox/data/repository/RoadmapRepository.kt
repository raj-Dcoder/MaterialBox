package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.RoadmapDao
import com.rajveer.materialbox.data.dao.RoadmapProgress
import com.rajveer.materialbox.data.entity.RoadmapItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoadmapRepository @Inject constructor(
    private val roadmapDao: RoadmapDao
) {
    fun getItemsForSubject(subjectId: Long): Flow<List<RoadmapItem>> =
        roadmapDao.getItemsForSubject(subjectId)

    fun getProgress(subjectId: Long): Flow<RoadmapProgress> =
        roadmapDao.getProgress(subjectId)

    suspend fun insertItem(item: RoadmapItem): Long =
        roadmapDao.insertItem(item)

    suspend fun updateItem(item: RoadmapItem) =
        roadmapDao.updateItem(item)

    suspend fun deleteItem(item: RoadmapItem) =
        roadmapDao.deleteItem(item)
        
    suspend fun updateItems(items: List<RoadmapItem>) =
        roadmapDao.updateItems(items)
}
