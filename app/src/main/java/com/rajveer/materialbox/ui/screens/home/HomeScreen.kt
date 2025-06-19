package com.rajveer.materialbox.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.MaterialCard
import com.rajveer.materialbox.ui.components.SubjectCard
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val materials by viewModel.materials.collectAsState()
    var showDeleteSubjectDialog by remember { mutableStateOf<Subject?>(null) }

    val context = LocalContext.current

    val handleOpenFile: (Material) -> Unit = { mat ->
        if (mat.type == MaterialType.LINK) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mat.pathOrUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open link", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle internally stored files
            val file = File(context.filesDir, mat.pathOrUrl)
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri))
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "No application found to open this file", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "File not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MaterialBox") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddSubject.route) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(subjects) { subject ->
                SubjectCard(
                    subject = subject,
                    onClick = {
                        navController.navigate(Screen.SubjectDetail.createRoute(subject.id))
                    },
                    onLongPress = {
                        showDeleteSubjectDialog = subject
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Materials",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(materials) { material ->
                MaterialCard(
                    material = material,
                    onClick = {
                        if (material.type == MaterialType.NOTE) {
                            navController.navigate(Screen.MaterialDetail.createRoute(material.id))
                        } else {
                            handleOpenFile(material)
                            viewModel.incrementViewCount(material.id)
                        }
                    },
                    onLongPress = {}
                )
            }
        }

        if (showDeleteSubjectDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSubjectDialog = null },
                title = { Text("Delete Subject") },
                text = { Text("Are you sure you want to delete this subject and all its topics and materials?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteSubjectDialog?.let { viewModel.deleteSubject(it) }
                            showDeleteSubjectDialog = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSubjectDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}