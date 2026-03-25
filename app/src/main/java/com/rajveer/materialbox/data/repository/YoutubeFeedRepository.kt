package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.YoutubeFeedDao
import com.rajveer.materialbox.data.entity.YoutubeFeed
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeFeedRepository @Inject constructor(
    private val youtubeFeedDao: YoutubeFeedDao
) {
    fun getYoutubeFeedsForSubject(subjectId: Long): Flow<List<YoutubeFeed>> =
        youtubeFeedDao.getYoutubeFeedsForSubject(subjectId)

    fun getYoutubeFeedById(id: Long): Flow<YoutubeFeed?> =
        youtubeFeedDao.getYoutubeFeedById(id)

    suspend fun insertYoutubeFeed(feed: YoutubeFeed): Long =
        youtubeFeedDao.insertYoutubeFeed(feed)

    suspend fun updateYoutubeFeed(feed: YoutubeFeed) =
        youtubeFeedDao.updateYoutubeFeed(feed)

    suspend fun deleteYoutubeFeed(feed: YoutubeFeed) =
        youtubeFeedDao.deleteYoutubeFeed(feed)
}
