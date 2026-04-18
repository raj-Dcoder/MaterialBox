package com.rajveer.materialbox.ui.screens.topicchecklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.TopicChecklistItem
import com.rajveer.materialbox.util.HapticUtils
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicChecklistScreen(
    navController: NavController,
    viewModel: TopicChecklistViewModel = hiltViewModel()
) {
    val topic by viewModel.topic.collectAsState()
    val items by viewModel.items.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val context = LocalContext.current
    var newItemText by remember { mutableStateOf("") }
    var parentForSubtask by remember { mutableStateOf<TopicChecklistItem?>(null) }
    var itemToDelete by remember { mutableStateOf<TopicChecklistItem?>(null) }
    var itemToEdit by remember { mutableStateOf<TopicChecklistItem?>(null) }
    var editItemText by remember { mutableStateOf("") }

    val state = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onDragMove(from.index, to.index) },
        canDragOver = { draggedOver, dragging ->
            val fromItem = items.getOrNull(dragging.index)?.item
            val toItem = items.getOrNull(draggedOver.index)?.item
            fromItem != null && toItem != null && fromItem.parentId == toItem.parentId
        },
        onDragEnd = { _, _ -> viewModel.onDragEnd() }
    )

    val totalItems = progress?.total ?: 0
    val completedItems = progress?.completed ?: 0
    val percent = if (totalItems > 0) completedItems.toFloat() / totalItems else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(topic?.name?.let { "$it Checklist" } ?: "Topic Checklist")
                        Text(
                            text = "Track topic progress with items and subtasks",
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
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.ime.asPaddingValues())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a checklist item") },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (newItemText.isNotBlank()) {
                                IconButton(onClick = {
                                    viewModel.addItem(newItemText)
                                    newItemText = ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Streamlined Progress Bar
            if (totalItems > 0) {
                LinearProgressIndicator(
                    progress = { percent },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$completedItems / $totalItems completed", style = MaterialTheme.typography.labelSmall)
                    Text("${(percent * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TopicChecklistEmptyState(
                        title = "This checklist is empty",
                        subtitle = "Add a parent item first, then split it into subtasks."
                    )
                }
            } else {
                LazyColumn(
                    state = state.listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .reorderable(state),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.item.id }) { listItem ->
                        ReorderableItem(state, key = listItem.item.id) { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 10.dp else 0.dp,
                                label = "topicChecklistElevation"
                            )

                            if (listItem.isChild) {
                                val siblings = items.filter { it.item.parentId == listItem.item.parentId }
                                val isLastChild = siblings.lastOrNull()?.item?.id == listItem.item.id

                                TopicChecklistChildRow(
                                    item = listItem.item,
                                    elevation = elevation,
                                    isLastChild = isLastChild,
                                    onToggle = {
                                        HapticUtils.playClick(context)
                                        viewModel.toggleItemCompletion(it)
                                    },
                                    onEdit = {
                                        HapticUtils.playClick(context)
                                        itemToEdit = it
                                        editItemText = it.text
                                    },
                                    onDelete = {
                                        HapticUtils.playHeavyClick(context)
                                        itemToDelete = it
                                    },
                                    dragModifier = Modifier.detectReorderAfterLongPress(state),
                                    modifier = Modifier.padding(start = 18.dp)
                                )
                            } else {
                                TopicChecklistParentRow(
                                    item = listItem.item,
                                    elevation = elevation,
                                    hasChildren = listItem.totalChildren > 0,
                                    totalChildren = listItem.totalChildren,
                                    completedChildren = listItem.completedChildren,
                                    isExpanded = !listItem.item.isCollapsed,
                                    onToggleExpand = {
                                        HapticUtils.playClick(context)
                                        viewModel.toggleParentExpansion(listItem.item.id)
                                    },
                                    onToggle = {
                                        HapticUtils.playClick(context)
                                        viewModel.toggleItemCompletion(it)
                                    },
                                    onEdit = {
                                        HapticUtils.playClick(context)
                                        itemToEdit = it
                                        editItemText = it.text
                                    },
                                    onDelete = {
                                        HapticUtils.playHeavyClick(context)
                                        itemToDelete = it
                                    },
                                    onAddSubtask = { parentForSubtask = listItem.item },
                                    dragModifier = Modifier.detectReorderAfterLongPress(state)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (parentForSubtask != null) {
            var subtaskText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { parentForSubtask = null },
                title = { Text("Add Subtask") },
                text = {
                    OutlinedTextField(
                        value = subtaskText,
                        onValueChange = { subtaskText = it },
                        label = { Text("Subtask description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (subtaskText.isNotBlank()) {
                                HapticUtils.playClick(context)
                                viewModel.addItem(subtaskText, parentId = parentForSubtask?.id)
                                parentForSubtask = null
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { parentForSubtask = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete task?") },
                text = { Text("Are you sure you want to delete '${itemToDelete?.text}'? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            itemToDelete?.let { viewModel.deleteItem(it) }
                            itemToDelete = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit dialog
        if (itemToEdit != null) {
            AlertDialog(
                onDismissRequest = { 
                    itemToEdit = null
                    editItemText = ""
                },
                title = { Text("Edit task") },
                text = {
                    OutlinedTextField(
                        value = editItemText,
                        onValueChange = { editItemText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editItemText.isNotBlank()) {
                                itemToEdit?.let { viewModel.editItem(it, editItemText) }
                                itemToEdit = null
                                editItemText = ""
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        itemToEdit = null
                        editItemText = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TopicChecklistOverviewCard(
    totalItems: Int,
    completedItems: Int,
    progress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Topic Progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% Completed",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$completedItems / $totalItems",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TopicMetricPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TopicChecklistEmptyState(
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TopicChecklistParentRow(
    item: TopicChecklistItem,
    elevation: Dp,
    hasChildren: Boolean,
    totalChildren: Int,
    completedChildren: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (TopicChecklistItem) -> Unit,
    onEdit: (TopicChecklistItem) -> Unit,
    onDelete: (TopicChecklistItem) -> Unit,
    onAddSubtask: () -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val containerColor = if (item.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (!item.isCompleted && !hasChildren) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { if (hasChildren) onToggleExpand() else onToggle(item) },
                onLongClick = { onDelete(item) }
            ).padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle(item) },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (hasChildren) {
                    Text(
                        text = "$completedChildren of $totalChildren done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Properly managing buttons in a compact row layout
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasChildren) {
                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onAddSubtask, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subtask", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Reorder",
                    modifier = dragModifier.padding(4.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TopicChecklistChildRow(
    item: TopicChecklistItem,
    elevation: Dp,
    isLastChild: Boolean,
    onToggle: (TopicChecklistItem) -> Unit,
    onEdit: (TopicChecklistItem) -> Unit,
    onDelete: (TopicChecklistItem) -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val outlineColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
        ) {
            val strokeWidth = 2.dp.toPx()
            val cornerRadius = 10.dp.toPx()
            val midY = size.height / 2f
            val startX = 8.dp.toPx()

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(startX, 0f)
                if (isLastChild) {
                    lineTo(startX, midY - cornerRadius)
                    quadraticTo(startX, midY, startX + cornerRadius, midY)
                    lineTo(size.width, midY)
                } else {
                    lineTo(startX, size.height)
                    moveTo(startX, midY)
                    lineTo(size.width, midY)
                }
            }
            drawPath(path = path, color = outlineColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (item.isCompleted) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = if (!item.isCompleted) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggle(item) },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onEdit(item) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    }
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Reorder",
                        modifier = dragModifier.padding(4.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
