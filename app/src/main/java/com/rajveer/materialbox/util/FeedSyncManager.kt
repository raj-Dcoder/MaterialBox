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

    // Guards activeJobs map to avoid race conditions from concurrent calls
    private val mutex = Mutex()
    private val activeJobs = mutableMapOf<Long, Job>()

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
                    // Launch each feed sync in its own coroutine so they can be tracked/joined individually
                    launch {
                        syncFeedInternal(feed.id, feed.channelUrls, "syncAll")
                    }
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
                syncFeedInternal(feedId, feed.channelUrls, "onDemand", forceFresh = false)
            } catch (e: Exception) {
                Log.e(TAG, "syncFeed($feedId) outer error", e)
            }
        }
    }

    /**
     * Force-syncs a single feed, bypassing the staleness check.
     * Used for manual pull-to-refresh so the user always gets fresh data.
     * Now suspendable so callers can wait for completion.
     */
    suspend fun forceSync(feedId: Long) {
        // We launch in the manager's scope to ensure the sync finishes even if the
        // caller's scope (like a ViewModel) is cancelled, but we join it to wait.
        val job = scope.launch {
            try {
                val feed = feedRepository.getYoutubeFeedById(feedId).first()
                if (feed == null) {
                    Log.w(TAG, "forceSync: no feed found for id=$feedId")
                    return@launch
                }
                syncFeedInternal(feedId, feed.channelUrls, "forceRefresh", forceFresh = true)
            } catch (e: Exception) {
                Log.e(TAG, "forceSync($feedId) outer error", e)
            }
        }
        job.join()
    }

    /**
     * Core refresh logic for a single feed.
     * - Checks staleness first; skips if fresh.
     * - Deduplicates via activeJobs map (joins existing job if present).
     * - Fetches RSS channels and writes to Room cache.
     */
    private suspend fun syncFeedInternal(
        feedId: Long,
        channelUrls: List<String>,
        source: String,
        forceFresh: Boolean = false
    ) {
        if (channelUrls.isEmpty()) {
            Log.d(TAG, "[$source] Feed $feedId has no channels, skipping.")
            return
        }

        // 1. Join existing job if one is already running for this feed
        val existingJob = mutex.withLock { activeJobs[feedId] }
        if (existingJob != null && existingJob.isActive) {
            Log.d(TAG, "[$source] Feed $feedId already syncing, joining existing job...")
            existingJob.join()

            // 2. After joining, check if the data is now fresh.
            // If it is, and we aren't forcing a fresh sync, we can return.
            if (!forceFresh && isFresh(feedId)) {
                Log.d(TAG, "[$source] Feed $feedId is now fresh after waiting, skipping.")
                return
            }
        }

        // 3. Register our current coroutine job as the active job for this feed
        val myJob = kotlin.coroutines.coroutineContext[Job]
        if (myJob != null) {
            mutex.withLock { activeJobs[feedId] = myJob }
        }

        try {
            // Skip if data is still fresh (unless force-refreshing)
            if (!forceFresh && isFresh(feedId)) {
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
                    sourceUrl = v.sourceUrl,
                    cachedAt = now
                )
            }
            cacheRepository.mergeCache(feedId, cachedEntities)
            Log.d(TAG, "[$source] Feed $feedId synced — ${videos.size} videos merged into cache.")
        } catch (e: Exception) {
            // Per-feed error: log and move on; do NOT rethrow so other feeds are unaffected
            Log.e(TAG, "[$source] Feed $feedId sync error", e)
        } finally {
            // 4. Clean up: only remove from activeJobs if we are still the registered job
            mutex.withLock {
                if (activeJobs[feedId] === myJob) {
                    activeJobs.remove(feedId)
                }
            }
        }
    }

    /** Returns true if the cached data for this feed is younger than STALE_THRESHOLD_MS. */
    private suspend fun isFresh(feedId: Long): Boolean {
        val lastCachedAt = cacheRepository.getLastCachedAt(feedId) ?: return false
        return (Date().time - lastCachedAt.time) < STALE_THRESHOLD_MS
    }
}
