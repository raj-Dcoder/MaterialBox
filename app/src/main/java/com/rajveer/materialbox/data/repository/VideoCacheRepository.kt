package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.CachedVideoDao
import com.rajveer.materialbox.data.entity.CachedVideo
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoCacheRepository @Inject constructor(
    private val cachedVideoDao: CachedVideoDao
) {
    fun getVideosForFeed(feedId: Long): Flow<List<CachedVideo>> =
        cachedVideoDao.getVideosForFeed(feedId)

    suspend fun getLastCachedAt(feedId: Long): Date? {
        val ms = cachedVideoDao.getLastCachedAt(feedId) ?: return null
        return Date(ms)
    }

    suspend fun replaceCache(feedId: Long, videos: List<CachedVideo>) =
        cachedVideoDao.replaceCache(feedId, videos)

    suspend fun mergeCache(feedId: Long, videos: List<CachedVideo>) =
        cachedVideoDao.mergeCache(feedId, videos)

    suspend fun clearCache(feedId: Long) =
        cachedVideoDao.deleteByFeedId(feedId)

    suspend fun getChannelNamesMapping(feedId: Long) =
        cachedVideoDao.getChannelNamesMapping(feedId)
}
