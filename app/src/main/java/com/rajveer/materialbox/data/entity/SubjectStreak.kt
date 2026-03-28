package com.rajveer.materialbox.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "subject_streaks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubjectStreak(
    @PrimaryKey
    val subjectId: Long,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "" // Stored as "yyyy-MM-dd"
)
