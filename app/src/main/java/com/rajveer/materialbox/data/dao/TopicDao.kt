package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getTopicsForSubject(subjectId: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :id")
    fun getTopicById(id: Long): Flow<Topic?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)
} 