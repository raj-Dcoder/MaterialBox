package com.rajveer.materialbox.ui.screens.addyoutubefeed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.entity.YoutubeFeed
import com.rajveer.materialbox.data.repository.YoutubeFeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddYoutubeFeedViewModel @Inject constructor(
    private val youtubeFeedRepository: YoutubeFeedRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val subjectId: Long = checkNotNull(savedStateHandle["subjectId"])

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
