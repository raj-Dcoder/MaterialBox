package com.rajveer.materialbox.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rajveer.materialbox.data.entity.DailyTask
import kotlinx.coroutines.flow.Flow

data class DailyTaskProgress(
    val completed: Int,
    val total: Int
)

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks WHERE plannedDate = :plannedDate ORDER BY isCompleted ASC, position ASC, createdAt ASC")
    fun getTasksForDate(plannedDate: String): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE plannedDate < :today AND isCompleted = 0 ORDER BY plannedDate DESC, position ASC, createdAt ASC")
    fun getUnfinishedBefore(today: String): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks WHERE plannedDate < :today ORDER BY plannedDate DESC, position ASC, createdAt ASC")
    fun getHistoryBefore(today: String): Flow<List<DailyTask>>

    @Query(
        "SELECT " +
            "SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed, " +
            "COUNT(*) as total " +
            "FROM daily_tasks WHERE plannedDate = :plannedDate"
    )
    fun getProgressForDate(plannedDate: String): Flow<DailyTaskProgress>

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE plannedDate = :plannedDate")
    suspend fun getTaskCountForDate(plannedDate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask): Long

    @Update
    suspend fun updateTask(task: DailyTask)

    @Update
    suspend fun updateTasks(tasks: List<DailyTask>)

    @Delete
    suspend fun deleteTask(task: DailyTask)
}
