package com.rajveer.materialbox.ui.screens.dayplan

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.DailyTask
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.util.HapticUtils
import com.rajveer.materialbox.util.toDayPlanHeader
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlanScreen(
    navController: NavController,
    viewModel: DayPlanViewModel = hiltViewModel()
) {
    val todayTasks by viewModel.todayTasks.collectAsState()
    val unfinishedTasks by viewModel.unfinishedTasks.collectAsState()
    val historyGroups by viewModel.historyGroups.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var newTaskText by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<DailyTask?>(null) }
    var editTaskText by remember { mutableStateOf("") }
    var editSubjectId by remember { mutableStateOf<Long?>(null) }
    var taskToDelete by remember { mutableStateOf<DailyTask?>(null) }

    val completed = progress?.completed ?: 0
    val total = progress?.total ?: 0
    val state = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onDragMove(from.index - 1, to.index - 1) },
        canDragOver = { draggedOver, dragging ->
            val fromIndex = dragging.index - 1
            val toIndex = draggedOver.index - 1
            fromIndex in todayTasks.indices && toIndex in todayTasks.indices
        },
        onDragEnd = { _, _ -> viewModel.onDragEnd() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Day Plan")
                        Text(
                            text = if (total == 0) "Plan today with a clean slate" else "$completed / $total done today",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    HapticUtils.playClick(context)
                    showAddTaskDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .reorderable(state),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DayPlanSectionHeader(
                    icon = Icons.Default.Today,
                    title = "Today",
                    subtitle = if (total == 0) "Add a few focused tasks for the day." else "$completed of $total completed"
                )
            }

            if (todayTasks.isEmpty()) {
                item {
                    DayPlanEmptyState()
                }
            } else {
                items(todayTasks, key = { it.task.id }) { item ->
                    ReorderableItem(state, key = item.task.id) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 10.dp else 0.dp,
                            label = "dailyTaskElevation"
                        )
                        DailyTaskRow(
                            item = item,
                            elevation = elevation,
                            onToggle = {
                                HapticUtils.playClick(context)
                                viewModel.toggleCompletion(it)
                            },
                            onEdit = {
                                HapticUtils.playClick(context)
                                taskToEdit = it
                                editTaskText = it.title
                                editSubjectId = it.subjectId
                            },
                            onDelete = {
                                HapticUtils.playHeavyClick(context)
                                taskToDelete = it
                            },
                            dragModifier = Modifier.detectReorderAfterLongPress(state)
                        )
                    }
                }
            }

            if (unfinishedTasks.isNotEmpty()) {
                item {
                    DayPlanSectionHeader(
                        icon = Icons.Default.Inbox,
                        title = "Unfinished",
                        subtitle = "Review older pending tasks when they still matter."
                    )
                }
                items(unfinishedTasks, key = { "unfinished-${it.task.id}" }) { item ->
                    UnfinishedTaskCard(
                        item = item,
                        onMoveToToday = {
                            HapticUtils.playClick(context)
                            viewModel.moveToToday(item.task)
                        },
                        onMarkDone = {
                            HapticUtils.playClick(context)
                            viewModel.markDone(item.task)
                        },
                        onDelete = {
                            HapticUtils.playHeavyClick(context)
                            taskToDelete = item.task
                        }
                    )
                }
            }

            if (historyGroups.isNotEmpty()) {
                item {
                    DayPlanSectionHeader(
                        icon = Icons.Default.History,
                        title = "History",
                        subtitle = "A quiet record of what you planned and finished."
                    )
                }
                historyGroups.forEach { group ->
                    item(key = "history-${group.plannedDate}") {
                        HistoryGroupCard(group = group)
                    }
                }
            }
        }

        if (showAddTaskDialog) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            AlertDialog(
                onDismissRequest = { showAddTaskDialog = false },
                title = { Text("Add today's task") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            label = { Text("Task") },
                            singleLine = true
                        )
                        SubjectPicker(
                            subjects = subjects,
                            selectedSubjectId = selectedSubjectId,
                            onSelected = { selectedSubjectId = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                HapticUtils.playClick(context)
                                viewModel.addTask(newTaskText, selectedSubjectId)
                                newTaskText = ""
                                showAddTaskDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTaskDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (taskToEdit != null) {
            AlertDialog(
                onDismissRequest = {
                    taskToEdit = null
                    editTaskText = ""
                },
                title = { Text("Edit task") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = editTaskText,
                            onValueChange = { editTaskText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Task") },
                            singleLine = true
                        )
                        SubjectPicker(
                            subjects = subjects,
                            selectedSubjectId = editSubjectId,
                            onSelected = { editSubjectId = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            taskToEdit?.let { viewModel.editTask(it, editTaskText, editSubjectId) }
                            taskToEdit = null
                            editTaskText = ""
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            taskToEdit = null
                            editTaskText = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (taskToDelete != null) {
            AlertDialog(
                onDismissRequest = { taskToDelete = null },
                title = { Text("Delete task?") },
                text = { Text("This removes '${taskToDelete?.title}' from your day plan.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            taskToDelete?.let { viewModel.deleteTask(it) }
                            taskToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DayPlanSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp).size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DayPlanEmptyState() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No tasks for today",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Keep it light. Add what you actually want to finish today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun DailyTaskRow(
    item: DayPlanTaskUi,
    elevation: Dp,
    onToggle: (DailyTask) -> Unit,
    onEdit: (DailyTask) -> Unit,
    onDelete: (DailyTask) -> Unit,
    dragModifier: Modifier
) {
    val task = item.task
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (task.isCompleted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle(task) },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { onToggle(task) },
                        onLongClick = { onDelete(task) }
                    )
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                SubjectLabel(item.subjectName)
            }
            IconButton(onClick = { onEdit(task) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(19.dp))
            }
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Reorder",
                modifier = dragModifier.padding(4.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun UnfinishedTaskCard(
    item: DayPlanTaskUi,
    onMoveToToday: () -> Unit,
    onMarkDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.task.plannedDate.toDayPlanHeader(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SubjectLabel(item.subjectName)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMoveToToday) {
                    Text("Move to Today")
                }
                TextButton(onClick = onMarkDone) {
                    Text("Mark Done")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryGroupCard(group: DayPlanHistoryGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val done = group.tasks.count { it.task.isCompleted }
            Text(
                text = "${group.plannedDate.toDayPlanHeader()} · $done/${group.tasks.size} done",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            group.tasks.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (item.task.isCompleted) Icons.Default.CheckCircle else Icons.Default.EventNote,
                        contentDescription = null,
                        tint = if (item.task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (item.task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (item.task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        SubjectLabel(item.subjectName)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectLabel(subjectName: String?) {
    if (subjectName == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = subjectName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubjectPicker(
    subjects: List<Subject>,
    selectedSubjectId: Long?,
    onSelected: (Long?) -> Unit,
    compact: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = subjects.firstOrNull { it.id == selectedSubjectId }?.name

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = {
                Text(
                    text = selectedName ?: "No subject",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = if (compact) Modifier.height(IntrinsicSize.Min) else Modifier
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("No subject") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name) },
                    onClick = {
                        onSelected(subject.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
