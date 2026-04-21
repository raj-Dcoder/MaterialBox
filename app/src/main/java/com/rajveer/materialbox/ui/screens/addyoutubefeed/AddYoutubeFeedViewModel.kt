package com.rajveer.materialbox.ui.screens.addyoutubefeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.YoutubeFeed
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import com.rajveer.materialbox.util.YoutubeRssFetcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddYoutubeFeedViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _channelNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val channelNames = _channelNames.asStateFlow()

    fun resolveChannelName(url: String) {
        if (url.isBlank() || _channelNames.value.containsKey(url)) return
        viewModelScope.launch {
            val name = YoutubeRssFetcher.fetchChannelName(url)
            if (name != null) {
                _channelNames.update { it + (url to name) }
            }
        }
    }

    fun addYoutubeFeed(name: String, channelUrls: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            val feed = YoutubeFeed(
                subjectId = subjectId,
                name = name,
                channelUrls = channelUrls
            )
            youtubeFeedRepository.insertYoutubeFeed(feed)
            onComplete()
        }
    }
}
