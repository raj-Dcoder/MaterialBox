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

    /**
     * Merges new videos into the existing cache for a feed.
     * Inserts videos that don't already exist (by videoUrl) and updates cachedAt
     * for the entire feed so staleness checks work correctly.
     */
    @Transaction
    suspend fun mergeCache(feedId: Long, videos: List<CachedVideo>) {
        if (videos.isEmpty()) return
        val existingUrls = getVideoUrlsForFeed(feedId).toSet()
        val newVideos = videos.filter { it.videoUrl !in existingUrls }
        if (newVideos.isNotEmpty()) {
            insertAll(newVideos)
        }
        // Always update cachedAt on at least one row so staleness check reflects this sync
        updateCachedAt(feedId, System.currentTimeMillis())
    }

    @Query("SELECT videoUrl FROM cached_videos WHERE feedId = :feedId")
    suspend fun getVideoUrlsForFeed(feedId: Long): List<String>

    @Query("UPDATE cached_videos SET cachedAt = :cachedAtMs WHERE feedId = :feedId AND id = (SELECT id FROM cached_videos WHERE feedId = :feedId LIMIT 1)")
    suspend fun updateCachedAt(feedId: Long, cachedAtMs: Long)

    @Query("DELETE FROM cached_videos WHERE feedId = :feedId")
    suspend fun deleteByFeedId(feedId: Long)
}
