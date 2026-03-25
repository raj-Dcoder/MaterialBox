package com.rajveer.materialbox.ui.screens.subjectdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.rajveer.materialbox.util.HapticUtils
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.data.entity.YoutubeFeed
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.ActionMenuBottomSheet
import com.rajveer.materialbox.ui.components.TopicCard
import com.rajveer.materialbox.ui.components.YoutubeFeedCard
import com.rajveer.materialbox.ui.screens.home.EmptyStateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    navController: NavController,
    viewModel: SubjectDetailViewModel = hiltViewModel(),
    subjectId: Long
) {
    val subject by viewModel.subject.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val youtubeFeeds by viewModel.youtubeFeeds.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActionTopic by remember { mutableStateOf<Topic?>(null) }
    var selectedActionYoutubeFeed by remember { mutableStateOf<YoutubeFeed?>(null) }
    var showEditTopicDialog by remember { mutableStateOf<Topic?>(null) }
    var showTopicDeleteDialog by remember { mutableStateOf<Topic?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = subject?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                            HapticUtils.playHeavyClick(context)
                        showDeleteDialog = true 
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (fabExpanded) {
                    // Add Youtube Feed
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                HapticUtils.playClick(context)
                                navController.navigate(Screen.AddYoutubeFeed.createRoute(subjectId))
                                fabExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Add Youtube Feed")
                        }
                    }

                    // Add Topic
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(250)),
                        exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(250))
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                HapticUtils.playClick(context)
                                navController.navigate(Screen.AddTopic.createRoute(subjectId))
                                fabExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Topic, contentDescription = "Add Topic")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        HapticUtils.playClick(context)
                        fabExpanded = !fabExpanded
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Expand menu",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        if (subject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Topics",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (topics.isEmpty() && youtubeFeeds.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Topic,
                            title = "No content yet",
                            subtitle = "Tap the + button to add a topic or YouTube feed"
                        )
                    }
                } else {
                    if (topics.isNotEmpty()) {
                        items(topics) { topic ->
                            val topicMaterialCount by viewModel.getMaterialCountForTopic(topic.id).collectAsState(initial = 0)
                            TopicCard(
                                topic = topic,
                                onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.id)) },
                                onLongPress = {
                                    selectedActionTopic = topic
                                },
                                materialCount = topicMaterialCount
                            )
                        }
                    }

                    if (youtubeFeeds.isNotEmpty()) {
                        item {
                            Text(
                                text = "YouTube Feeds",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }

                        items(youtubeFeeds) { feed ->
                            YoutubeFeedCard(
                                feed = feed,
                                onClick = { navController.navigate(Screen.YoutubeFeedDetail.createRoute(feed.id)) },
                                onLongPress = {
                                    selectedActionYoutubeFeed = feed
                                }
                            )
                        }
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Action Menu Bottom Sheet for Topics
        if (selectedActionTopic != null) {
            ActionMenuBottomSheet(
                title = selectedActionTopic?.name ?: "Options",
                onDismissRequest = { selectedActionTopic = null },
                onEditClick = { showEditTopicDialog = selectedActionTopic },
                onDeleteClick = { showTopicDeleteDialog = selectedActionTopic }
            )
        }

        // Action Menu Bottom Sheet for YouTube Feeds
        if (selectedActionYoutubeFeed != null) {
            ActionMenuBottomSheet(
                title = selectedActionYoutubeFeed?.name ?: "Options",
                onDismissRequest = { selectedActionYoutubeFeed = null },
                onEditClick = null, // Edit not implemented yet for feeds
                onDeleteClick = { 
                    HapticUtils.playHeavyClick(context)
                    selectedActionYoutubeFeed?.let { viewModel.deleteYoutubeFeed(it) }
                    selectedActionYoutubeFeed = null
                }
            )
        }

        // Edit topic dialog
        if (showEditTopicDialog != null) {
            var editedName by remember { mutableStateOf(showEditTopicDialog?.name ?: "") }
            AlertDialog(
                onDismissRequest = { showEditTopicDialog = null },
                title = { Text("Rename Topic") },
                text = {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Topic Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                showEditTopicDialog?.let { topic ->
                                    viewModel.updateTopic(topic.copy(name = editedName.trim()))
                                }
                                showEditTopicDialog = null
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTopicDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete topic dialog
        if (showTopicDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showTopicDeleteDialog = null },
                title = { Text("Delete Topic") },
                text = { Text("This will also delete all materials inside it.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            HapticUtils.playHeavyClick(context)
                            showTopicDeleteDialog?.let { viewModel.deleteTopic(it) }
                            showTopicDeleteDialog = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopicDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete subject dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Subject") },
                text = { Text("This will delete all topics and materials inside it.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSubject()
                            showDeleteDialog = false
                            navController.navigateUp()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
