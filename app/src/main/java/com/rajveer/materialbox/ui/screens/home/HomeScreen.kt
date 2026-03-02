package com.rajveer.materialbox.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.navigation.Screen
import com.rajveer.materialbox.ui.components.SubjectCard
import com.rajveer.materialbox.ui.components.accentColor
import com.rajveer.materialbox.ui.components.icon
import com.rajveer.materialbox.util.toRelativeTimeString
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
    val topicCount by viewModel.topicCount.collectAsState()
    val materialCount by viewModel.materialCount.collectAsState()
    var showDeleteSubjectDialog by remember { mutableStateOf<Subject?>(null) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Show TopAppBar title only when the header is scrolled out of view
    val showTopBarTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // ============================================================
    // File opener utility
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

    Scaffold(
        topBar = {
            // Sticky TopAppBar — shows "MaterialBox" only after scroll
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = showTopBarTitle,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "MaterialBox",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (showTopBarTitle)
                        MaterialTheme.colorScheme.background
                    else
                        Color.Transparent
                )
            )
        },
        floatingActionButton = {
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ============================================================
            // HEADER — App name + Greeting + Stats
            // ============================================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "MaterialBox",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = viewModel.getGreeting(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${subjects.size} subjects • $topicCount topics • $materialCount materials",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // ============================================================
            // FILTER CHIPS — Recent / Most Viewed
            // ============================================================
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == HomeViewModel.ViewMode.RECENTLY_ADDED,
                        onClick = { viewModel.setViewMode(HomeViewModel.ViewMode.RECENTLY_ADDED) },
                        label = { Text("Recent") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = viewMode == HomeViewModel.ViewMode.MOST_VIEWED,
                        onClick = { viewModel.setViewMode(HomeViewModel.ViewMode.MOST_VIEWED) },
                        label = { Text("Most Viewed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // ============================================================
            // MATERIALS — Horizontal scroll with COMPACT cards
            // ============================================================
            if (materials.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(materials) { material ->
                            CompactMaterialCard(
                                material = material,
                                onClick = {
                                    if (material.type == MaterialType.NOTE) {
                                        navController.navigate(Screen.MaterialDetail.createRoute(material.id))
                                    } else {
                                        handleOpenFile(material)
                                        viewModel.incrementViewCount(material.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ============================================================
            // SUBJECTS SECTION
            // ============================================================
            item {
                Text(
                    text = "Subjects",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (subjects.isEmpty()) {
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
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
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
// Compact Material Card — for horizontal scroll on HomeScreen
// Vertical layout: icon/thumbnail on top, title, date below
// ============================================================
@Composable
private fun CompactMaterialCard(
    material: Material,
    onClick: () -> Unit
) {
    val typeColor = material.type.accentColor()
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(150.dp)
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Type icon OR image thumbnail
            if (material.type == MaterialType.IMAGE) {
                val imageFile = File(context.filesDir, material.pathOrUrl)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageFile)
                        .crossfade(true)
                        .size(96)
                        .build(),
                    contentDescription = "Image thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = material.type.icon(),
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Title + date
            Column {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = material.createdAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ============================================================
// Reusable empty state card
// ============================================================
@Composable
fun EmptyStateCard(
    icon: ImageVector,
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