package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "roadmap_items",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoadmapItem::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["parentId"])
    ]
)
data class RoadmapItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val parentId: Long? = null,
    val text: String,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val createdAt: Date = Date()
)
