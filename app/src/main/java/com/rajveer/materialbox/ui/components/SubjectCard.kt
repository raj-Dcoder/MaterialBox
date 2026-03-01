package com.rajveer.materialbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajveer.materialbox.data.entity.Subject
import com.rajveer.materialbox.util.toRelativeTimeString

// ============================================================
// A list of subtle accent colors that cycle based on position.
// This gives each subject card a unique "personality" without
// being random — the same subject always gets the same color.
// ============================================================
private val subjectAccentColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF5C6BC0), // Indigo
    androidx.compose.ui.graphics.Color(0xFF00BFA5), // Teal
    androidx.compose.ui.graphics.Color(0xFFFF6B9D), // Coral
    androidx.compose.ui.graphics.Color(0xFF7C4DFF), // Deep Purple
    androidx.compose.ui.graphics.Color(0xFFFFD740), // Amber
    androidx.compose.ui.graphics.Color(0xFF448AFF), // Blue
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectCard(
    subject: Subject,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pick an accent color based on the subject's ID (consistent across sessions)
    val accent = subjectAccentColors[(subject.id % subjectAccentColors.size).toInt()]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
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
            // Letter avatar with accent color
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.name.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subject.createdAt.toRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron — visual hint that this is tappable
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}