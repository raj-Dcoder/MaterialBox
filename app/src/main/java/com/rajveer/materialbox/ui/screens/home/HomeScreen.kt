package com.rajveer.materialbox.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
    val viewMode by viewModel.viewMode.collectAsState()
    var showDeleteSubjectDialog by remember { mutableStateOf<Subject?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ============================================================
    // File opener utility (handles both LINK and file-based materials)
    // TODO: Extract this to a shared util class (currently duplicated in TopicDetailScreen)
    // ============================================================
    val handleOpenFile: (Material) -> Unit = { mat ->
        if (mat.type == MaterialType.LINK) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(mat.pathOrUrl))
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open link", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
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

    // Collapsible top bar — shrinks when you scroll down
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MaterialBox",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Clean, static FAB — no gimmicky pulsating animation
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddSubject.route) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Subject",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ============================================================
            // SUBJECTS SECTION
            // ============================================================
            item {
                Text(
                    text = "Your Subjects",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (subjects.isEmpty()) {
                // Styled empty state — much better than plain text
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.School,
                        title = "No subjects yet",
                        subtitle = "Tap the + button to create your first subject"
                    )
                }
            } else {
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
            }

            // ============================================================
            // MATERIALS SECTION with toggle chips
            // ============================================================
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Materials",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle chips: "Recently Added" / "Most Viewed"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewMode == HomeViewModel.ViewMode.RECENTLY_ADDED,
                        onClick = { viewModel.setViewMode(HomeViewModel.ViewMode.RECENTLY_ADDED) },
                        label = { Text("Recent") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = viewMode == HomeViewModel.ViewMode.MOST_VIEWED,
                        onClick = { viewModel.setViewMode(HomeViewModel.ViewMode.MOST_VIEWED) },
                        label = { Text("Most Viewed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (materials.isEmpty()) {
                item {
                    Text(
                        text = "No materials yet. Add materials inside a subject to see them here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth()
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
                        onLongPress = {}
                    )
                }
            }

            // Bottom spacer so content doesn't sit behind the FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Delete subject confirmation dialog
        if (showDeleteSubjectDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSubjectDialog = null },
                title = { Text("Delete Subject") },
                text = { Text("Are you sure? This will delete all topics and materials inside it.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteSubjectDialog?.let { viewModel.deleteSubject(it) }
                            showDeleteSubjectDialog = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
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

// ============================================================
// Reusable empty state card — used across multiple screens
// Shows a large icon + title + subtitle
// ============================================================
@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}