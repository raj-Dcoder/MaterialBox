package com.rajveer.materialbox.ui.screens.subjectdetail

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.rajveer.materialbox.util.HapticUtils
import androidx.compose.ui.Modifier
import com.rajveer.materialbox.ui.components.MaterialCard
import com.rajveer.materialbox.ui.theme.MaterialPdfColor
import androidx.compose.ui.text.font.FontWeight
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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    navController: NavController,
    viewModel: SubjectDetailViewModel = hiltViewModel(),
    subjectId: Long
) {
    val subject by viewModel.subject.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val materials by viewModel.materials.collectAsState()
    val fabMode by viewModel.fabMode.collectAsState()
    val youtubeFeeds by viewModel.youtubeFeeds.collectAsState()
    val roadmapItemCount by viewModel.getRoadmapItemCount().collectAsState(initial = 0)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActionTopic by remember { mutableStateOf<Topic?>(null) }
    var selectedActionYoutubeFeed by remember { mutableStateOf<YoutubeFeed?>(null) }
    var showEditTopicDialog by remember { mutableStateOf<Topic?>(null) }
    var showTopicDeleteDialog by remember { mutableStateOf<Topic?>(null) }
    var selectedActionMaterial by remember { mutableStateOf<Material?>(null) }
    var showDeleteMaterialDialog by remember { mutableStateOf<Material?>(null) }
    var showEditMaterialDialog by remember { mutableStateOf<Material?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scannerOptions = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()
    val scanner = GmsDocumentScanning.getClient(scannerOptions)

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val fileName = "Scanned_${System.currentTimeMillis()}.pdf"
                viewModel.saveDocumentMaterials(listOf(DocumentInfo(pdf.uri, fileName, MaterialType.PDF))) { added, skipped ->
                    if (added > 0) Toast.makeText(context, "Scanned document saved", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        
        val docsToSave = mutableListOf<DocumentInfo>()
        uris.forEach { uri ->
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)

                val fileName = getFileName(context, uri) ?: "Untitled"
                val fileExtension = getFileExtension(context, uri)
                val inferredType = when (fileExtension?.lowercase()) {
                    "pdf" -> MaterialType.PDF
                    "docx" -> MaterialType.DOCX
                    "txt" -> MaterialType.TXT
                    "jpg", "jpeg", "png", "webp" -> MaterialType.IMAGE
                    else -> {
                        val mimeType = context.contentResolver.getType(uri)
                        when {
                            mimeType?.startsWith("image/") == true -> MaterialType.IMAGE
                            mimeType == "application/pdf" -> MaterialType.PDF
                            mimeType == "application/msword" || mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> MaterialType.DOCX
                            mimeType?.startsWith("text/") == true -> MaterialType.TXT
                            else -> MaterialType.NOTE
                        }
                    }
                }

                if (inferredType != MaterialType.NOTE) {
                    docsToSave.add(DocumentInfo(uri, fileName, inferredType))
                } else {
                    Toast.makeText(context, "File type not supported for: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(context, "Error: Could not get permission for a file.", Toast.LENGTH_LONG).show()
            }
        }

        if (docsToSave.isNotEmpty()) {
            viewModel.saveDocumentMaterials(docsToSave) { added, skipped ->
                if (added > 0 && skipped == 0) {
                    Toast.makeText(context, "$added document(s) added successfully.", Toast.LENGTH_SHORT).show()
                } else if (added > 0 && skipped > 0) {
                    Toast.makeText(context, "$added added, $skipped skipped (duplicate).", Toast.LENGTH_LONG).show()
                } else if (added == 0 && skipped > 0) {
                    Toast.makeText(context, "$skipped duplicate document(s) skipped.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val handleOpenFile: (Material) -> Unit = { mat ->
        if (mat.type == MaterialType.LINK) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mat.pathOrUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        } else {
            val isContentUri = mat.pathOrUrl.startsWith("content://") || mat.pathOrUrl.startsWith("file://")
            if (isContentUri) {
                try {
                    val uri = Uri.parse(mat.pathOrUrl)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                }
            } else {
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
    }

    Scaffold(
        topBar = {
            Column {
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
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (fabExpanded) {
                    if (fabMode == FabMode.NORMAL) {
                        // Add Youtube Feed
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "YouTube feed",
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

                        // Add Roadmap
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(150)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(150)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Roadmap",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    navController.navigate(Screen.Roadmap.createRoute(subjectId))
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Study Roadmap")
                            }
                        }

                        // Add Topic
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(250)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Topic",
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

                        // Material Mode Toggle
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Materials",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    viewModel.setFabMode(FabMode.MATERIALS)
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Material Options")
                            }
                        }
                    } else {
                        // MATERIALS MODE
                        // Scan document
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Scan",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    context.findActivity()?.let { activity ->
                                        scanner.getStartScanIntent(activity)
                                            .addOnSuccessListener { intentSender ->
                                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Failed to open scanner", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Scan Document")
                            }
                        }

                        // Document upload
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Upload",
                                onClick = { 
                                    HapticUtils.playClick(context)
                                    pickDocumentLauncher.launch(arrayOf("*/*")) 
                                },
                                containerColor = MaterialPdfColor.copy(alpha = 0.15f),
                                contentColor = MaterialPdfColor
                            ) {
                                Icon(Icons.Default.Description, contentDescription = "Upload Document")
                            }
                        }

                        // Text note
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Note",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    navController.navigate(Screen.AddMaterialSubject.createRoute(subjectId))
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = "Add Text Material")
                            }
                        }

                        // Add Link
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Link",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    navController.navigate(Screen.AddMaterialSubject.createRoute(subjectId, "LINK", ""))
                                    fabExpanded = false
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Add Link")
                            }
                        }

                        // Back to Normal Mode
                        AnimatedVisibility(
                            visible = fabExpanded,
                            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200)),
                            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                        ) {
                            LabeledSmallFab(
                                label = "Back",
                                onClick = {
                                    HapticUtils.playClick(context)
                                    viewModel.setFabMode(FabMode.NORMAL)
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        HapticUtils.playClick(context)
                        fabExpanded = !fabExpanded
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (fabMode == FabMode.MATERIALS) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        if (fabExpanded) Icons.Default.Close else if (fabMode == FabMode.MATERIALS) Icons.Default.Description else Icons.Default.Add,
                        contentDescription = "Expand menu",
                        tint = if (fabMode == FabMode.MATERIALS) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        // Roadmap entry card
                        val showRoadmapTitle = roadmapItemCount > 0
                        item {
                            Card(
                                onClick = {
                                    HapticUtils.playClick(context)
                                    navController.navigate(Screen.Roadmap.createRoute(subjectId))
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Study Roadmap",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (showRoadmapTitle) {
                                            Text(
                                                text = "$roadmapItemCount goal${if (roadmapItemCount == 1) "" else "s"} set",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (topics.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Topics",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

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

                        if (materials.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Materials",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }

                            items(materials) { material ->
                                MaterialCard(
                                    material = material,
                                    onClick = {
                                        HapticUtils.playClick(context)
                                        if (material.type == MaterialType.NOTE) {
                                            navController.navigate(Screen.MaterialDetail.createRoute(material.id))
                                        } else {
                                            viewModel.incrementViewCount(material.id)
                                            handleOpenFile(material)
                                        }
                                    },
                                    onLongPress = {
                                        selectedActionMaterial = material
                                    }
                                )
                            }
                        }

                        if (topics.isEmpty() && youtubeFeeds.isEmpty() && materials.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Outlined.Topic,
                                    title = "No content yet",
                                    subtitle = "Tap the + button to add a topic or YouTube feed"
                                )
                            }
                        }

                        // Bottom spacer for FAB
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
            }
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

    if (selectedActionMaterial != null) {
        ActionMenuBottomSheet(
            title = selectedActionMaterial?.title ?: "Options",
            onDismissRequest = { selectedActionMaterial = null },
            onEditClick = { showEditMaterialDialog = selectedActionMaterial },
            onDeleteClick = { showDeleteMaterialDialog = selectedActionMaterial }
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
                        HapticUtils.playHeavyClick(context)
                        showDeleteMaterialDialog?.let { viewModel.deleteMaterial(it) }
                        showDeleteMaterialDialog = null
                        selectedActionMaterial = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteMaterialDialog = null
                    selectedActionMaterial = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditMaterialDialog != null) {
        val materialToEdit = showEditMaterialDialog!!
        var editedTitle by remember { mutableStateOf(materialToEdit.title) }
        var editedUrl by remember { mutableStateOf(materialToEdit.pathOrUrl) }

        AlertDialog(
            onDismissRequest = { showEditMaterialDialog = null },
            title = { Text("Edit Material") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (materialToEdit.type == MaterialType.LINK) {
                        OutlinedTextField(
                            value = editedUrl,
                            onValueChange = { editedUrl = it },
                            label = { Text("URL Link") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedTitle.isNotBlank()) {
                            viewModel.updateMaterial(
                                materialToEdit.copy(
                                    title = editedTitle.trim(),
                                    pathOrUrl = if (materialToEdit.type == MaterialType.LINK) editedUrl.trim() else materialToEdit.pathOrUrl
                                )
                            )
                            showEditMaterialDialog = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMaterialDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LabeledSmallFab(
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    icon: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor
        ) {
            icon()
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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

private fun getFileExtension(context: Context, uri: Uri): String? {
    val mimeType = context.contentResolver.getType(uri)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
}
