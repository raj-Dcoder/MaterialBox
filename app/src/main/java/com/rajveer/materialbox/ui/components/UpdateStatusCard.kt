package com.rajveer.materialbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajveer.materialbox.util.update.AppUpdateUiState

@Composable
fun UpdateStatusCard(
    state: AppUpdateUiState,
    onInstallClick: () -> Unit,
    onRetryDownloadClick: () -> Unit,
    onOpenReleasePageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = when (state) {
        is AppUpdateUiState.Downloading -> UpdateCardContent(
            eyebrow = "Update downloading",
            title = "MaterialBox ${state.release.versionName} is being downloaded",
            body = "We found a newer GitHub release and started the APK download automatically.",
            primaryLabel = "View release",
            secondaryLabel = null,
            progressPercent = state.progressPercent,
            primaryAction = onOpenReleasePageClick,
            secondaryAction = null,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        is AppUpdateUiState.ReadyToInstall -> UpdateCardContent(
            eyebrow = "Update ready",
            title = "MaterialBox ${state.release.versionName} is ready to install",
            body = "Android requires your confirmation for APK installs. Tap install and the system installer will take it from there.",
            primaryLabel = "Install now",
            secondaryLabel = "View release",
            progressPercent = 100,
            primaryAction = onInstallClick,
            secondaryAction = onOpenReleasePageClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdateAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        is AppUpdateUiState.ActionRequired -> UpdateCardContent(
            eyebrow = "New release available",
            title = "MaterialBox ${state.release.versionName} is out now",
            body = state.message,
            primaryLabel = if (state.canDownloadInApp) "Download update" else "Open GitHub release",
            secondaryLabel = if (state.canDownloadInApp) "View release" else null,
            progressPercent = null,
            primaryAction = if (state.canDownloadInApp) onRetryDownloadClick else onOpenReleasePageClick,
            secondaryAction = if (state.canDownloadInApp) onOpenReleasePageClick else null,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        else -> null
    } ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                content.icon()
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = content.eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = content.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (content.progressPercent != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { content.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                    Text(
                        text = "${content.progressPercent}% downloaded",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = content.primaryAction) {
                    Text(content.primaryLabel)
                }
                content.secondaryLabel?.let { label ->
                    OutlinedButton(onClick = { content.secondaryAction?.invoke() }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

private data class UpdateCardContent(
    val eyebrow: String,
    val title: String,
    val body: String,
    val primaryLabel: String,
    val secondaryLabel: String?,
    val progressPercent: Int?,
    val primaryAction: () -> Unit,
    val secondaryAction: (() -> Unit)?,
    val icon: @Composable () -> Unit
)
