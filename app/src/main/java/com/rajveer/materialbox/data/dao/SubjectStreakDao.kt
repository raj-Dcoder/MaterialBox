package com.rajveer.materialbox.data.dao

import androidx.room.*
import com.rajveer.materialbox.data.entity.SubjectStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectStreakDao {
    @Query("SELECT * FROM subject_streaks WHERE subjectId = :subjectId")
    fun getStreak(subjectId: Long): Flow<SubjectStreak?>

    @Query("SELECT * FROM subject_streaks")
    fun getAllStreaks(): Flow<List<SubjectStreak>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: SubjectStreak)

    @Query("SELECT * FROM subject_streaks WHERE subjectId = :subjectId")
    suspend fun getStreakSync(subjectId: Long): SubjectStreak?
}
