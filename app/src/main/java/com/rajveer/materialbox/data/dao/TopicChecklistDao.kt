package com.rajveer.materialbox.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rajveer.materialbox.data.entity.TopicChecklistItem
import kotlinx.coroutines.flow.Flow

data class TopicChecklistProgress(
    val completed: Int,
    val total: Int
)

@Dao
interface TopicChecklistDao {
    @Query("SELECT * FROM topic_checklist_items WHERE topicId = :topicId ORDER BY position ASC, createdAt ASC")
    fun getItemsForTopic(topicId: Long): Flow<List<TopicChecklistItem>>

    @Query(
        "SELECT " +
            "SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed, " +
            "COUNT(*) as total " +
            "FROM topic_checklist_items WHERE topicId = :topicId " +
            "AND id NOT IN (SELECT IFNULL(parentId, -1) FROM topic_checklist_items WHERE topicId = :topicId)"
    )
    fun getProgress(topicId: Long): Flow<TopicChecklistProgress>

    @Query("SELECT COUNT(*) FROM topic_checklist_items WHERE topicId = :topicId")
    fun getItemCountForTopic(topicId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TopicChecklistItem): Long

    @Update
    suspend fun updateItem(item: TopicChecklistItem)

    @Delete
    suspend fun deleteItem(item: TopicChecklistItem)

    @Update
    suspend fun updateItems(items: List<TopicChecklistItem>)
}
