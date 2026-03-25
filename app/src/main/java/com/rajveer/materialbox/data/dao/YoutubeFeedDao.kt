package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.YoutubeFeed
import kotlinx.coroutines.flow.Flow

@Dao
interface YoutubeFeedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYoutubeFeed(feed: YoutubeFeed): Long

    @Update
    suspend fun updateYoutubeFeed(feed: YoutubeFeed)

    @Delete
    suspend fun deleteYoutubeFeed(feed: YoutubeFeed)

    @Query("SELECT * FROM youtube_feeds WHERE id = :id")
    fun getYoutubeFeedById(id: Long): Flow<YoutubeFeed?>

    @Query("SELECT * FROM youtube_feeds WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getYoutubeFeedsForSubject(subjectId: Long): Flow<List<YoutubeFeed>>
}
