package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

enum class MaterialType {
    PDF, DOCX, IMAGE, LINK, NOTE, TXT
}

@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Material(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long,
    val type: MaterialType,
    val title: String,
    val pathOrUrl: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val viewCount: Int = 0
) 