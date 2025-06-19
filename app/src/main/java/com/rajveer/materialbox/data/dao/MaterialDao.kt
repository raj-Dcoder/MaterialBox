package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.Material
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getMaterialsForTopic(topicId: Long): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE id = :id")
    fun getMaterialById(id: Long): Flow<Material?>

    @Query("SELECT * FROM materials ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMaterials(limit: Int): Flow<List<Material>>

    @Query("SELECT * FROM materials ORDER BY viewCount DESC LIMIT :limit")
    fun getMostViewedMaterials(limit: Int = 10): Flow<List<Material>>

    @Query("SELECT * FROM materials WHERE title LIKE '%' || :query || '%'")
    fun searchMaterials(query: String): Flow<List<Material>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: Material): Long

    @Update
    suspend fun updateMaterial(material: Material)

    @Delete
    suspend fun deleteMaterial(material: Material)

    @Query("UPDATE materials SET viewCount = viewCount + 1 WHERE id = :materialId")
    suspend fun incrementViewCount(materialId: Long)
} 