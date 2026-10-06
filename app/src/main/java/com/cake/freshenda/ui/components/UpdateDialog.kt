package com.cake.freshenda.ui.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cake.freshenda.BuildConfig
import com.cake.freshenda.R
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.update.DownloadFailure
import com.cake.freshenda.update.DownloadProgress
import com.cake.freshenda.update.DownloadStatus
import com.cake.freshenda.update.UpdateFeedback
import com.cake.freshenda.update.UpdateResult
import com.cake.freshenda.update.UpdateUiState
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onIgnore: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    val info = state.latest
    val canUpdate = state.result == UpdateResult.AVAILABLE
    val download = state.download
    var expanded by remember(info?.versionCode) { mutableStateOf(false) }
    val notesScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FreshendaColors.Card,
        title = {
            Text(
                stringResource(when {
                    state.checking -> R.string.update_checking
                    download.active -> R.string.update_downloading
                    download.status == DownloadStatus.READY -> R.string.update_ready
                    download.status == DownloadStatus.FAILED -> R.string.update_download_failed_title
                    canUpdate -> R.string.update_found
                    state.result == UpdateResult.UP_TO_DATE -> R.string.update_latest
                    else -> R.string.update_section
                }),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(color = FreshendaColors.GlassSoft, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        VersionLabel(stringResource(R.string.update_installed_label), BuildConfig.VERSION_NAME, Modifier.weight(1f))
                        Text("→", Modifier.padding(horizontal = 12.dp), color = FreshendaColors.Unknown)
                        VersionLabel(stringResource(R.string.update_latest_label), info?.versionName ?: "—", Modifier.weight(1f))
                    }
                }
                AnimatedContent(
                    targetState = when {
                        state.checking -> 0
                        download.status != DownloadStatus.IDLE -> 1
                        else -> 2
                    },
                    transitionSpec = { fadeIn(tween(160, easing = MotionEase)) togetherWith fadeOut(tween(100)) },
                    label = "updateStage",
                ) { stage ->
                    when (stage) {
                        0 -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        1 -> DownloadPanel(download, state.feedback)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Column(
                                Modifier.heightIn(max = 280.dp).verticalScroll(notesScroll),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (state.result == UpdateResult.INCOMPATIBLE) {
                                    Text(stringResource(R.string.update_incompatible, info?.minSdk ?: 0), color = MaterialTheme.colorScheme.error)
                                }
                                UpdateError(state.feedback)
                                if (info != null) {
                                    Text(stringResource(R.string.update_highlights), style = MaterialTheme.typography.titleMedium)
                                    Column(
                                        Modifier.animateContentSize(tween(200, easing = MotionEase)),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        val notes = if (expanded) info.details else info.notes
                                        if (notes.isEmpty()) Text(stringResource(R.string.update_notes_empty))
                                        notes.forEach { note ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("•", color = FreshendaColors.Secondary)
                                                Text(note, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                            if (!info?.details.isNullOrEmpty()) {
                                TextButton(onClick = {
                                    expanded = !expanded
                                    scope.launch { notesScroll.scrollTo(0) }
                                }) {
                                    Text(stringResource(if (expanded) R.string.update_collapse else R.string.update_details))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                download.active -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_background)) }
                download.status == DownloadStatus.READY -> Button(onClick = onInstall) { Text(stringResource(R.string.update_install)) }
                download.status == DownloadStatus.FAILED -> Button(onClick = onDownload) { Text(stringResource(R.string.update_download_retry)) }
                canUpdate && info != null -> Button(onClick = onDownload, enabled = info.apkUrl != null) {
                    Text(stringResource(if (info.apkUrl != null) R.string.update_download else R.string.update_download_unavailable))
                }
                state.feedback != null -> Button(onClick = onRetry) { Text(stringResource(R.string.update_retry)) }
                else -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_close)) }
            }
        },
        dismissButton = {
            when {
                download.active -> TextButton(onClick = onCancelDownload, enabled = download.status != DownloadStatus.STARTING) {
                    Text(stringResource(R.string.update_download_cancel))
                }
                download.status == DownloadStatus.READY -> Row {
                    if (state.feedback == UpdateFeedback.INSTALL_FAILED) {
                        TextButton(onClick = onDownload) { Text(stringResource(R.string.update_download_retry)) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
                }
                download.status == DownloadStatus.FAILED -> TextButton(onClick = onCancelDownload) { Text(stringResource(R.string.update_back_to_notes)) }
                canUpdate -> Row {
                    TextButton(onClick = onIgnore) { Text(stringResource(R.string.update_ignore)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
                }
            }
        },
    )
}

@Composable
private fun DownloadPanel(download: DownloadProgress, feedback: UpdateFeedback?) {
    val context = LocalContext.current
    val fraction = download.fraction
    val animatedProgress by animateFloatAsState(fraction ?: 0f, tween(160, easing = MotionEase), label = "downloadProgress")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(when (download.status) {
                    DownloadStatus.STARTING -> R.string.update_connecting
                    DownloadStatus.WAITING -> R.string.update_waiting
                    DownloadStatus.READY -> R.string.update_download_complete
                    DownloadStatus.FAILED -> R.string.update_download_failed_title
                    else -> R.string.update_transferring
                }),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (fraction != null && download.status != DownloadStatus.FAILED) {
                Text((fraction * 100).toInt().toString() + "%", style = MaterialTheme.typography.titleMedium, color = FreshendaColors.Primary)
            }
        }
        if (download.status != DownloadStatus.FAILED) {
            if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
        }
        if (download.totalBytes > 0) {
            Text(
                Formatter.formatShortFileSize(context, download.downloadedBytes) + " / " + Formatter.formatShortFileSize(context, download.totalBytes),
                style = MaterialTheme.typography.bodySmall,
                color = FreshendaColors.Unknown,
            )
        }
        Text(
            stringResource(when {
                download.status == DownloadStatus.FAILED -> when (download.failure) {
                    DownloadFailure.STORAGE -> R.string.update_storage_failed
                    DownloadFailure.MISSING -> R.string.update_file_missing
                    else -> R.string.update_download_failed
                }
                download.status == DownloadStatus.READY -> R.string.update_install_hint
                else -> R.string.update_download_hint
            }),
            style = MaterialTheme.typography.bodyMedium,
            color = if (download.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else FreshendaColors.Unknown,
        )
        UpdateError(feedback)
    }
}

@Composable
private fun UpdateError(feedback: UpdateFeedback?) {
    val message = when (feedback) {
        UpdateFeedback.UNAVAILABLE -> R.string.update_unavailable
        UpdateFeedback.FAILED -> R.string.update_failed
        UpdateFeedback.IGNORE_FAILED -> R.string.update_ignore_failed
        UpdateFeedback.INSTALL_PERMISSION -> R.string.update_install_permission
        UpdateFeedback.INSTALL_FAILED -> R.string.update_install_failed
        null -> null
    }
    message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun VersionLabel(label: String, version: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FreshendaColors.Unknown)
        Text(version, style = MaterialTheme.typography.titleLarge, color = FreshendaColors.Primary)
    }
}
