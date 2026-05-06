package com.rajveer.materialbox.ui.screens.youtubefeeddetail

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rajveer.materialbox.ui.screens.home.EmptyStateCard
import com.rajveer.materialbox.util.HapticUtils
import com.rajveer.materialbox.util.YoutubeRssFetcher
import com.rajveer.materialbox.util.toRelativeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val VIDEO_PREFS = "youtube_video_metadata"
private const val MAX_MANUAL_RESUME_SECONDS = 24 * 60 * 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeFeedDetailScreen(
    navController: NavController,
    viewModel: YoutubeFeedDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val videoPrefs = remember {
        context.getSharedPreferences(VIDEO_PREFS, android.content.Context.MODE_PRIVATE)
    }
    val resumeSecondsByUrl = remember { mutableStateMapOf<String, Int>() }
    val durationSecondsByUrl = remember { mutableStateMapOf<String, Int>() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var selectedTimestampVideo by remember { mutableStateOf<YoutubeVideo?>(null) }

    val pullRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error if it exists
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.videos) {
        uiState.videos.forEach { video ->
            val resumeSeconds = videoPrefs.getInt(video.resumePreferenceKey(), 0)
            if (resumeSeconds > 0) {
                resumeSecondsByUrl[video.videoUrl] = resumeSeconds
            } else {
                resumeSecondsByUrl.remove(video.videoUrl)
            }

            val cachedDuration = videoPrefs.getInt(video.durationPreferenceKey(), -1)
            if (cachedDuration > 0) {
                durationSecondsByUrl[video.videoUrl] = cachedDuration
            }
        }

        uiState.videos
            .filter { durationSecondsByUrl[it.videoUrl] == null }
            .take(20)
            .forEach { video ->
                val durationSeconds = withContext(Dispatchers.IO) {
                    YoutubeRssFetcher.fetchVideoDurationSeconds(video.videoUrl)
                }
                if (durationSeconds != null) {
                    videoPrefs.edit()
                        .putInt(video.durationPreferenceKey(), durationSeconds)
                        .apply()
                    durationSecondsByUrl[video.videoUrl] = durationSeconds
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            CircularProgressIndicator()
                        }
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
                            YoutubeVideoCard(
                                video = video,
                                resumeSeconds = resumeSecondsByUrl[video.videoUrl] ?: 0,
                                durationSeconds = durationSecondsByUrl[video.videoUrl],
                                onLongClick = {
                                    HapticUtils.playHeavyClick(context)
                                    selectedTimestampVideo = video
                                }
                            ) {
                                try {
                                    viewModel.markWatched(video.videoUrl)
                                    val resumeSeconds = resumeSecondsByUrl[video.videoUrl] ?: 0
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(video.videoUrl.withYoutubeTimestamp(resumeSeconds))
                                    )
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

        selectedTimestampVideo?.let { video ->
            TimestampSetterSheet(
                video = video,
                initialSeconds = resumeSecondsByUrl[video.videoUrl] ?: 0,
                durationSeconds = durationSecondsByUrl[video.videoUrl],
                onDismiss = { selectedTimestampVideo = null },
                onSave = { rawSeconds ->
                    val safeSeconds = sanitizeResumeSeconds(rawSeconds, durationSecondsByUrl[video.videoUrl])
                    videoPrefs.edit().apply {
                        if (safeSeconds > 0) {
                            putInt(video.resumePreferenceKey(), safeSeconds)
                        } else {
                            remove(video.resumePreferenceKey())
                        }
                    }.apply()

                    if (safeSeconds > 0) {
                        resumeSecondsByUrl[video.videoUrl] = safeSeconds
                    } else {
                        resumeSecondsByUrl.remove(video.videoUrl)
                    }

                    selectedTimestampVideo = null
                }
            )
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
                            // Auto-trigger resolution for existing and new URLs
                            LaunchedEffect(url) {
                                if (url.isNotBlank()) {
                                    viewModel.resolveChannelName(url)
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
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

                                val nameResolved = uiState.channelNames[url]
                                if (nameResolved != null) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = nameResolved,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YoutubeVideoCard(
    video: YoutubeVideo,
    resumeSeconds: Int,
    durationSeconds: Int?,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
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
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .allowHardware(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                if (resumeSeconds > 0) {
                    ThumbnailBadge(
                        text = "Resume ${resumeSeconds.formatTimestamp()}",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp)
                    )
                }
                durationSeconds?.let { duration ->
                    ThumbnailBadge(
                        text = duration.formatTimestamp(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 8.dp)
                    )
                }
                val progressFraction = when {
                    durationSeconds != null && durationSeconds > 0 && resumeSeconds > 0 ->
                        (resumeSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                    video.isWatched -> 1f
                    else -> 0f
                }
                if (progressFraction > 0f) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomStart)
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(maxWidth * progressFraction)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                } else if (video.isWatched) {
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

@Composable
private fun ThumbnailBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimestampSetterSheet(
    video: YoutubeVideo,
    initialSeconds: Int,
    durationSeconds: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var hours by remember(video.videoUrl) { mutableStateOf((initialSeconds / 3600).toString()) }
    var minutes by remember(video.videoUrl) { mutableStateOf(((initialSeconds % 3600) / 60).toString()) }
    var seconds by remember(video.videoUrl) { mutableStateOf((initialSeconds % 60).toString()) }
    val totalSeconds = remember(hours, minutes, seconds, durationSeconds) {
        sanitizeResumeSeconds(parseTimeInput(hours, minutes, seconds), durationSeconds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Set Resume Point",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TimeInputField("Hours", hours, { hours = it.onlyDigits(3) }, Modifier.weight(1f))
                TimeInputField("Minutes", minutes, { minutes = it.onlyDigits(3) }, Modifier.weight(1f))
                TimeInputField("Seconds", seconds, { seconds = it.onlyDigits(3) }, Modifier.weight(1f))
            }

            Text(
                text = buildString {
                    append("Will open at ")
                    append(totalSeconds.formatTimestamp())
                    durationSeconds?.let {
                        append(" of ")
                        append(it.formatTimestamp())
                    }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (durationSeconds != null && parseTimeInput(hours, minutes, seconds) >= durationSeconds) {
                Text(
                    text = "That time is past the video length, so it will be adjusted automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (durationSeconds != null && durationSeconds > 0) {
                Slider(
                    value = totalSeconds.toFloat().coerceIn(0f, durationSeconds.toFloat()),
                    onValueChange = { value ->
                        val normalized = sanitizeResumeSeconds(value.toInt(), durationSeconds)
                        hours = (normalized / 3600).toString()
                        minutes = ((normalized % 3600) / 60).toString()
                        seconds = (normalized % 60).toString()
                    },
                    valueRange = 0f..durationSeconds.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSave(0) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = { onSave(parseTimeInput(hours, minutes, seconds)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun TimeInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun String.onlyDigits(maxLength: Int): String =
    filter { it.isDigit() }.take(maxLength)

private fun parseTimeInput(hours: String, minutes: String, seconds: String): Int {
    val total = hours.safeLong() * 3600L + minutes.safeLong() * 60L + seconds.safeLong()
    return total.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}

private fun String.safeLong(): Long = toLongOrNull() ?: 0L

private fun sanitizeResumeSeconds(seconds: Int, durationSeconds: Int?): Int {
    if (seconds <= 0) return 0
    if (durationSeconds == null || durationSeconds <= 0) {
        return seconds.coerceAtMost(MAX_MANUAL_RESUME_SECONDS)
    }
    return seconds.coerceAtMost((durationSeconds - 5).coerceAtLeast(0))
}

private fun Int.formatTimestamp(): String {
    val safe = coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun YoutubeVideo.resumePreferenceKey(): String = "resume_${videoUrl.stablePreferenceKey()}"

private fun YoutubeVideo.durationPreferenceKey(): String = "duration_${videoUrl.stablePreferenceKey()}"

private fun String.stablePreferenceKey(): String =
    Base64.encodeToString(toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)

private fun String.withYoutubeTimestamp(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    if (safeSeconds == 0) return this

    val uri = runCatching { Uri.parse(this) }.getOrNull() ?: return this
    val builder = uri.buildUpon().clearQuery()

    for (name in uri.queryParameterNames) {
        if (name != "t" && name != "start") {
            for (value in uri.getQueryParameters(name)) {
                builder.appendQueryParameter(name, value)
            }
        }
    }

    builder.appendQueryParameter("t", "${safeSeconds}s")
    return builder.build().toString()
}
