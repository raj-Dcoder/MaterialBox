package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rajveer.materialbox.ui.screens.home.EmptyStateCard
import com.rajveer.materialbox.util.HapticUtils
import com.rajveer.materialbox.util.toRelativeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeFeedDetailScreen(
    navController: NavController,
    viewModel: YoutubeFeedDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.feed?.name ?: "Loading...",
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
                        HapticUtils.playClick(context)
                        showEditSheet = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Feed")
                    }
                    IconButton(onClick = {
                        HapticUtils.playHeavyClick(context)
                        showDeleteDialog = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Feed")
                    }
                }
            )
        }
    ) { padding ->

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshFeed() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.videos.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Outlined.VideoLibrary,
                                title = "No videos found",
                                subtitle = "Pull down to refresh, or check the channel URLs."
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // "Last updated X ago" header
                        item {
                            uiState.lastCachedAt?.let { date ->
                                Text(
                                    text = "Updated ${date.toRelativeTimeString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                        }

                        items(uiState.videos) { video ->
                            YoutubeVideoCard(video = video) {
                                try {
                                    viewModel.markWatched(video.videoUrl)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context, "Could not open video",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        // ── Delete dialog ──────────────────────────────────────────────────
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Feed") },
                text = { Text("Are you sure you want to delete this YouTube feed?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFeed()
                            showDeleteDialog = false
                            navController.navigateUp()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ── Edit Bottom Sheet ──────────────────────────────────────────────
        if (showEditSheet) {
            val feed = uiState.feed
            if (feed != null) {
                var editedName by remember { mutableStateOf(feed.name) }
                var editedChannels by remember {
                    mutableStateOf(feed.channelUrls.toMutableList().ifEmpty { mutableListOf("") })
                }

                ModalBottomSheet(
                    onDismissRequest = { showEditSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Edit Feed",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Feed Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        item {
                            Text(
                                "Channel URLs",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        itemsIndexed(editedChannels) { index, url ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = url,
                                    onValueChange = { newUrl ->
                                        val list = editedChannels.toMutableList()
                                        list[index] = newUrl
                                        editedChannels = list
                                    },
                                    label = { Text("Channel URL") },
                                    placeholder = { Text("https://www.youtube.com/@channel") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                if (editedChannels.size > 1) {
                                    IconButton(onClick = {
                                        val list = editedChannels.toMutableList()
                                        list.removeAt(index)
                                        editedChannels = list
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove channel",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TextButton(
                                onClick = {
                                    val list = editedChannels.toMutableList()
                                    list.add("")
                                    editedChannels = list
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Another Channel")
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    if (editedName.isNotBlank() && editedChannels.any { it.isNotBlank() }) {
                                        HapticUtils.playClick(context)
                                        viewModel.updateFeed(editedName, editedChannels)
                                        showEditSheet = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = editedName.isNotBlank() && editedChannels.any { it.isNotBlank() }
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YoutubeVideoCard(video: YoutubeVideo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (video.isWatched) 0.35f else 0.5f
            )
        )
    ) {
        Column {
            // ── Thumbnail with watched progress bar overlay (Option D) ──
            Box {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                // Red "watched" bar at the bottom of the thumbnail
                if (video.isWatched) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (video.isWatched)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = video.publishedAt.toRelativeTimeString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
