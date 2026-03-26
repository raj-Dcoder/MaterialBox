package com.rajveer.materialbox.util

import com.rajveer.materialbox.ui.screens.youtubefeeddetail.YoutubeVideo
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L  // 6 hours

/**
 * Singleton in-memory cache that persists video lists across navigation.
 * Lives as long as the app process — survives back-navigation but clears on full app kill.
 */
@Singleton
class YoutubeFeedCache @Inject constructor() {

    private data class CacheEntry(
        val videos: List<YoutubeVideo>,
        val fetchedAt: Long
    )

    private val cache = mutableMapOf<Long, CacheEntry>()

    /** Returns cached videos if they exist and are within the TTL, otherwise null. */
    fun get(feedId: Long): List<YoutubeVideo>? {
        val entry = cache[feedId] ?: return null
        val age = System.currentTimeMillis() - entry.fetchedAt
        return if (age < CACHE_TTL_MS) entry.videos else null
    }

    /** Stores freshly fetched videos for the given feed. */
    fun put(feedId: Long, videos: List<YoutubeVideo>) {
        cache[feedId] = CacheEntry(videos = videos, fetchedAt = System.currentTimeMillis())
    }

    /** Force-invalidates the cache for a specific feed (e.g., after channel URL changes). */
    fun invalidate(feedId: Long) {
        cache.remove(feedId)
    }
}
