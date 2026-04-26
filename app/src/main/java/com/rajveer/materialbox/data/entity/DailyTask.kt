package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "daily_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["plannedDate"]),
        Index(value = ["subjectId"])
    ]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val plannedDate: String,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val position: Int = 0,
    val subjectId: Long? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
