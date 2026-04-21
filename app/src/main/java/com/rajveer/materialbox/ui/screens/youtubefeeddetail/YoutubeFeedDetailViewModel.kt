package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.CachedVideo
import com.rajveer.materialbox.data.repository.VideoCacheRepository
import com.rajveer.materialbox.data.repository.WatchedVideoRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import com.rajveer.materialbox.data.repository.StreakRepository
import com.rajveer.materialbox.util.FeedSyncManager
import com.rajveer.materialbox.util.YoutubeRssFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class YoutubeVideo(
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val publishedAt: Date,
    val channelName: String,
    val sourceUrl: String,
    val isWatched: Boolean = false
)

data class YoutubeFeedUiState(
    val feed: com.rajveer.materialbox.data.entity.YoutubeFeed? = null,
    val videos: List<YoutubeVideo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastCachedAt: Date? = null,
    val channelNames: Map<String, String> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class YoutubeFeedDetailViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
    private val videoCacheRepository: VideoCacheRepository,
    private val watchedVideoRepository: WatchedVideoRepository,
    private val feedSyncManager: FeedSyncManager,
    private val streakRepository: StreakRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val feedId: Long = checkNotNull(savedStateHandle["feedId"])

    private val _uiState = MutableStateFlow(YoutubeFeedUiState())
    val uiState: StateFlow<YoutubeFeedUiState> = _uiState.asStateFlow()

    init {
        observeFeed()
        observeVideosWithWatchedState()
        loadChannelNamesFromCache()
    }

    private fun loadChannelNamesFromCache() {
        viewModelScope.launch {
            val mapping = videoCacheRepository.getChannelNamesMapping(feedId)
            _uiState.update { state ->
                state.copy(channelNames = mapping.associate { it.sourceUrl to it.channelName })
            }
        }
    }

    fun resolveChannelName(url: String) {
        if (url.isBlank() || _uiState.value.channelNames.containsKey(url)) return
        viewModelScope.launch {
            val name = YoutubeRssFetcher.fetchChannelName(url)
            if (name != null) {
                _uiState.update { state ->
                    state.copy(channelNames = state.channelNames + (url to name))
                }
            }
        }
    }

    private fun observeFeed() {
        viewModelScope.launch {
            youtubeFeedRepository.getYoutubeFeedById(feedId)
                .collect { feed ->
                    _uiState.update { it.copy(feed = feed) }
                    if (feed != null) {
                        // Trigger a background sync whenever the screen is opened.
                        // FeedSyncManager deduplicates and skips if already fresh/running.
                        feedSyncManager.syncFeed(feedId)
                    }
                }
        }
    }

    /** Combines cached videos with the set of watched URLs into a single reactive stream. */
    private fun observeVideosWithWatchedState() {
        viewModelScope.launch {
            combine(
                videoCacheRepository.getVideosForFeed(feedId),
                watchedVideoRepository.getAllWatchedUrls()
            ) { cached, watchedUrls ->
                cached.map { it.toUiModel(watchedUrls) }
            }.collect { videos ->
                val lastCachedAt = videoCacheRepository.getLastCachedAt(feedId)
                _uiState.update { state ->
                    state.copy(
                        videos = videos,
                        lastCachedAt = lastCachedAt,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    /** Called when user taps a video — marks it watched AND opens YouTube. */
    fun markWatched(videoUrl: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            watchedVideoRepository.markWatched(videoUrl)
            _uiState.value.feed?.subjectId?.let {
                streakRepository.recordActivity(it)
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            feedSyncManager.forceSync(feedId)
            
            val isLimited = YoutubeRssFetcher.isCurrentlyRateLimited()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    error = if (isLimited) "YouTube is rate-limiting requests. Please try again in a few minutes." else null
                )
            }
        }
    }

    private fun fetchVideos(isManualRefresh: Boolean, channelUrlsOverride: List<String>? = null) {
        val channelUrls = channelUrlsOverride ?: _uiState.value.feed?.channelUrls
        if (channelUrls.isNullOrEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                if (isManualRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            try {
                val videos = YoutubeRssFetcher.fetchVideosFromChannels(channelUrls)
                if (videos.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = "Couldn't find videos for the configured channels/playlists."
                        )
                    }
                    return@launch
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
                videoCacheRepository.mergeCache(feedId, cachedEntities)
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Couldn't refresh: ${e.message}")
                }
            }
        }
    }

    fun updateFeed(name: String, channelUrls: List<String>) {
        viewModelScope.launch {
            _uiState.value.feed?.let { currentFeed ->
                val updated = currentFeed.copy(
                    name = name.trim(),
                    channelUrls = channelUrls.filter { it.isNotBlank() }.map { it.trim() },
                    updatedAt = Date()
                )
                youtubeFeedRepository.updateYoutubeFeed(updated)
                if (updated.channelUrls != currentFeed.channelUrls) {
                    videoCacheRepository.clearCache(feedId)
                    fetchVideos(isManualRefresh = false, channelUrlsOverride = updated.channelUrls)
                }
            }
        }
    }

    fun deleteFeed() {
        viewModelScope.launch {
            _uiState.value.feed?.let { youtubeFeedRepository.deleteYoutubeFeed(it) }
        }
    }

    private fun CachedVideo.toUiModel(watchedUrls: Set<String>) = YoutubeVideo(
        title = title,
        videoUrl = videoUrl,
        thumbnailUrl = YoutubeRssFetcher.upgradeToHighQualityThumbnail(thumbnailUrl),
        publishedAt = publishedAt,
        channelName = channelName,
        sourceUrl = sourceUrl,
        isWatched = videoUrl in watchedUrls
    )
}
