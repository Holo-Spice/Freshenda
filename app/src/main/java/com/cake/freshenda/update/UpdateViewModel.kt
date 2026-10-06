package com.cake.freshenda.update

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cake.freshenda.AppContainer
import com.cake.freshenda.BuildConfig
import com.cake.freshenda.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateFeedback { UNAVAILABLE, FAILED, IGNORE_FAILED, INSTALL_PERMISSION, INSTALL_FAILED }

data class UpdateUiState(
    val checking: Boolean = false,
    val latest: UpdateInfo? = null,
    val result: UpdateResult? = null,
    val dialogVisible: Boolean = false,
    val feedback: UpdateFeedback? = null,
    val download: DownloadProgress = DownloadProgress(),
    val installRequested: Boolean = false,
)

class UpdateViewModel(
    private val checker: UpdateChecker,
    private val settings: SettingsRepository,
    private val downloader: UpdateDownloader,
) : ViewModel() {
    private val state = MutableStateFlow(UpdateUiState())
    val uiState = state.asStateFlow()
    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var operationJob: Job? = null
    private var pending: PendingUpdate? = null
    private var foreground = false
    private val restoreJob = viewModelScope.launch {
        try {
            val saved = settings.pendingUpdate.first() ?: return@launch
            if (saved.info.versionCode <= BuildConfig.VERSION_CODE) {
                downloader.cancel(saved.downloadId)
                settings.setPendingUpdate(null)
            } else {
                pending = saved
                state.update { it.copy(latest = saved.info, result = UpdateResult.AVAILABLE, download = DownloadProgress(DownloadStatus.DOWNLOADING)) }
                observeDownload()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state.update { it.copy(feedback = UpdateFeedback.FAILED) }
        }
    }

    fun setForeground(value: Boolean) {
        foreground = value
        if (value) observeDownload() else downloadJob?.cancel()
    }

    fun check(manual: Boolean = false) {
        if (checkJob?.isActive == true || (!manual && state.value.dialogVisible)) return
        checkJob = viewModelScope.launch {
            try {
                restoreJob.join()
                if (pending != null || state.value.download.active) {
                    if (manual) state.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                if (manual) state.value = UpdateUiState(checking = true, dialogVisible = true)
                val preferences = settings.updatePreferences.first()
                val now = System.currentTimeMillis()
                if (!manual && !UpdatePolicy.shouldCheck(now, preferences.lastCheckEpochMillis)) return@launch
                state.update { it.copy(checking = true, feedback = null) }
                // 失败也记录检查时间，避免离线时反复重试；手动检查不受此限制。
                settings.recordUpdateCheck(now)
                val info = checker.check()
                val result = UpdatePolicy.evaluate(info, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, preferences.ignoredVersionCode, manual)
                state.update {
                    it.copy(
                        latest = info,
                        result = result,
                        dialogVisible = it.dialogVisible || (!manual && result == UpdateResult.AVAILABLE),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: UpdateMetadataUnavailable) {
                if (manual) state.update { it.copy(feedback = UpdateFeedback.UNAVAILABLE) }
            } catch (_: Exception) {
                if (manual) state.update { it.copy(feedback = UpdateFeedback.FAILED) }
            } finally {
                state.update { it.copy(checking = false) }
            }
        }
    }

    fun dismissUpdate() { state.update { it.copy(dialogVisible = false, installRequested = false) } }

    fun downloadUpdate() {
        val info = state.value.latest ?: return
        if (state.value.download.active || operationJob?.isActive == true) return
        if (info.apkUrl == null) {
            state.update { it.copy(feedback = UpdateFeedback.UNAVAILABLE) }
            return
        }
        state.update { it.copy(download = DownloadProgress(DownloadStatus.STARTING), feedback = null) }
        operationJob = viewModelScope.launch {
            try {
                downloadJob?.cancel()
                pending?.let { downloader.cancel(it.downloadId) }
                val download = PendingUpdate(downloader.start(info), info)
                pending = download
                settings.setPendingUpdate(download)
                state.update { it.copy(download = DownloadProgress(DownloadStatus.DOWNLOADING)) }
                observeDownload()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(download = DownloadProgress(DownloadStatus.FAILED, failure = DownloadFailure.NETWORK)) }
            }
        }
    }

    fun cancelDownload() {
        if (operationJob?.isActive == true) return
        downloadJob?.cancel()
        operationJob = viewModelScope.launch {
            try {
                pending?.let { downloader.cancel(it.downloadId) }
                settings.setPendingUpdate(null)
                pending = null
                state.update { it.copy(download = DownloadProgress(), feedback = null, installRequested = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(download = DownloadProgress(DownloadStatus.FAILED, failure = DownloadFailure.NETWORK)) }
            }
        }
    }

    private fun observeDownload() {
        val download = pending ?: return
        if (!foreground || downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            try {
                while (isActive) {
                    val progress = downloader.progress(download.downloadId)
                    state.update {
                        it.copy(
                            download = progress,
                            installRequested = it.installRequested ||
                                (progress.status == DownloadStatus.READY && it.download.active && it.dialogVisible),
                        )
                    }
                    if (!progress.active) break
                    delay(500)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(download = DownloadProgress(DownloadStatus.FAILED, failure = DownloadFailure.NETWORK)) }
            }
        }
    }

    fun consumeInstallRequest() { state.update { it.copy(installRequested = false, feedback = null) } }
    fun installFeedback(feedback: UpdateFeedback) { state.update { it.copy(feedback = feedback) } }
    suspend fun installIntent(): Intent? {
        restoreJob.join()
        return pending?.let { downloader.installIntent(it.downloadId) }
    }

    fun ignoreUpdate() {
        val version = state.value.latest?.versionCode ?: return
        dismissUpdate()
        viewModelScope.launch {
            try {
                settings.ignoreUpdateVersion(version)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(feedback = UpdateFeedback.IGNORE_FAILED) }
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UpdateViewModel(container.updateChecker, container.settingsRepository, container.updateDownloader) as T
    }
}
