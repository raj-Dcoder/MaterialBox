package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.TopicChecklistDao
import com.rajveer.materialbox.data.dao.TopicChecklistProgress
import com.rajveer.materialbox.data.entity.TopicChecklistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicChecklistRepository @Inject constructor(
    private val topicChecklistDao: TopicChecklistDao
) {
    fun getItemsForTopic(topicId: Long): Flow<List<TopicChecklistItem>> =
        topicChecklistDao.getItemsForTopic(topicId)

    fun getProgress(topicId: Long): Flow<TopicChecklistProgress> =
        topicChecklistDao.getProgress(topicId)

    fun getItemCountForTopic(topicId: Long): Flow<Int> =
        topicChecklistDao.getItemCountForTopic(topicId)

    suspend fun insertItem(item: TopicChecklistItem): Long =
        topicChecklistDao.insertItem(item)

    suspend fun updateItem(item: TopicChecklistItem) =
        topicChecklistDao.updateItem(item)

    suspend fun deleteItem(item: TopicChecklistItem) =
        topicChecklistDao.deleteItem(item)

    suspend fun updateItems(items: List<TopicChecklistItem>) =
        topicChecklistDao.updateItems(items)
}
