package com.rajveer.materialbox.ui.screens.roadmap

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.RoadmapItem
import com.rajveer.materialbox.util.HapticUtils
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    navController: NavController,
    subjectId: Long,
    viewModel: RoadmapViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val collapsedParentIds by viewModel.collapsedParentIds.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val context = LocalContext.current
    var newItemText by remember { mutableStateOf("") }
    var parentForSubGoal by remember { mutableStateOf<RoadmapItem?>(null) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.onDragMove(from.index, to.index) },
        canDragOver = { draggedOver, dragging -> 
            val fromItem = items.getOrNull(dragging.index)?.item
            val toItem = items.getOrNull(draggedOver.index)?.item
            fromItem != null && toItem != null && fromItem.parentId == toItem.parentId
        },
        onDragEnd = { _, _ -> viewModel.onDragEnd() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Roadmap") },
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
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.ime.asPaddingValues())
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a main study goal...") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                HapticUtils.playClick(context)
                                viewModel.addItem(newItemText)
                                newItemText = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress Header
            if (progress != null && progress!!.total > 0) {
                val percent = progress!!.completed.toFloat() / progress!!.total
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Completion",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${progress!!.completed} / ${progress!!.total} (${(percent * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val animatedPercent by animateFloatAsState(
                        targetValue = percent,
                        animationSpec = androidx.compose.animation.core.tween(500),
                        label = "progressAnimation"
                    )
                    LinearProgressIndicator(
                        progress = { animatedPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            } else if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your roadmap is empty.\nAdd your first study goal below!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Checklist
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .reorderable(state),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.item.id }) { listItem ->
                    ReorderableItem(state, key = listItem.item.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
                        
                        if (listItem.isChild) {
                            val siblings = items.filter { it.item.parentId == listItem.item.parentId }
                            val isLastChild = siblings.lastOrNull()?.item?.id == listItem.item.id

                            RoadmapChildRow(
                                item = listItem.item,
                                elevation = elevation,
                                isLastChild = isLastChild,
                                onToggle = { 
                                    HapticUtils.playClick(context)
                                    viewModel.toggleItemCompletion(it) 
                                },
                                onDelete = { 
                                    HapticUtils.playHeavyClick(context)
                                    viewModel.deleteItem(it) 
                                },
                                dragModifier = Modifier.detectReorderAfterLongPress(state),
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        } else {
                            val isExpanded = !collapsedParentIds.contains(listItem.item.id)

                            RoadmapParentRow(
                                item = listItem.item,
                                elevation = elevation,
                                hasChildren = listItem.totalChildren > 0,
                                totalChildren = listItem.totalChildren,
                                completedChildren = listItem.completedChildren,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    HapticUtils.playClick(context)
                                    viewModel.toggleParentExpansion(listItem.item.id)
                                },
                                onToggle = { 
                                    HapticUtils.playClick(context)
                                    viewModel.toggleItemCompletion(it) 
                                },
                                onDelete = { 
                                    HapticUtils.playHeavyClick(context)
                                    viewModel.deleteItem(it) 
                                },
                                onAddSubGoal = { parentForSubGoal = listItem.item },
                                dragModifier = Modifier.detectReorderAfterLongPress(state)
                            )
                        }
                    }
                }
            }
        }

        // Add Sub-goal dialog
        if (parentForSubGoal != null) {
            var subGoalText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { parentForSubGoal = null },
                title = { Text("Add Sub-goal") },
                text = {
                    OutlinedTextField(
                        value = subGoalText,
                        onValueChange = { subGoalText = it },
                        label = { Text("Sub-goal description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (subGoalText.isNotBlank()) {
                                HapticUtils.playClick(context)
                                viewModel.addItem(subGoalText, parentId = parentForSubGoal?.id)
                                parentForSubGoal = null
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { parentForSubGoal = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RoadmapParentRow(
    item: RoadmapItem,
    elevation: androidx.compose.ui.unit.Dp,
    hasChildren: Boolean,
    totalChildren: Int = 0,
    completedChildren: Int = 0,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggle: (RoadmapItem) -> Unit,
    onDelete: (RoadmapItem) -> Unit,
    onAddSubGoal: () -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onToggleExpand() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isCompleted) 0.dp else elevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle(item) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = item.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    fontWeight = FontWeight.Bold
                ),
                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            
            if (hasChildren) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$completedChildren/$totalChildren",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Expand",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Add sub-goal
            IconButton(onClick = onAddSubGoal) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Sub-goal",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Delete
            if (!hasChildren) {
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Section",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Drag Handle
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = dragModifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun RoadmapChildRow(
    item: RoadmapItem,
    elevation: androidx.compose.ui.unit.Dp,
    isLastChild: Boolean = false,
    onToggle: (RoadmapItem) -> Unit,
    onDelete: (RoadmapItem) -> Unit,
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = elevation,
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual tree connector
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
            ) {
                val strokeWidth = 2.dp.toPx()
                val cornerRadius = 8.dp.toPx()
                val midY = size.height / 2f
                val startX = strokeWidth / 2f
                
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(startX, 0f)
                    if (isLastChild) {
                        // └ shape
                        lineTo(startX, midY - cornerRadius)
                        quadraticBezierTo(startX, midY, startX + cornerRadius, midY)
                        lineTo(size.width, midY)
                    } else {
                        // ├ shape
                        lineTo(startX, size.height)
                        moveTo(startX, midY)
                        lineTo(size.width, midY)
                    }
                }
                drawPath(path = path, color = outlineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggle(item) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                    ),
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Drag Handle
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = dragModifier.padding(4.dp).size(20.dp)
                )
            }
        }
    }
}
