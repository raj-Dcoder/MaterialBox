package com.rajveer.materialbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajveer.materialbox.data.entity.Material
import com.rajveer.materialbox.data.entity.MaterialType

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MaterialCard(
    material: Material,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = when (material.type) {
                    MaterialType.PDF -> MaterialTheme.colorScheme.errorContainer
                    MaterialType.DOCX -> MaterialTheme.colorScheme.tertiaryContainer
                    MaterialType.IMAGE -> MaterialTheme.colorScheme.secondaryContainer
                    MaterialType.LINK -> MaterialTheme.colorScheme.primaryContainer
                    MaterialType.NOTE -> MaterialTheme.colorScheme.surfaceVariant
                    MaterialType.TXT -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (material.type) {
                            MaterialType.PDF -> Icons.Default.PictureAsPdf
                            MaterialType.DOCX -> Icons.Default.Description
                            MaterialType.IMAGE -> Icons.Default.Image
                            MaterialType.LINK -> Icons.Default.Link
                            MaterialType.NOTE -> Icons.Default.Note
                            MaterialType.TXT -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when (material.type) {
                            MaterialType.PDF -> MaterialTheme.colorScheme.onErrorContainer
                            MaterialType.DOCX -> MaterialTheme.colorScheme.onTertiaryContainer
                            MaterialType.IMAGE -> MaterialTheme.colorScheme.onSecondaryContainer
                            MaterialType.LINK -> MaterialTheme.colorScheme.onPrimaryContainer
                            MaterialType.NOTE -> MaterialTheme.colorScheme.onSurfaceVariant
                            MaterialType.TXT -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = material.type.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = material.viewCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Material",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}