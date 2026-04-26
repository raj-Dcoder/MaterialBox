package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.DailyTaskDao
import com.rajveer.materialbox.data.dao.DailyTaskProgress
import com.rajveer.materialbox.data.entity.DailyTask
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyTaskRepository @Inject constructor(
    private val dailyTaskDao: DailyTaskDao
) {
    fun getTasksForDate(plannedDate: String): Flow<List<DailyTask>> =
        dailyTaskDao.getTasksForDate(plannedDate)

    fun getUnfinishedBefore(today: String): Flow<List<DailyTask>> =
        dailyTaskDao.getUnfinishedBefore(today)

    fun getHistoryBefore(today: String): Flow<List<DailyTask>> =
        dailyTaskDao.getHistoryBefore(today)

    fun getProgressForDate(plannedDate: String): Flow<DailyTaskProgress> =
        dailyTaskDao.getProgressForDate(plannedDate)

    suspend fun getTaskCountForDate(plannedDate: String): Int =
        dailyTaskDao.getTaskCountForDate(plannedDate)

    suspend fun insertTask(task: DailyTask): Long =
        dailyTaskDao.insertTask(task)

    suspend fun updateTask(task: DailyTask) =
        dailyTaskDao.updateTask(task)

    suspend fun updateTasks(tasks: List<DailyTask>) =
        dailyTaskDao.updateTasks(tasks)

    suspend fun deleteTask(task: DailyTask) =
        dailyTaskDao.deleteTask(task)
}
