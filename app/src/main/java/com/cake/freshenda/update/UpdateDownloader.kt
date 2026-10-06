package com.cake.freshenda.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class DownloadStatus { IDLE, STARTING, DOWNLOADING, WAITING, READY, FAILED }
enum class DownloadFailure { NETWORK, STORAGE, MISSING }

data class DownloadProgress(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val failure: DownloadFailure? = null,
) {
    val active: Boolean get() = status == DownloadStatus.STARTING || status == DownloadStatus.DOWNLOADING || status == DownloadStatus.WAITING
    val fraction: Float? get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}

class UpdateDownloader(private val context: Context) {
    private val manager = context.getSystemService(DownloadManager::class.java)

    suspend fun start(info: UpdateInfo): Long = withContext(Dispatchers.IO) {
        val url = requireNotNull(info.apkUrl) { "This release has no APK URL" }
        // A single app-owned download; DownloadManager handles redirects and network interruptions.
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_NAME).delete()
        manager.enqueue(
            DownloadManager.Request(Uri.parse(url))
                .setTitle("鲜序 ${info.versionName}")
                .setDescription("应用更新")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_NAME),
        )
    }

    suspend fun progress(id: Long): DownloadProgress = withContext(Dispatchers.IO) {
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext DownloadProgress(DownloadStatus.FAILED, failure = DownloadFailure.MISSING)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            DownloadProgress(
                status = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.READY
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> DownloadStatus.WAITING
                    else -> DownloadStatus.DOWNLOADING
                },
                downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                failure = if (status == DownloadManager.STATUS_FAILED) {
                    if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE || reason == DownloadManager.ERROR_DEVICE_NOT_FOUND) DownloadFailure.STORAGE
                    else DownloadFailure.NETWORK
                } else null,
            )
        }
    }

    suspend fun cancel(id: Long) = withContext(Dispatchers.IO) { manager.remove(id); Unit }

    fun installIntent(id: Long): Intent? = manager.getUriForDownloadedFile(id)?.let { uri ->
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private companion object {
        const val APK_NAME = "Freshenda-update.apk"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
