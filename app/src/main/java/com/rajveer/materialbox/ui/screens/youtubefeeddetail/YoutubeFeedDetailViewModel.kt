package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import com.rajveer.materialbox.util.YoutubeFeedCache
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
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class YoutubeFeedDetailViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
    private val feedCache: YoutubeFeedCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val feedId: Long = checkNotNull(savedStateHandle["feedId"])

    private val _uiState = MutableStateFlow(YoutubeFeedUiState())
    val uiState: StateFlow<YoutubeFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            youtubeFeedRepository.getYoutubeFeedById(feedId)
                .onEach { feed ->
                    _uiState.update { it.copy(feed = feed) }
                    if (feed != null) {
                        // Check singleton cache first — survives back-navigation
                        val cached = feedCache.get(feedId)
                        if (cached != null) {
                            // Cache is fresh (< 6h): show instantly, no network call
                            _uiState.update { it.copy(videos = cached) }
                        } else {
                            // Cache is stale or empty: fetch from network
                            fetchVideos(feed.channelUrls, isManualRefresh = false)
                        }
                    }
                }
                .collect()
        }
    }

    /** Called by pull-to-refresh — always fetches fresh regardless of TTL */
    fun refreshFeed() {
        val channelUrls = _uiState.value.feed?.channelUrls ?: return
        fetchVideos(channelUrls, isManualRefresh = true)
    }

    private fun fetchVideos(channelUrls: List<String>, isManualRefresh: Boolean) {
        viewModelScope.launch {
            if (isManualRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }
            try {
                val videos = YoutubeRssFetcher.fetchVideosFromChannels(channelUrls)
                feedCache.put(feedId, videos)   // store in singleton cache
                _uiState.update {
                    it.copy(videos = videos, isLoading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
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
                    // Channels changed — invalidate cache and re-fetch
                    feedCache.invalidate(feedId)
                    fetchVideos(updated.channelUrls, isManualRefresh = false)
                }
            }
        }
    }

    fun deleteFeed() {
        viewModelScope.launch {
            _uiState.value.feed?.let {
                feedCache.invalidate(feedId)
                youtubeFeedRepository.deleteYoutubeFeed(it)
            }
        }
    }
}
