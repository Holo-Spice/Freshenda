package com.cake.freshenda.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cake.freshenda.R
import com.cake.freshenda.update.UpdateInfo

@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit, onIgnore: () -> Unit) {
    val context = LocalContext.current
    var browserFailed by remember(info.versionCode) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title, info.versionName)) },
        text = {
            Column(
                Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (info.notes.isEmpty()) Text(stringResource(R.string.update_notes_empty))
                info.notes.forEach { Text("• $it") }
                if (browserFailed) Text(stringResource(R.string.update_browser_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                    onDismiss()
                } catch (_: ActivityNotFoundException) {
                    browserFailed = true
                } catch (_: SecurityException) {
                    browserFailed = true
                }
            }) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            TextButton(onClick = onIgnore) { Text(stringResource(R.string.update_ignore)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) }
        },
    )
}
