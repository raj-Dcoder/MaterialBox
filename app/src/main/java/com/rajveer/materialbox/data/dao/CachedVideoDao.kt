package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.CachedVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedVideoDao {

    @Query("SELECT * FROM cached_videos WHERE feedId = :feedId ORDER BY publishedAt DESC")
    fun getVideosForFeed(feedId: Long): Flow<List<CachedVideo>>

    /** Returns the cachedAt timestamp of the most recent entry for this feed (null if empty). */
    @Query("SELECT cachedAt FROM cached_videos WHERE feedId = :feedId ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getLastCachedAt(feedId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<CachedVideo>)

    /** Replaces all cached videos for a feed with a fresh batch. */
    @Transaction
    suspend fun replaceCache(feedId: Long, videos: List<CachedVideo>) {
        deleteByFeedId(feedId)
        insertAll(videos)
    }

    @Query("DELETE FROM cached_videos WHERE feedId = :feedId")
    suspend fun deleteByFeedId(feedId: Long)
}
