package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.TopicWithMaterials
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("SELECT * FROM topics WHERE id = :id")
    fun getTopicById(id: Long): Flow<Topic?>

    @Transaction
    @Query("SELECT * FROM topics WHERE id = :id")
    fun getTopicWithMaterials(id: Long): Flow<TopicWithMaterials>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getTopicsForSubject(subjectId: Long): Flow<List<Topic>>
}