package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
        ),
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["subjectId"])
    ]
)
data class Material(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val topicId: Long? = null,
    val subjectId: Long? = null,
    val type: MaterialType,
    val title: String,
    val pathOrUrl: String,
    val originalFileUri: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val viewCount: Int = 0
) 