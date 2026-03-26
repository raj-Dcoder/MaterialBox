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

object YoutubeRssFetcher {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)

    suspend fun fetchVideosFromChannels(channelUrls: List<String>): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val allVideos = mutableListOf<YoutubeVideo>()
        
        channelUrls.forEach { url ->
            try {
                val channelId = extractChannelId(url)
                if (channelId != null) {
                    val rssUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
                    val videos = fetchRssFeed(rssUrl)
                    allVideos.addAll(videos)
                } else {
                    Log.e("YoutubeRssFetcher", "Could not extract channel ID from $url")
                }
            } catch (e: Exception) {
                Log.e("YoutubeRssFetcher", "Error fetching videos for $url", e)
            }
        }
        
        // Sort by date descending, cap at 20 total across all channels
        allVideos.sortByDescending { it.publishedAt }
        allVideos.take(20)
    }

    private fun extractChannelId(url: String): String? {
        val trimmed = url.trim().removeSuffix("/")
        return when {
            trimmed.contains("/channel/") -> trimmed.substringAfter("/channel/").substringBefore("?").substringBefore("/")
            trimmed.contains("/@") || trimmed.startsWith("@") -> resolveHandleToId(trimmed)
            else -> null
        }
    }

    private fun resolveHandleToId(url: String): String? {
        val fullUrl = if (!url.startsWith("http")) "https://www.youtube.com/$url" else url
        return try {
            val connection = URL(fullUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            
            connection.inputStream.bufferedReader().use { reader ->
                val content = reader.readText()
                // Regex 1: meta identifier
                val regex1 = "<meta itemprop=\"identifier\" content=\"(UC[a-zA-Z0-9_-]+)\">".toRegex()
                val match1 = regex1.find(content)
                if (match1 != null) return match1.groupValues[1]
                
                // Regex 2: channelId in JSON
                val regex2 = "\"channelId\":\"(UC[a-zA-Z0-9_-]+)\"".toRegex()
                val match2 = regex2.find(content)
                if (match2 != null) return match2.groupValues[1]
                
                // Regex 3: canonical URL
                val regex3 = "https://www.youtube.com/channel/(UC[a-zA-Z0-9_-]+)".toRegex()
                val match3 = regex3.find(content)
                if (match3 != null) return match3.groupValues[1]
                
                Log.e("YoutubeRssFetcher", "No channel ID found in content for $fullUrl")
                null
            }
        } catch (e: Exception) {
            Log.e("YoutubeRssFetcher", "Error resolving handle $fullUrl", e)
            null
        }
    }

    private fun fetchRssFeed(rssUrl: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        var connection: HttpURLConnection? = null
        try {
            connection = URL(rssUrl).openConnection() as HttpURLConnection
            val inputStream = connection.inputStream
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var currentVideo: MutableYoutubeVideo? = null
            var channelName = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (name) {
                            "title" -> {
                                if (currentVideo == null) {
                                    if (channelName.isEmpty()) channelName = parser.nextText()
                                } else {
                                    currentVideo.title = parser.nextText()
                                }
                            }
                            "entry" -> currentVideo = MutableYoutubeVideo(channelName = channelName)
                            "link" -> {
                                if (currentVideo != null) {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (href != null && href.contains("watch")) {
                                        currentVideo.videoUrl = href
                                    }
                                }
                            }
                            "published" -> {
                                if (currentVideo != null) {
                                    val dateStr = parser.nextText()
                                    currentVideo.publishedAt = try {
                                        dateFormat.parse(dateStr)
                                    } catch (e: Exception) {
                                        Date()
                                    }
                                }
                            }
                            "thumbnail" -> { // media:thumbnail becomes thumbnail with namespace processing
                                if (currentVideo != null) {
                                    currentVideo.thumbnailUrl = parser.getAttributeValue(null, "url")
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "entry" && currentVideo != null) {
                            val video = currentVideo.toYoutubeVideo()
                            if (video != null) videos.add(video)
                            currentVideo = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("YoutubeRssFetcher", "Error parsing RSS $rssUrl", e)
        } finally {
            connection?.disconnect()
        }
        return videos
    }

    private data class MutableYoutubeVideo(
        var title: String? = null,
        var videoUrl: String? = null,
        var thumbnailUrl: String? = null,
        var publishedAt: Date? = null,
        var channelName: String = ""
    ) {
        fun toYoutubeVideo(): YoutubeVideo? {
            return if (title != null && videoUrl != null && thumbnailUrl != null && publishedAt != null) {
                YoutubeVideo(title!!, videoUrl!!, thumbnailUrl!!, publishedAt!!, channelName)
            } else null
        }
    }
}
