package com.cake.freshenda.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
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
import com.cake.freshenda.update.UpdateFeedback
import com.cake.freshenda.update.UpdateResult
import com.cake.freshenda.update.UpdateUiState

@Composable
fun UpdateDialog(state: UpdateUiState, onDismiss: () -> Unit, onIgnore: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val info = state.latest
    val canUpdate = state.result == UpdateResult.AVAILABLE
    var expanded by remember(info?.versionCode) { mutableStateOf(false) }
    var browserFailed by remember(info?.versionCode) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FreshendaColors.Card,
        title = {
            Text(stringResource(when {
                state.checking -> R.string.update_checking
                canUpdate -> R.string.update_found
                state.result == UpdateResult.UP_TO_DATE -> R.string.update_latest
                else -> R.string.update_section
            }))
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
                    targetState = state.checking,
                    transitionSpec = { fadeIn(tween(160, easing = MotionEase)) togetherWith fadeOut(tween(100)) },
                    label = "updateResult",
                ) { checking ->
                    if (checking) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        Column(
                            Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (state.result == UpdateResult.INCOMPATIBLE) {
                                Text(stringResource(R.string.update_incompatible, info?.minSdk ?: 0), color = MaterialTheme.colorScheme.error)
                            }
                            val error = when (state.feedback) {
                                UpdateFeedback.UNAVAILABLE -> R.string.update_unavailable
                                UpdateFeedback.FAILED -> R.string.update_failed
                                UpdateFeedback.IGNORE_FAILED -> R.string.update_ignore_failed
                                null -> null
                            }
                            error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
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
                                if (info.details.isNotEmpty()) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Text(stringResource(if (expanded) R.string.update_collapse else R.string.update_details))
                                    }
                                }
                            }
                            if (browserFailed) Text(stringResource(R.string.update_browser_failed), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                canUpdate && info != null -> Button(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                        onDismiss()
                    } catch (_: ActivityNotFoundException) {
                        browserFailed = true
                    } catch (_: SecurityException) {
                        browserFailed = true
                    }
                }) { Text(stringResource(R.string.update_download)) }
                state.feedback != null -> Button(onClick = onRetry) { Text(stringResource(R.string.update_retry)) }
                else -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_close)) }
            }
        },
        dismissButton = {
            if (canUpdate) {
                Row {
                    TextButton(onClick = onIgnore) { Text(stringResource(R.string.update_ignore)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
                }
            }
        },
    )
}

@Composable
private fun VersionLabel(label: String, version: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FreshendaColors.Unknown)
        Text(version, style = MaterialTheme.typography.titleLarge, color = FreshendaColors.Primary)
    }
}