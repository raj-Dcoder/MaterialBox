package com.rajveer.materialbox.ui.screens.topicdetail

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.MaterialCard
import com.rajveer.materialbox.ui.theme.Red400
import com.rajveer.materialbox.ui.theme.Blue400
import android.content.Context
import android.provider.OpenableColumns
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.rajveer.materialbox.data.entity.Material
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    navController: NavController,
    viewModel: TopicDetailViewModel = hiltViewModel(),
    topicId: Long
) {
    val topic by viewModel.topic.collectAsState()
    val materials by viewModel.materials.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteMaterialDialog by remember { mutableStateOf<Material?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val handleOpenFile: (Material) -> Unit = { mat ->
        if (mat.type == MaterialType.LINK) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mat.pathOrUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle internally stored files
            val file = File(context.filesDir, mat.pathOrUrl)
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No application found to open this file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Take persistable URI permission to access the file across app restarts
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)

                val fileName = getFileName(context, it) ?: "Untitled"
                val fileExtension = getFileExtension(context, it)
                val inferredType = when (fileExtension?.lowercase()) {
                    "pdf" -> MaterialType.PDF
                    "docx" -> MaterialType.DOCX
                    "txt" -> MaterialType.TXT
                    "jpg", "jpeg", "png" -> MaterialType.IMAGE
                    else -> {
                        // Attempt to infer from mime type for more robustness
                        val mimeType = context.contentResolver.getType(it)
                        when {
                            mimeType?.startsWith("image/") == true -> MaterialType.IMAGE
                            mimeType == "application/pdf" -> MaterialType.PDF
                            mimeType == "application/msword" || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> MaterialType.DOCX
                            mimeType?.startsWith("text/") == true -> MaterialType.TXT
                            else -> MaterialType.NOTE // Fallback for unsupported types
                        }
                    }
                }

                if (inferredType != MaterialType.NOTE) {
                    viewModel.saveDocumentMaterial(
                        uri = it,
                        title = fileName,
                        type = inferredType,
                        onSuccess = {
                            Toast.makeText(context, "'$fileName' added successfully.", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "This file type is not supported.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(context, "Error: Could not get permission for the file.", Toast.LENGTH_LONG).show()
            }
        }
    }

//    LaunchedEffect(topic) {
//        if (topic == null) {
//            navController.navigateUp()
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topic?.name ?: "",
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
                        Icon(Icons.Default.Delete, contentDescription = "Delete Topic")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
            ) {
                if (fabExpanded) {
                    // Document Upload option
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(300))
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                topic?.let { 
                                    pickDocumentLauncher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"))
                                }
                            },
                            containerColor = Red400,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Description, contentDescription = "Upload Document")
                        }
                    }

                    // Text option
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 50)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = 50)),
                        exit = fadeOut(animationSpec = tween(300, delayMillis = 50)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = 50))
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                topic?.let {
                                    navController.navigate(Screen.AddMaterial.createRoute(it.id))
                                }
                                fabExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.TextSnippet, contentDescription = "Add Text Material")
                        }
                    }

                    // Add Link option
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = 100)),
                        exit = fadeOut(animationSpec = tween(300, delayMillis = 100)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(300, delayMillis = 100))
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                topic?.let {
                                    navController.navigate(Screen.AddMaterial.createRoute(it.id, MaterialType.LINK.name, ""))
                                }
                                fabExpanded = false
                            },
                            containerColor = Blue400,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Add Link")
                        }
                    }
                }

                // Main Floating Action Button
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    modifier = Modifier.zIndex(1f) // Ensure main FAB is always on top
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Close options" else "Add Material"
                    )
                }
            }
        }
    ) { padding ->
        if (topic != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Materials",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
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
                        onLongPress = {
                            showDeleteMaterialDialog = material
                        },
                        onDelete = {
                            showDeleteMaterialDialog = material
                        }
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Topic") },
                text = { Text("Are you sure you want to delete this topic and all its materials?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTopic()
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

        if (showDeleteMaterialDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteMaterialDialog = null },
                title = { Text("Delete Material") },
                text = { Text("Are you sure you want to delete this material?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteMaterialDialog?.let { viewModel.deleteMaterial(it) }
                            showDeleteMaterialDialog = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMaterialDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    result = it.getString(displayNameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result
}

fun getFileExtension(context: Context, uri: Uri): String? {
    val mimeType = context.contentResolver.getType(uri)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
} 