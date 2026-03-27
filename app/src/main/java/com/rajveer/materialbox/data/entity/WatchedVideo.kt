package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Tracks which YouTube videos the user has watched.
 * Keyed by videoUrl so it works across feeds if the same video appears in multiple feeds.
 */
@Entity(tableName = "watched_videos")
data class WatchedVideo(
    @PrimaryKey
    val videoUrl: String,
    val watchedAt: Date = Date()
)
