package com.rajveer.materialbox.ui.screens.topicdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.ActionMenuBottomSheet
import com.rajveer.materialbox.ui.components.MaterialCard
import com.rajveer.materialbox.ui.screens.home.EmptyStateCard
import com.rajveer.materialbox.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.compose.ui.platform.LocalContext
import com.rajveer.materialbox.util.HapticUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    navController: NavController,
    viewModel: TopicDetailViewModel = hiltViewModel(),
    topicId: Long
) {
    val uiState by viewModel.uiState.collectAsState()
    val checklistCount by viewModel.getChecklistItemCount().collectAsState(initial = 0)
    val topic = uiState.topic
    val materials = uiState.materials
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedActionMaterial by remember { mutableStateOf<Material?>(null) }
    var showEditMaterialDialog by remember { mutableStateOf<Material?>(null) }
    var showDeleteMaterialDialog by remember { mutableStateOf<Material?>(null) }
    var fabExpanded by remember { mutableStateOf(false) }
    var scannedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var scannedDocumentName by remember { mutableStateOf("") }

    val context = LocalContext.current
    // LocalView removed

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(20)
            .setResultFormats(RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
    
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                scannedPdfUri = pdf.uri
                val timeStamp = SimpleDateFormat("yyyy-MM-dd (HH:mm)", Locale.getDefault()).format(Date())
                scannedDocumentName = "Scan - ${topic?.name ?: "Topic"} - $timeStamp"
            }
        }
    }

    // File opener utility
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        HapticUtils.playHeavyClick(context)
                        showDeleteDialog = true 
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Topic")
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fabExpanded) {
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
                        enter = fadeIn(tween(200, delayMillis = 50)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200, delayMillis = 50)),
                        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                    ) {
                        LabeledSmallFab(
                            label = "Note",
                            onClick = {
                                HapticUtils.playClick(context)
                                topic?.let {
                                    navController.navigate(Screen.AddMaterial.createRoute(it.id))
                                }
                                fabExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TextSnippet, contentDescription = "Add Text Material")
                        }
                    }

                    // Add link
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(200, delayMillis = 100)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200, delayMillis = 100)),
                        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                    ) {
                        LabeledSmallFab(
                            label = "Link",
                            onClick = {
                                HapticUtils.playClick(context)
                                topic?.let {
                                    navController.navigate(Screen.AddMaterial.createRoute(it.id, MaterialType.LINK.name, ""))
                                }
                                fabExpanded = false
                            },
                            containerColor = MaterialLinkColor.copy(alpha = 0.15f),
                            contentColor = MaterialLinkColor
                        ) {
                            Icon(Icons.Default.Link, contentDescription = "Add Link")
                        }
                    }

                    // Add Checklist
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(200, delayMillis = 150)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(200, delayMillis = 150)),
                        exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200))
                    ) {
                        LabeledSmallFab(
                            label = "Checklist",
                            onClick = {
                                HapticUtils.playClick(context)
                                topic?.let {
                                    navController.navigate(Screen.TopicChecklist.createRoute(it.id))
                                }
                                fabExpanded = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Add Checklist")
                        }
                    }
                }

                // Main FAB
                FloatingActionButton(
                    onClick = { 
                        HapticUtils.playClick(context)
                        fabExpanded = !fabExpanded 
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (fabExpanded) "Close options" else "Add Material",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (topic != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (checklistCount > 0) {
                    item {
                        Card(
                            onClick = {
                                topic?.let {
                                    HapticUtils.playClick(context)
                                    navController.navigate(Screen.TopicChecklist.createRoute(it.id))
                                }
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
                                        text = "Topic Checklist",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$checklistCount item${if (checklistCount == 1) "" else "s"} to track",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (materials.isNotEmpty()) {
                    item {
                        Text(
                            text = "Materials",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (materials.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Inventory2,
                            title = "No materials yet",
                            subtitle = "Tap the + button to add notes, files, or links"
                        )
                    }
                } else {
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
                                selectedActionMaterial = material
                            }
                        )
                    }
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Delete topic dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Topic") },
                text = { Text("This will also delete all materials inside it.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            HapticUtils.playHeavyClick(context)
                            viewModel.deleteTopic()
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

        // Action Menu Bottom Sheet
        if (selectedActionMaterial != null) {
            ActionMenuBottomSheet(
                title = selectedActionMaterial?.title ?: "Options",
                onDismissRequest = { selectedActionMaterial = null },
                onEditClick = { showEditMaterialDialog = selectedActionMaterial },
                onDeleteClick = { showDeleteMaterialDialog = selectedActionMaterial }
            )
        }

        // Edit Material dialog
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

        // Delete material dialog
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
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteMaterialDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Rename Scanned Document Dialog
        if (scannedPdfUri != null) {
            AlertDialog(
                onDismissRequest = { scannedPdfUri = null },
                title = { Text("Name your scan") },
                text = {
                    OutlinedTextField(
                        value = scannedDocumentName,
                        onValueChange = { scannedDocumentName = it },
                        label = { Text("Document Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val finalName = if (scannedDocumentName.isNotBlank()) {
                                if (scannedDocumentName.endsWith(".pdf", ignoreCase = true)) scannedDocumentName else "$scannedDocumentName.pdf"
                            } else "Untitled Scan.pdf"
                            
                            viewModel.saveDocumentMaterials(
                                listOf(DocumentInfo(scannedPdfUri!!, finalName, MaterialType.PDF))
                            ) { added, skipped ->
                                if (added > 0) Toast.makeText(context, "'$finalName' saved.", Toast.LENGTH_SHORT).show()
                                else Toast.makeText(context, "Duplicate skipped.", Toast.LENGTH_SHORT).show()
                            }
                            scannedPdfUri = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scannedPdfUri = null }) {
                        Text("Discard")
                    }
                }
            )
        }
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

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
