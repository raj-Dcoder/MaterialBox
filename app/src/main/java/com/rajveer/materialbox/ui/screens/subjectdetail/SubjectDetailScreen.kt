package com.rajveer.materialbox.ui.screens.subjectdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.Topic
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.TopicCard
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTopicDeleteDialog by remember { mutableStateOf(false) }
    var topicToDelete by remember { mutableStateOf<Topic?>(null) }

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
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject")
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
                    subject?.let {
                        navController.navigate(Screen.AddTopic.createRoute(it.id))
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic",
                    tint = MaterialTheme.colorScheme.onPrimary)
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

                if (topics.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Topic,
                            title = "No topics yet",
                            subtitle = "Tap the + button to add your first topic"
                        )
                    }
                } else {
                    items(topics) { topic ->
                        TopicCard(
                            topic = topic,
                            onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.id)) },
                            onLongPress = {
                                topicToDelete = topic
                                showTopicDeleteDialog = true
                            }
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Delete topic dialog
        if (showTopicDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showTopicDeleteDialog = false },
                title = { Text("Delete Topic") },
                text = { Text("This will also delete all materials inside it.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            topicToDelete?.let { viewModel.deleteTopic(it) }
                            showTopicDeleteDialog = false
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopicDeleteDialog = false }) {
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
