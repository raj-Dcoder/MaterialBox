package com.rajveer.materialbox.ui.screens.subjectdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.TopicCard

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

  /*  LaunchedEffect(subject) {
        if (subject == null) {
            navController.navigateUp()
        }
    }*/

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    subject?.let { 
                        navController.navigate(Screen.AddTopic.createRoute(it.id))
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic")
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
        } else{
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Topics",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                items(topics) { topic ->
                    TopicCard(
                        topic = topic,
                        onClick = { navController.navigate(Screen.TopicDetail.createRoute(topic.id)) },
                        onDelete = { viewModel.deleteTopic(topic) }
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Subject") },
                text = { Text("Are you sure you want to delete this subject and all its topics?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSubject()
                            showDeleteDialog = false
                            navController.navigateUp()
                        }
                    ) {
                        Text("Delete")
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