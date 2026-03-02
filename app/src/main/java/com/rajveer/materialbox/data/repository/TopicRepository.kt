package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.TopicDao
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.TopicWithMaterials
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicRepository @Inject constructor(
    private val topicDao: TopicDao
) {
    fun getTopicsForSubject(subjectId: Long): Flow<List<Topic>> =
        topicDao.getTopicsForSubject(subjectId)

    fun getTopicById(id: Long): Flow<Topic?> =
        topicDao.getTopicById(id)

    fun getTopicWithMaterials(id: Long): Flow<TopicWithMaterials> =
        topicDao.getTopicWithMaterials(id)

    suspend fun insertTopic(topic: Topic): Long =
        topicDao.insertTopic(topic)

    suspend fun updateTopic(topic: Topic) =
        topicDao.updateTopic(topic)

    suspend fun deleteTopic(topic: Topic) =
        topicDao.deleteTopic(topic)

    fun getTotalTopicCount(): Flow<Int> =
        topicDao.getTotalTopicCount()
}