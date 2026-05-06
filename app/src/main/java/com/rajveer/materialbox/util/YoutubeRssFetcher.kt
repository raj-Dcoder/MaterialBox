package com.rajveer.materialbox.util

import android.util.Log
import android.util.Xml
import com.rajveer.materialbox.ui.screens.youtubefeeddetail.YoutubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

object YoutubeRssFetcher {

    private const val TAG = "YoutubeRssFetcher"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"

    /** Max RSS retry attempts for transient HTTP failures */
    private const val MAX_RSS_RETRIES = 2
    private const val INITIAL_RETRY_DELAY_MS = 1000L

    /** In-memory cache to avoid re-resolving handles (@mkbhd) in the same session */
    private val resolvedUrlCache = Collections.synchronizedMap(mutableMapOf<String, String>())

    /** Flag to track if we've hit a 429 rate limit recently to bail out early */
    @Volatile
    private var isRateLimited = false
    private var rateLimitResetTime = 0L
    private const val RATE_LIMIT_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

    /**
     * Fetches videos from ALL given channel/playlist URLs.
     */
    suspend fun fetchVideosFromChannels(channelUrls: List<String>): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val allVideos = mutableListOf<YoutubeVideo>()

        // Check if we are currently rate limited
        if (isRateLimited && System.currentTimeMillis() < rateLimitResetTime) {
            Log.w(TAG, "Still in rate-limit cooldown, skipping network fetch.")
            return@withContext emptyList()
        }

        // Process in chunks of 3 (reduced from 5 to be more polite)
        channelUrls.chunked(3).forEach { chunk ->
            val deferredVideos = chunk.map { url ->
                async {
                    try {
                        val rssUrl = buildRssUrl(url)
                        if (rssUrl != null) {
                            fetchRssFeedWithRetry(rssUrl, url)
                        } else {
                            Log.w(TAG, "Skipping URL, could not resolve RSS: $url")
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error preparing feed for $url", e)
                        emptyList()
                    }
                }
            }
            allVideos.addAll(deferredVideos.awaitAll().flatten())
            
            // If any fetch marked us as rate limited, stop processing this chunk
            if (isRateLimited) return@forEach
        }

        allVideos
            .distinctBy { it.videoUrl }
            .sortedByDescending { it.publishedAt }
    }

    private suspend fun buildRssUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        // 0. Check session cache first
        resolvedUrlCache[trimmed]?.let { return it }

        // 1. Already a direct RSS feed URL
        if (trimmed.contains("feeds/videos.xml")) return trimmed

        // 2. Raw channel ID (e.g. "UCxxxxxx")
        if (trimmed.startsWith("UC") && !trimmed.contains("/")) {
            val rss = "https://www.youtube.com/feeds/videos.xml?channel_id=$trimmed"
            resolvedUrlCache[trimmed] = rss
            return rss
        }

        // 3. @handle shorthand (e.g. "@mkbhd")
        if (trimmed.startsWith("@")) {
            val rss = resolvePageToRssUrl("https://www.youtube.com/$trimmed")
            if (rss != null) resolvedUrlCache[trimmed] = rss
            return rss
        }

        // 4. /channel/UCxxx path
        val channelId = extractChannelId(trimmed)
        if (channelId != null) {
            val rss = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
            resolvedUrlCache[trimmed] = rss
            return rss
        }

        // 5. Playlist URL (e.g. /playlist?list=PLxxx)
        val playlistId = extractPlaylistId(trimmed)
        if (playlistId != null) {
            val rss = "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
            resolvedUrlCache[trimmed] = rss
            return rss
        }

        // 6. Any other YouTube URL — resolve by fetching the page HTML
        if (trimmed.contains("youtube.com/") || trimmed.contains("youtu.be/")) {
            val rss = resolvePageToRssUrl(trimmed)
            if (rss != null) resolvedUrlCache[trimmed] = rss
            return rss
        }

        return null
    }

    private fun extractPlaylistId(url: String): String? {
        return try {
            val uri = android.net.Uri.parse(url)
            val listParam = uri.getQueryParameter("list")
            if (!listParam.isNullOrBlank()) listParam else null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractChannelId(url: String): String? {
        val trimmed = url.trim().removeSuffix("/")
        return when {
            trimmed.contains("/channel/") -> trimmed.substringAfter("/channel/").substringBefore("?").substringBefore("/")
            else -> null
        }?.takeIf { it.startsWith("UC") }
    }

    private suspend fun resolvePageToRssUrl(url: String): String? {
        // If we hit a rate limit, don't even try to scrape HTML
        if (isRateLimited && System.currentTimeMillis() < rateLimitResetTime) return null

        // Try up to 2 times
        repeat(2) { attempt ->
            val content = fetchPageContent(url)
            if (content != null) {
                extractChannelIdFromHtml(content)?.let { channelId ->
                    return "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
                }
                extractPlaylistIdFromHtml(content)?.let { playlistId ->
                    return "https://www.youtube.com/feeds/videos.xml?playlist_id=$playlistId"
                }
            }
            if (attempt == 0 && !isRateLimited) delay(500) // reduced delay
        }
        return null
    }

    private fun fetchPageContent(url: String): String? {
        val fullUrl = if (!url.startsWith("http")) "https://$url" else url
        return try {
            val connection = URL(fullUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // reduced timeout
            connection.readTimeout = 5000    // reduced timeout
            connection.useCaches = true      // allow some caching
            connection.setRequestProperty("User-Agent", USER_AGENT)
            
            val responseCode = connection.responseCode
            if (responseCode == 429) {
                Log.e(TAG, "HIT RATE LIMIT (429) on $fullUrl")
                markRateLimited()
                return null
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving page $fullUrl", e)
            null
        }
    }

    private fun markRateLimited() {
        isRateLimited = true
        rateLimitResetTime = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
    }

    /** Returns true if we are currently in a rate-limit cooldown period */
    fun isCurrentlyRateLimited(): Boolean {
        return isRateLimited && System.currentTimeMillis() < rateLimitResetTime
    }

    suspend fun fetchVideoDurationSeconds(videoUrl: String): Int? = withContext(Dispatchers.IO) {
        try {
            val html = fetchPageContent(videoUrl) ?: return@withContext null
            val match = Regex(""""lengthSeconds":"(\d+)"""").find(html)
            match?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching duration for $videoUrl", e)
            null
        }
    }

    private fun extractChannelIdFromHtml(content: String): String? {
        val patterns = listOf(
            "<meta itemprop=\"identifier\" content=\"(UC[a-zA-Z0-9_-]+)\">".toRegex(),
            "\"channelId\":\"(UC[a-zA-Z0-9_-]+)\"".toRegex(),
            "https://www\\.youtube\\.com/channel/(UC[a-zA-Z0-9_-]+)".toRegex(),
            "channel/(UC[a-zA-Z0-9_-]+)".toRegex()
        )

        return patterns.asSequence()
            .mapNotNull { it.find(content)?.groupValues?.getOrNull(1) }
            .firstOrNull()
    }

    private fun extractPlaylistIdFromHtml(content: String): String? {
        val patterns = listOf(
            "\"playlistId\":\"([A-Za-z0-9_-]+)\"".toRegex(),
            "list=([A-Za-z0-9_-]+)".toRegex()
        )

        return patterns.asSequence()
            .mapNotNull { it.find(content)?.groupValues?.getOrNull(1) }
            .firstOrNull()
    }

    // ── RSS fetching with retry ─────────────────────────────────────────

    /**
     * Wraps [fetchRssFeed] with exponential-backoff retry for transient HTTP errors.
     * @param sourceUrl the original user-supplied URL, for logging purposes only.
     */
    private suspend fun fetchRssFeedWithRetry(rssUrl: String, sourceUrl: String): List<YoutubeVideo> {
        var lastException: Exception? = null
        var delayMs = INITIAL_RETRY_DELAY_MS

        repeat(MAX_RSS_RETRIES) { attempt ->
            try {
                val videos = fetchRssFeed(rssUrl, sourceUrl)
                if (videos.isNotEmpty() || attempt == MAX_RSS_RETRIES - 1) {
                    if (attempt > 0) {
                        Log.d(TAG, "RSS fetch succeeded on attempt ${attempt + 1} for $sourceUrl")
                    }
                    return videos
                }
                // Empty result could be transient — retry
                Log.w(TAG, "RSS returned 0 videos for $sourceUrl (attempt ${attempt + 1}), retrying…")
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "RSS fetch attempt ${attempt + 1} failed for $sourceUrl: ${e.message}")
            }

            if (attempt < MAX_RSS_RETRIES - 1) {
                delay(delayMs)
                delayMs *= 2
            }
        }

        Log.e(TAG, "All $MAX_RSS_RETRIES RSS fetch attempts failed for $sourceUrl ($rssUrl)", lastException)
        return emptyList()
    }

    private fun fetchRssFeed(rssUrl: String, sourceUrl: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        var connection: HttpURLConnection? = null
        try {
            connection = URL(rssUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/atom+xml, application/xml, text/xml, */*")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText().take(200) } ?: "no error body"
                } catch (_: Exception) { "could not read error body" }
                Log.e(TAG, "RSS fetch failed for $rssUrl — HTTP $responseCode: $errorBody")
                // Throw on retryable errors so the retry loop can catch them
                if (responseCode in 500..599 || responseCode == 429 || responseCode == 404) {
                    throw java.io.IOException("HTTP $responseCode for $rssUrl")
                }
                return emptyList()
            }

            val inputStream = connection.inputStream
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentVideo: MutableYoutubeVideo? = null
            var feedTitle = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (name) {
                            "title" -> {
                                val text = readTextSafe(parser)
                                if (currentVideo == null) {
                                    if (feedTitle.isEmpty()) feedTitle = text
                                } else {
                                    currentVideo.title = text
                                }
                            }
                            "entry" -> currentVideo = MutableYoutubeVideo(channelName = feedTitle, sourceUrl = sourceUrl)
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (currentVideo != null && href != null && href.contains("watch")) {
                                    currentVideo.videoUrl = href
                                }
                            }
                            "published" -> {
                                val text = readTextSafe(parser)
                                if (currentVideo != null && text.isNotBlank()) {
                                    currentVideo.publishedAt = parseDateSafe(text)
                                }
                            }
                            "updated" -> {
                                val text = readTextSafe(parser)
                                // Only use <updated> if <published> wasn't already set
                                if (currentVideo != null && currentVideo.publishedAt == null && text.isNotBlank()) {
                                    currentVideo.publishedAt = parseDateSafe(text)
                                }
                            }
                            "thumbnail" -> {
                                // Handles both <media:thumbnail> (namespace-aware) and plain <thumbnail>
                                val url = parser.getAttributeValue(null, "url")
                                if (currentVideo != null && url != null) {
                                    currentVideo.thumbnailUrl = upgradeToHighQualityThumbnail(url)
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "entry" && currentVideo != null) {
                            // Generate fallback thumbnail from video URL if none was found
                            if (currentVideo.thumbnailUrl == null && currentVideo.videoUrl != null) {
                                currentVideo.thumbnailUrl = buildFallbackThumbnail(currentVideo.videoUrl!!)
                            }
                            currentVideo.toYoutubeVideo()?.let { videos.add(it) }
                            currentVideo = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS $rssUrl: ${e.message}")
            // Re-throw IOExceptions so the retry loop can handle them
            if (e is java.io.IOException) throw e
        } finally {
            connection?.disconnect()
        }
        return videos
    }

    /**
     * Safely reads text content from the current XML element.
     * Handles empty elements like `<title/>` without crashing.
     */
    private fun readTextSafe(parser: XmlPullParser): String {
        var result = ""
        try {
            val next = parser.next()
            if (next == XmlPullParser.TEXT) {
                result = parser.text ?: ""
                parser.nextTag() // Move to END_TAG
            }
            // If next is already END_TAG (empty element), result stays ""
        } catch (e: Exception) {
            Log.w(TAG, "readTextSafe: could not read text: ${e.message}")
        }
        return result
    }

    /**
     * Constructs a high-quality thumbnail URL from a YouTube watch URL.
     * e.g. "https://www.youtube.com/watch?v=dQw4w9WgXcQ" → "https://i.ytimg.com/vi/dQw4w9WgXcQ/sddefault.jpg"
     */
    private fun buildFallbackThumbnail(videoUrl: String): String? {
        return try {
            val uri = android.net.Uri.parse(videoUrl)
            val videoId = uri.getQueryParameter("v")
            if (!videoId.isNullOrBlank()) {
                "https://i.ytimg.com/vi/$videoId/sddefault.jpg"
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Upgrades any ytimg.com thumbnail URL to the highest universally-available
     * quality (sddefault.jpg — 640×480). Works for RSS-provided URLs like:
     *   https://i1.ytimg.com/vi/VIDEO_ID/hqdefault.jpg
     *   https://i.ytimg.com/vi/VIDEO_ID/default.jpg
     *
     * If the URL doesn't match the expected pattern, it is returned unchanged.
     */
    fun upgradeToHighQualityThumbnail(url: String): String {
        // Pattern: https://i[N].ytimg.com/vi/VIDEO_ID/<quality>.jpg
        val regex = Regex("""(https?://i\d*\.ytimg\.com/vi/[^/]+/)(?:default|mqdefault|hqdefault|sddefault|maxresdefault)\.jpg""")
        val match = regex.find(url)
        return if (match != null) {
            "${match.groupValues[1]}sddefault.jpg"
        } else {
            url
        }
    }

    private data class MutableYoutubeVideo(
        var title: String? = null,
        var videoUrl: String? = null,
        var thumbnailUrl: String? = null,
        var publishedAt: Date? = null,
        var channelName: String = "",
        var sourceUrl: String = ""
    ) {
        fun toYoutubeVideo(): YoutubeVideo? {
            return if (title != null && videoUrl != null && thumbnailUrl != null && publishedAt != null) {
                YoutubeVideo(title!!, videoUrl!!, thumbnailUrl!!, publishedAt!!, channelName, sourceUrl)
            } else null
        }
    }

    suspend fun fetchChannelName(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val rssUrl = buildRssUrl(url) ?: return@withContext null
            
            // We only need the feed title, so we can stop parsing after <title> is found
            var connection: HttpURLConnection? = null
            try {
                connection = URL(rssUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                parser.setInput(connection.inputStream, null)
                
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "title") {
                        val title = readTextSafe(parser)
                        if (title.isNotBlank()) return@withContext title
                    }
                    eventType = parser.next()
                }
            } finally {
                connection?.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching channel name for $url", e)
        }
        null
    }

    private fun parseDateSafe(dateStr: String): Date {
        if (dateStr.isBlank()) return Date(0)
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US).parse(dateStr) ?: Date(0)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).parse(dateStr) ?: Date(0)
            } catch (e2: Exception) {
                Date(0)
            }
        }
    }
}
