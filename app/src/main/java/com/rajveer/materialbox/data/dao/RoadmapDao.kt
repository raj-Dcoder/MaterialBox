package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.RoadmapItem
import kotlinx.coroutines.flow.Flow

data class RoadmapProgress(
    val completed: Int,
    val total: Int
)

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM roadmap_items WHERE subjectId = :subjectId ORDER BY position ASC, createdAt ASC")
    fun getItemsForSubject(subjectId: Long): Flow<List<RoadmapItem>>

    @Query("SELECT COUNT(*) FROM roadmap_items WHERE subjectId = :subjectId")
    fun getItemCountForSubject(subjectId: Long): Flow<Int>

    @Query("SELECT " +
            "SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed, " +
            "COUNT(*) as total " +
            "FROM roadmap_items WHERE subjectId = :subjectId " +
            "AND id NOT IN (SELECT IFNULL(parentId, -1) FROM roadmap_items WHERE subjectId = :subjectId)")
    fun getProgress(subjectId: Long): Flow<RoadmapProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: RoadmapItem): Long

    @Update
    suspend fun updateItem(item: RoadmapItem)

    @Delete
    suspend fun deleteItem(item: RoadmapItem)
    
    // For drag-and-drop reordering
    @Update
    suspend fun updateItems(items: List<RoadmapItem>)
}
