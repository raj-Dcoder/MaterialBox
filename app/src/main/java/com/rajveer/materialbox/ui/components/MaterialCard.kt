package com.rajveer.materialbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType
import com.rajveer.materialbox.ui.theme.*
import com.rajveer.materialbox.util.toRelativeTimeString
import java.io.File

// ============================================================
// Helper: maps each MaterialType to its icon + accent color
// ============================================================
fun MaterialType.icon(): ImageVector = when (this) {
    MaterialType.PDF -> Icons.Filled.PictureAsPdf
    MaterialType.LINK -> Icons.Filled.Link
    MaterialType.NOTE -> Icons.AutoMirrored.Filled.StickyNote2
    MaterialType.IMAGE -> Icons.Filled.Image
    MaterialType.TXT -> Icons.AutoMirrored.Filled.TextSnippet
    MaterialType.DOCX -> Icons.Filled.Description
}

fun MaterialType.accentColor(): Color = when (this) {
    MaterialType.PDF -> MaterialPdfColor
    MaterialType.LINK -> MaterialLinkColor
    MaterialType.NOTE -> MaterialNoteColor
    MaterialType.IMAGE -> MaterialImageColor
    MaterialType.TXT -> MaterialTxtColor
    MaterialType.DOCX -> MaterialDocxColor
}

fun getLinkThumbnailUrl(link: String): String? {
    try {
        val uri = android.net.Uri.parse(link)
        val host = uri.host?.lowercase() ?: return null
        
        if (host.contains("youtube.com") && uri.path?.contains("/watch") == true) {
            val videoId = uri.getQueryParameter("v")
            if (videoId != null) {
                return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        } else if (host.contains("youtu.be")) {
            val videoId = uri.lastPathSegment
            if (videoId != null) {
                return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
            }
        }
        
        // Return Google Favicon service for other domains
        return "https://www.google.com/s2/favicons?domain=${host}&sz=128"
    } catch (e: Exception) {
        return null
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MaterialCard(
    material: Material,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = material.type.accentColor()
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    com.rajveer.materialbox.util.HapticUtils.playClick(context)
                    onClick()
                },
                onLongClick = {
                    com.rajveer.materialbox.util.HapticUtils.playHeavyClick(context)
                    onLongPress()
                },
                onLongClickLabel = "More options",
                onClickLabel = "Open material"
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon OR image thumbnail for IMAGE type
            val isLinkWithThumbnail = material.type == MaterialType.LINK && getLinkThumbnailUrl(material.pathOrUrl) != null

            if (material.type == MaterialType.IMAGE || isLinkWithThumbnail) {
                // Show actual image thumbnail or link preview
                val modelData: Any? = if (material.type == MaterialType.IMAGE) {
                    val isContentUri = material.pathOrUrl.startsWith("content://") || material.pathOrUrl.startsWith("file://")
                    if (isContentUri) android.net.Uri.parse(material.pathOrUrl) else java.io.File(context.filesDir, material.pathOrUrl)
                } else {
                    getLinkThumbnailUrl(material.pathOrUrl)
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .size(128) // thumbnail size in px
                            .build(),
                        contentDescription = "Preview thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            } else {
                // Standard type icon with accent-colored container
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = material.type.icon(),
                            contentDescription = "${material.type.name} icon",
                            tint = typeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title + metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type badge
                    Text(
                        text = material.type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )

                    // Dot separator
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Relative time
                    Text(
                        text = material.createdAt.toRelativeTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // View count badge (only show if > 0)
            if (material.viewCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "Views",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = material.viewCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}