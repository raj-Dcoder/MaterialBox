package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.YoutubeFeed
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
    val feed: YoutubeFeed? = null,
    val videos: List<YoutubeVideo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class YoutubeFeedDetailViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
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
                        fetchVideos(feed.channelUrls)
                    }
                }
                .collect()
        }
    }

    private fun fetchVideos(channelUrls: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val videos = YoutubeRssFetcher.fetchVideosFromChannels(channelUrls)
                _uiState.update { it.copy(videos = videos, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
    
    fun deleteFeed() {
        viewModelScope.launch {
            _uiState.value.feed?.let {
                youtubeFeedRepository.deleteYoutubeFeed(it)
            }
        }
    }
}
