package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.WatchedVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedVideoDao {

    @Query("SELECT videoUrl FROM watched_videos")
    fun getAllWatchedUrls(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markWatched(video: WatchedVideo)

    @Query("DELETE FROM watched_videos WHERE videoUrl = :videoUrl")
    suspend fun unmarkWatched(videoUrl: String)
}
