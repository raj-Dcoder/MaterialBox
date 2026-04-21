package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Persists the last fetched video list for a YouTube Feed so it survives app kills.
 */
@Entity(
    tableName = "cached_videos",
    foreignKeys = [
        ForeignKey(
            entity = YoutubeFeed::class,
            parentColumns = ["id"],
            childColumns = ["feedId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("feedId")]
)
data class CachedVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedId: Long,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val publishedAt: Date,
    val channelName: String,
    val sourceUrl: String,      // URL of the channel/playlist this video came from
    val cachedAt: Date = Date()    // when this batch was fetched — used to show "last updated X ago"
)
