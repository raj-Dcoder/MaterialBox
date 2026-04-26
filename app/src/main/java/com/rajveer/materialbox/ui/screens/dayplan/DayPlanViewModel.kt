package com.rajveer.materialbox.ui.screens.dayplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajveer.materialbox.data.dao.DailyTaskProgress
import com.rajveer.materialbox.data.entity.DailyTask
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.data.repository.DailyTaskRepository
import com.rajveer.materialbox.data.repository.SubjectRepository
import com.rajveer.materialbox.util.todayDayKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class DayPlanTaskUi(
    val task: DailyTask,
    val subjectName: String? = null
)

data class DayPlanHistoryGroup(
    val plannedDate: String,
    val tasks: List<DayPlanTaskUi>
)

@HiltViewModel
class DayPlanViewModel @Inject constructor(
    private val dailyTaskRepository: DailyTaskRepository,
    subjectRepository: SubjectRepository
) : ViewModel() {

    private val today = todayDayKey()
    private val _todayTaskItems = MutableStateFlow<List<DailyTask>>(emptyList())

    val subjects: StateFlow<List<Subject>> = subjectRepository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTasks: StateFlow<List<DayPlanTaskUi>> = combine(
        _todayTaskItems.asStateFlow(),
        subjects
    ) { tasks, subjects ->
        tasks.map { it.toUi(subjects) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            dailyTaskRepository.getTasksForDate(today).collect { tasks ->
                _todayTaskItems.value = tasks
            }
        }
    }

    val progress: StateFlow<DailyTaskProgress?> = dailyTaskRepository.getProgressForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unfinishedTasks: StateFlow<List<DayPlanTaskUi>> = combine(
        dailyTaskRepository.getUnfinishedBefore(today),
        subjects
    ) { tasks, subjects ->
        tasks.map { it.toUi(subjects) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyGroups: StateFlow<List<DayPlanHistoryGroup>> = combine(
        dailyTaskRepository.getHistoryBefore(today),
        subjects
    ) { tasks, subjects ->
        tasks
            .map { it.toUi(subjects) }
            .groupBy { it.task.plannedDate }
            .map { (date, groupTasks) -> DayPlanHistoryGroup(date, groupTasks) }
            .sortedByDescending { it.plannedDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTask(title: String, subjectId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val position = dailyTaskRepository.getTaskCountForDate(today)
            dailyTaskRepository.insertTask(
                DailyTask(
                    title = trimmed,
                    plannedDate = today,
                    subjectId = subjectId,
                    position = position
                )
            )
        }
    }

    fun editTask(task: DailyTask, title: String, subjectId: Long?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            dailyTaskRepository.updateTask(
                task.copy(
                    title = trimmed,
                    subjectId = subjectId,
                    updatedAt = Date()
                )
            )
        }
    }

    fun toggleCompletion(task: DailyTask) {
        viewModelScope.launch {
            val completed = !task.isCompleted
            dailyTaskRepository.updateTask(
                task.copy(
                    isCompleted = completed,
                    completedAt = if (completed) Date() else null,
                    updatedAt = Date()
                )
            )
        }
    }

    fun markDone(task: DailyTask) {
        if (task.isCompleted) return
        viewModelScope.launch {
            dailyTaskRepository.updateTask(
                task.copy(
                    isCompleted = true,
                    completedAt = Date(),
                    updatedAt = Date()
                )
            )
        }
    }

    fun moveToToday(task: DailyTask) {
        viewModelScope.launch {
            val position = dailyTaskRepository.getTaskCountForDate(today)
            dailyTaskRepository.updateTask(
                task.copy(
                    plannedDate = today,
                    isCompleted = false,
                    completedAt = null,
                    position = position,
                    updatedAt = Date()
                )
            )
        }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch {
            dailyTaskRepository.deleteTask(task)
        }
    }

    fun onDragMove(fromIndex: Int, toIndex: Int) {
        val list = _todayTaskItems.value.toMutableList()
        val moved = list.getOrNull(fromIndex) ?: return
        if (toIndex !in list.indices) return

        list.removeAt(fromIndex)
        list.add(toIndex, moved)
        _todayTaskItems.value = list
    }

    fun onDragEnd() {
        viewModelScope.launch {
            val updates = _todayTaskItems.value.mapIndexedNotNull { index, task ->
                if (task.position != index) {
                    task.copy(position = index, updatedAt = Date())
                } else {
                    null
                }
            }
            if (updates.isNotEmpty()) {
                dailyTaskRepository.updateTasks(updates)
            }
        }
    }

    private fun DailyTask.toUi(subjects: List<Subject>): DayPlanTaskUi {
        val subjectName = subjectId?.let { id -> subjects.firstOrNull { it.id == id }?.name }
        return DayPlanTaskUi(task = this, subjectName = subjectName)
    }
}
