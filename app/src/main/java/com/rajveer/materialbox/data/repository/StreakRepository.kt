package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.SubjectStreakDao
import com.rajveer.materialbox.data.entity.SubjectStreak
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val streakDao: SubjectStreakDao
) {
    fun getStreak(subjectId: Long): Flow<SubjectStreak?> =
        streakDao.getStreak(subjectId)

    /**
     * Records activity for a subject. 
     * Applies streak rules:
     * - If already active today, do nothing.
     * - If last active yesterday, increment current streak.
     * - If last active earlier, reset current streak to 1.
     */
    suspend fun recordActivity(subjectId: Long) {
        val today = getTodayString()
        val yesterday = getYesterdayString()
        
        val currentStreakEntity = streakDao.getStreakSync(subjectId)
        
        if (currentStreakEntity == null) {
            // First time activity
            streakDao.upsertStreak(SubjectStreak(subjectId, 1, 1, today))
            return
        }
        
        if (currentStreakEntity.lastActiveDate == today) {
            // Already active today
            return
        }
        
        val newStreakCount = if (currentStreakEntity.lastActiveDate == yesterday) {
            currentStreakEntity.currentStreak + 1
        } else {
            1 // Streak broken
        }
        
        val newLongest = maxOf(currentStreakEntity.longestStreak, newStreakCount)
        
        streakDao.upsertStreak(
            currentStreakEntity.copy(
                currentStreak = newStreakCount,
                longestStreak = newLongest,
                lastActiveDate = today
            )
        )
    }

    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
