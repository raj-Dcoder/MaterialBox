package com.rajveer.materialbox.ui.screens.manageoriginals

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rajveer.materialbox.data.entity.Material
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import android.app.Activity
import android.app.PendingIntent
import android.content.IntentSender
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageOriginalsScreen(
    viewModel: ManageOriginalsViewModel = hiltViewModel()
) {
    val materials by viewModel.materialsWithOriginalUri.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    var lastDeniedPermanently by remember { mutableStateOf(false) }
    var showManualDeleteDialog by remember { mutableStateOf(false) }
    var manualDeleteFileName by remember { mutableStateOf("") }
    var showSAFDeleteResultDialog by remember { mutableStateOf(false) }
    var safDeleteResultMessage by remember { mutableStateOf("") }
    var showHelpDialog by remember { mutableStateOf(false) }
    var manualDeleteFilePath by remember { mutableStateOf("") }
    var showMediaStoreDeleteResultDialog by remember { mutableStateOf(false) }
    var mediaStoreDeleteResultMessage by remember { mutableStateOf("") }

    // Determine the correct permissions for the Android version
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        val anyDeniedPermanently = result.entries.any { !it.value &&
            !shouldShowRequestPermissionRationale(context, it.key) }
        if (allGranted) {
            pendingDelete = false
            performDelete(
                context = context,
                materials = materials,
                selectedIds = selectedIds,
                setIsDeleting = { isDeleting = it },
                setResultMessage = { resultMessage = it },
                setShowResultDialog = { showResultDialog = it },
                clearSelection = { viewModel.clearSelection() }
            )
        } else if (anyDeniedPermanently) {
            lastDeniedPermanently = true
            showSettingsDialog = true
            pendingDelete = false
        } else {
            showPermissionRationale = true
            pendingDelete = false
        }
    }

    fun requestPermissions() {
        permissionLauncher.launch(permissions)
    }

    val safDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                safDeleteResultMessage = if (rows > 0) {
                    "File deleted successfully."
                } else {
                    "Failed to delete the selected file."
                }
            } catch (e: Exception) {
                safDeleteResultMessage = "Error deleting file: ${e.message}"
            }
            showSAFDeleteResultDialog = true
        }
    }

    // Pass these setters to deleteOriginalFiles
    fun setShowManualDeleteDialog(value: Boolean) { showManualDeleteDialog = value }
    fun setManualDeleteFileName(value: String) {
        manualDeleteFileName = value
        manualDeleteFilePath = tryExtractFilePathFromUri(context, value)
    }

    val activity = context as? Activity
    val mediaStoreDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        mediaStoreDeleteResultMessage = if (result.resultCode == Activity.RESULT_OK) {
            "File(s) deleted successfully via system dialog."
        } else {
            "File(s) not deleted. User may have cancelled or denied the request."
        }
        showMediaStoreDeleteResultDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Original Files") })
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (materials.isEmpty()) {
                    Text("No original files to manage.")
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(materials) { material ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(material.title, style = MaterialTheme.typography.titleMedium)
                                    Text(material.originalFileUri ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                                Checkbox(
                                    checked = selectedIds.contains(material.id),
                                    onCheckedChange = { checked ->
                                        viewModel.setSelection(material.id, checked)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Check for storage permission before deleting
                            if (hasAllPermissions(context, permissions)) {
                                performDelete(
                                    context = context,
                                    materials = materials,
                                    selectedIds = selectedIds,
                                    setIsDeleting = { isDeleting = it },
                                    setResultMessage = { resultMessage = it },
                                    setShowResultDialog = { showResultDialog = it },
                                    clearSelection = { viewModel.clearSelection() },
                                    setShowManualDeleteDialog = ::setShowManualDeleteDialog,
                                    setManualDeleteFileName = ::setManualDeleteFileName
                                )
                            } else {
                                pendingDelete = true
                                requestPermissions()
                            }
                        },
                        enabled = selectedIds.isNotEmpty() && !isDeleting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Delete Selected Originals")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { safDeleteLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick file to delete using system file picker")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            // Only attempt for media files (images, video, audio)
                            val selectedMaterials = materials.filter { selectedIds.contains(it.id) }
                            val mediaUris = selectedMaterials.mapNotNull {
                                val uriStr = it.originalFileUri
                                if (uriStr != null) {
                                    val uri = Uri.parse(uriStr)
                                    // Try to convert document URI to MediaStore URI
                                    val mediaStoreUri = if (uriStr.startsWith("content://com.android.providers.media.documents/document/")) {
                                        documentUriToMediaStoreUri(context, uri)
                                    } else if (uriStr.startsWith("content://media/external/images") || uriStr.startsWith("content://media/external/video") || uriStr.startsWith("content://media/external/audio")) {
                                        uri
                                    } else null
                                    mediaStoreUri
                                } else null
                            }
                            if (mediaUris.isNotEmpty() && activity != null) {
                                try {
                                    val pi = MediaStore.createDeleteRequest(context.contentResolver, mediaUris)
                                    mediaStoreDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(pi.intentSender).build()
                                    )
                                } catch (e: Exception) {
                                    mediaStoreDeleteResultMessage = "Error launching system delete dialog: ${e.message}"
                                    showMediaStoreDeleteResultDialog = true
                                }
                            } else {
                                mediaStoreDeleteResultMessage = "No supported media files selected for system dialog deletion."
                                showMediaStoreDeleteResultDialog = true
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete with system dialog (MediaStore)")
                    }
                }
            }
            if (showResultDialog) {
                AlertDialog(
                    onDismissRequest = { showResultDialog = false },
                    title = { Text("Delete Results", style = MaterialTheme.typography.titleLarge) },
                    text = { Text(resultMessage, style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                        TextButton(onClick = { showResultDialog = false }) {
                            Text("OK")
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                )
            }
            if (showPermissionRationale) {
                AlertDialog(
                    onDismissRequest = { showPermissionRationale = false },
                    title = { Text("Permission Required") },
                    text = { Text("Storage permission is required to delete original files. Please grant the permission and try again.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showPermissionRationale = false
                            requestPermissions()
                        }) {
                            Text("Grant Access")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPermissionRationale = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Permission Required") },
                    text = { Text("Storage permission is permanently denied. Please open app settings and grant the permission manually.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showSettingsDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:" + context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Open Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            if (showManualDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showManualDeleteDialog = false },
                    title = { Text("Manual Deletion Required") },
                    text = {
                        Column {
                            Text("The file '", style = MaterialTheme.typography.bodyMedium)
                            Text(manualDeleteFileName, style = MaterialTheme.typography.bodyLarge)
                            if (manualDeleteFilePath.isNotBlank()) {
                                Text("Path: $manualDeleteFilePath", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("\ncannot be deleted automatically due to Android security restrictions. Please delete it manually using your file manager.", style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        Row {
                            TextButton(onClick = {
                                // Copy file name to clipboard
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("File Name", manualDeleteFileName)
                                clipboard.setPrimaryClip(clip)
                                showManualDeleteDialog = false
                            }) {
                                Text("Copy file name")
                            }
                            TextButton(onClick = { showHelpDialog = true }) {
                                Text("Help")
                            }
                            TextButton(onClick = { showManualDeleteDialog = false }) {
                                Text("OK")
                            }
                        }
                    }
                )
            }
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { Text("Why can't I delete this file?") },
                    text = { Text("Due to Android security restrictions, some files (especially those not created by this app or picked from certain locations) cannot be deleted automatically. To delete such files, please use your device's file manager and search for the file name. If you copied the file name, you can paste it in your file manager's search bar.") },
                    confirmButton = {
                        TextButton(onClick = { showHelpDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (showSAFDeleteResultDialog) {
                AlertDialog(
                    onDismissRequest = { showSAFDeleteResultDialog = false },
                    title = { Text("Delete File (SAF)") },
                    text = { Text(safDeleteResultMessage) },
                    confirmButton = {
                        TextButton(onClick = { showSAFDeleteResultDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (showMediaStoreDeleteResultDialog) {
                AlertDialog(
                    onDismissRequest = { showMediaStoreDeleteResultDialog = false },
                    title = { Text("System Delete Dialog Result") },
                    text = { Text(mediaStoreDeleteResultMessage) },
                    confirmButton = {
                        TextButton(onClick = { showMediaStoreDeleteResultDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    )
}

private fun performDelete(
    context: android.content.Context,
    materials: List<Material>,
    selectedIds: Set<Long>,
    setIsDeleting: (Boolean) -> Unit,
    setResultMessage: (String) -> Unit,
    setShowResultDialog: (Boolean) -> Unit,
    clearSelection: () -> Unit,
    setShowManualDeleteDialog: (Boolean) -> Unit = {},
    setManualDeleteFileName: (String) -> Unit = {}
) {
    setIsDeleting(true)
    val selectedMaterials = materials.filter { selectedIds.contains(it.id) }
    val (success, failed) = deleteOriginalFiles(context, selectedMaterials, setShowManualDeleteDialog, setManualDeleteFileName)
    val resultMessage = buildString {
        append("Deleted: ")
        if (success.isNotEmpty()) append(success.joinToString(", ")) else append("None")
        append("\nFailed: ")
        if (failed.isNotEmpty()) append(failed.joinToString(", ")) else append("None")
    }
    setResultMessage(resultMessage)
    setIsDeleting(false)
    setShowResultDialog(true)
    clearSelection()
}

private fun deleteOriginalFiles(
    context: Context,
    materials: List<Material>,
    setShowManualDeleteDialog: (Boolean) -> Unit = {},
    setManualDeleteFileName: (String) -> Unit = {}
): Pair<List<String>, List<String>> {
    val success = mutableListOf<String>()
    val failed = mutableListOf<String>()
    for (material in materials) {
        val uriString = material.originalFileUri
        if (uriString != null) {
            try {
                val uri = Uri.parse(uriString)
                Log.d("DeleteOriginal", "Attempting to delete URI: $uriString, scheme: ${uri.scheme}")
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    success.add(material.title)
                } else {
                    failed.add(material.title)
                    Log.e("DeleteOriginal", "Delete returned 0 rows for URI: $uriString")
                    Toast.makeText(context, "Failed to delete: ${material.title}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                failed.add(material.title)
                Log.e("DeleteOriginal", "Exception deleting URI: $uriString", e)
                Toast.makeText(context, "Error deleting ${material.title}: ${e.message}", Toast.LENGTH_LONG).show()
                if (e is java.lang.UnsupportedOperationException) {
                    setShowManualDeleteDialog(true)
                    setManualDeleteFileName(material.title)
                }
            }
        }
    }
    return Pair(success, failed)
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
    // This is a workaround for Compose, as we can't call ActivityCompat directly
    // In a real app, you might want to pass the Activity and use ActivityCompat.shouldShowRequestPermissionRationale
    return false // Always return false for simplicity; can be improved with Activity reference
}

fun tryExtractFilePathFromUri(context: Context, fileName: String): String {
    // This is a best-effort attempt; not all URIs can be resolved to a file path
    // For MediaStore URIs, you can try querying the display name and data column
    return "" // For now, just return empty; can be improved with a real resolver if needed
}

private fun documentUriToMediaStoreUri(context: Context, documentUri: Uri): Uri? {
    val projection = arrayOf("_id", "mime_type")
    val cursor = context.contentResolver.query(documentUri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val idIndex = it.getColumnIndex("_id")
            val mimeTypeIndex = it.getColumnIndex("mime_type")
            if (idIndex != -1 && mimeTypeIndex != -1) {
                val id = it.getString(idIndex)
                val mimeType = it.getString(mimeTypeIndex)
                val mediaType = when {
                    mimeType.startsWith("image/") -> "images"
                    mimeType.startsWith("video/") -> "video"
                    mimeType.startsWith("audio/") -> "audio"
                    else -> null
                }
                if (mediaType != null) {
                    return Uri.parse("content://media/external/$mediaType/media/$id")
                }
            }
        }
    }
    return null
} 