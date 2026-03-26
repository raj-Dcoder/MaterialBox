package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.CachedVideo
import com.rajveer.materialbox.data.repository.VideoCacheRepository
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
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
    val channelName: String
)

data class YoutubeFeedUiState(
    val feed: com.rajveer.materialbox.data.entity.YoutubeFeed? = null,
    val videos: List<YoutubeVideo> = emptyList(),
    val isLoading: Boolean = false,        // first-ever load (empty cache)
    val isRefreshing: Boolean = false,     // pull-to-refresh in progress
    val lastCachedAt: Date? = null,        // shown as "Last updated X ago"
    val error: String? = null
)

@HiltViewModel
class YoutubeFeedDetailViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
    private val videoCacheRepository: VideoCacheRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val feedId: Long = checkNotNull(savedStateHandle["feedId"])

    private val _uiState = MutableStateFlow(YoutubeFeedUiState())
    val uiState: StateFlow<YoutubeFeedUiState> = _uiState.asStateFlow()

    init {
        observeFeed()
        observeCache()
    }

    private fun observeFeed() {
        viewModelScope.launch {
            youtubeFeedRepository.getYoutubeFeedById(feedId)
                .collect { feed ->
                    _uiState.update { it.copy(feed = feed) }

                    // First-time load: if DB cache is empty, fetch from network
                    if (feed != null) {
                        val cached = videoCacheRepository.getLastCachedAt(feedId)
                        if (cached == null) {
                            // No cache at all — do the initial fetch
                            fetchVideos(isManualRefresh = false)
                        }
                    }
                }
        }
    }

    private fun observeCache() {
        viewModelScope.launch {
            videoCacheRepository.getVideosForFeed(feedId)
                .collect { cached ->
                    if (cached.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                videos = cached.map { it.toUiModel() },
                                lastCachedAt = cached.first().cachedAt,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    /** Pull-to-refresh — always fetches fresh from network. */
    fun refreshFeed() {
        fetchVideos(isManualRefresh = true)
    }

    private fun fetchVideos(isManualRefresh: Boolean) {
        val channelUrls = _uiState.value.feed?.channelUrls
        if (channelUrls.isNullOrEmpty()) return

        viewModelScope.launch {
            if (isManualRefresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            try {
                val videos = YoutubeRssFetcher.fetchVideosFromChannels(channelUrls)
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
                // Atomically replace old cache with fresh data
                videoCacheRepository.replaceCache(feedId, cachedEntities)
                // UI updates automatically via observeCache() Flow
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = "Couldn't refresh: ${e.message}"
                    )
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
                    // Channels changed — clear old cache and re-fetch
                    videoCacheRepository.clearCache(feedId)
                    fetchVideos(isManualRefresh = false)
                }
            }
        }
    }

    fun deleteFeed() {
        viewModelScope.launch {
            _uiState.value.feed?.let {
                youtubeFeedRepository.deleteYoutubeFeed(it)
                // cached_videos cascade-deletes automatically via FK
            }
        }
    }

    private fun CachedVideo.toUiModel() = YoutubeVideo(
        title = title,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        publishedAt = publishedAt,
        channelName = channelName
    )
}
