package com.rajveer.materialbox.util

import android.util.Log
import com.rajveer.materialbox.data.entity.CachedVideo
import com.rajveer.materialbox.data.repository.VideoCacheRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that owns a long-lived background coroutine scope and coordinates
 * YouTube RSS refresh for every feed in the database.
 *
 * Design decisions:
 * - SupervisorJob: a failure in one feed's coroutine never cancels the others.
 * - Dispatchers.IO: all network work runs off the main thread.
 * - Per-feed deduplication: if a feed is already being synced, we skip launching
 *   a second job for it instead of queueing up duplicate network calls.
 * - Staleness threshold: 30 minutes; fresh feeds are skipped silently.
 * - Sequential per-channel fetch with 500 ms inter-feed delay to be polite to
 *   YouTube's servers and avoid sudden bursts.
 */
@Singleton
class FeedSyncManager @Inject constructor(
    private val feedRepository: YoutubeFeedRepository,
    private val cacheRepository: VideoCacheRepository
) {
    companion object {
        private const val TAG = "FeedSyncManager"
        private val STALE_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(30)
        private const val INTER_FEED_DELAY_MS = 500L
    }

    // Own background scope - lives for the process lifetime
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Guards activeJobs set to avoid race conditions from concurrent calls
    private val mutex = Mutex()
    private val activeJobs = mutableSetOf<Long>()

    /**
     * Kicks off a background refresh for ALL feeds in the DB, one by one.
     * Safe to call from Application.onCreate() - non-blocking.
     */
    fun syncAllFeeds() {
        scope.launch {
            try {
                val feeds = feedRepository.getAllFeeds().first()
                if (feeds.isEmpty()) {
                    Log.d(TAG, "No feeds to sync.")
                    return@launch
                }
                Log.d(TAG, "Starting background sync for ${feeds.size} feed(s).")
                feeds.forEach { feed ->
                    syncFeedInternal(feed.id, feed.channelUrls, "syncAll")
                    // Be polite between feeds to avoid bursting the network
                    delay(INTER_FEED_DELAY_MS)
                }
                Log.d(TAG, "Background sync complete for all feeds.")
            } catch (e: Exception) {
                Log.e(TAG, "syncAllFeeds failed", e)
            }
        }
    }

    /**
     * Syncs a single feed by ID. Skips if already in progress or not stale.
     * Safe to call from a ViewModel - non-blocking, returns immediately.
     */
    fun syncFeed(feedId: Long) {
        scope.launch {
            try {
                val feed = feedRepository.getYoutubeFeedById(feedId).first()
                if (feed == null) {
                    Log.w(TAG, "syncFeed: no feed found for id=$feedId")
                    return@launch
                }
                syncFeedInternal(feedId, feed.channelUrls, "onDemand")
            } catch (e: Exception) {
                Log.e(TAG, "syncFeed($feedId) outer error", e)
            }
        }
    }

    /**
     * Core refresh logic for a single feed.
     * - Checks staleness first; skips if fresh.
     * - Deduplicates via activeJobs set.
     * - Fetches RSS channels and writes to Room cache.
     */
    private suspend fun syncFeedInternal(
        feedId: Long,
        channelUrls: List<String>,
        source: String
    ) {
        if (channelUrls.isEmpty()) {
            Log.d(TAG, "[$source] Feed $feedId has no channels, skipping.")
            return
        }

        // Skip if a job for this feed is already running
        val alreadyRunning = mutex.withLock { !activeJobs.add(feedId) }
        if (alreadyRunning) {
            Log.d(TAG, "[$source] Feed $feedId already syncing, skipping duplicate.")
            return
        }

        try {
            // Skip if data is still fresh
            if (isFresh(feedId)) {
                Log.d(TAG, "[$source] Feed $feedId is fresh, skipping.")
                return
            }

            Log.d(TAG, "[$source] Syncing feed $feedId with ${channelUrls.size} channel(s)…")
            val videos = YoutubeRssFetcher.fetchVideosFromChannels(channelUrls)

            if (videos.isEmpty()) {
                Log.w(TAG, "[$source] Feed $feedId returned 0 videos (possible network/parse issue).")
                // Don't wipe existing cache — keep stale data rather than showing nothing
                return
            }

            val now = Date()
            val cachedEntities = videos.map { v ->
                CachedVideo(
                    feedId = feedId,
                    title = v.title,
                    videoUrl = v.videoUrl,
                    thumbnailUrl = v.thumbnailUrl,
                    publishedAt = v.publishedAt,
                    channelName = v.channelName,
                    cachedAt = now
                )
            }
            cacheRepository.replaceCache(feedId, cachedEntities)
            Log.d(TAG, "[$source] Feed $feedId synced — ${videos.size} videos cached.")
        } catch (e: Exception) {
            // Per-feed error: log and move on; do NOT rethrow so other feeds are unaffected
            Log.e(TAG, "[$source] Feed $feedId sync error", e)
        } finally {
            mutex.withLock { activeJobs.remove(feedId) }
        }
    }

    /** Returns true if the cached data for this feed is younger than STALE_THRESHOLD_MS. */
    private suspend fun isFresh(feedId: Long): Boolean {
        val lastCachedAt = cacheRepository.getLastCachedAt(feedId) ?: return false
        return (Date().time - lastCachedAt.time) < STALE_THRESHOLD_MS
    }
}
