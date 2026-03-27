package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.WatchedVideoDao
import com.rajveer.materialbox.data.entity.WatchedVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchedVideoRepository @Inject constructor(
    private val watchedVideoDao: WatchedVideoDao
) {
    fun getAllWatchedUrls(): Flow<Set<String>> =
        watchedVideoDao.getAllWatchedUrls().map { it.toSet() }

    suspend fun markWatched(videoUrl: String) =
        watchedVideoDao.markWatched(WatchedVideo(videoUrl = videoUrl))

    suspend fun unmarkWatched(videoUrl: String) =
        watchedVideoDao.unmarkWatched(videoUrl)
}
