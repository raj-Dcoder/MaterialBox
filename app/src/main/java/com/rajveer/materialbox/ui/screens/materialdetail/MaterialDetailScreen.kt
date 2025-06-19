package com.rajveer.materialbox.ui.screens.materialdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailScreen(
    navController: NavController,
    viewModel: MaterialDetailViewModel = hiltViewModel(),
    materialId: Long
) {
    val material by viewModel.material.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editedContent by remember { mutableStateOf("") }

    val context = LocalContext.current

    LaunchedEffect(material, isLoading) {
        val currentMaterial = material
        if (!isLoading && currentMaterial == null) {
            navController.navigateUp()
        } else if (currentMaterial != null && !isEditing) {
            editedTitle = currentMaterial.title
            editedContent = currentMaterial.pathOrUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = material?.title ?: "",
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
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.updateMaterial(
                                    title = editedTitle,
                                    content = editedContent
                                ) {
                                    isEditing = false
                                }
                            },
                            enabled = editedTitle.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (material != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (material?.type) {
                    MaterialType.NOTE -> {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Title") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editedContent,
                                onValueChange = { editedContent = it },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                label = { Text("Content") }
                            )
                        } else {
                            Text(
                                text = material?.title ?: "",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = material?.pathOrUrl ?: "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editedContent,
                                onValueChange = { editedContent = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Path or URL") },
                                singleLine = true
                            )
                        } else {
                            Text(material?.pathOrUrl ?: "No file/link", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Material") },
                text = { Text("Are you sure you want to delete this material?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMaterial()
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