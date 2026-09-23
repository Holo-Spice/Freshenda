package com.cake.freshenda.update

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cake.freshenda.AppContainer
import com.cake.freshenda.BuildConfig
import com.cake.freshenda.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateFeedback { UP_TO_DATE, INCOMPATIBLE, UNAVAILABLE, FAILED, IGNORE_FAILED }

data class UpdateUiState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    val feedback: UpdateFeedback? = null,
    val requiredSdk: Int? = null,
)

class UpdateViewModel(
    private val checker: UpdateChecker,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val state = MutableStateFlow(UpdateUiState())
    val uiState = state.asStateFlow()
    private var checkJob: Job? = null

    fun check(manual: Boolean = false) {
        if (checkJob?.isActive == true || (!manual && state.value.available != null)) return
        checkJob = viewModelScope.launch {
            try {
                val preferences = settings.updatePreferences.first()
                val now = System.currentTimeMillis()
                if (!manual && !UpdatePolicy.shouldCheck(now, preferences.lastCheckEpochMillis)) return@launch
                state.update { it.copy(checking = true, feedback = null, requiredSdk = null) }
                // 失败也记录检查时间，避免离线时反复重试；手动检查不受此限制。
                settings.recordUpdateCheck(now)
                val info = checker.check()
                when (UpdatePolicy.evaluate(info, BuildConfig.VERSION_CODE.toLong(), Build.VERSION.SDK_INT, preferences.ignoredVersionCode, manual)) {
                    UpdateResult.AVAILABLE -> state.update { it.copy(available = info) }
                    UpdateResult.UP_TO_DATE -> if (manual) state.update { it.copy(feedback = UpdateFeedback.UP_TO_DATE) }
                    UpdateResult.INCOMPATIBLE -> if (manual) state.update { it.copy(feedback = UpdateFeedback.INCOMPATIBLE, requiredSdk = info.minSdk) }
                    UpdateResult.IGNORED -> Unit
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

    fun dismissUpdate() { state.update { it.copy(available = null) } }

    fun ignoreUpdate() {
        val version = state.value.available?.versionCode ?: return
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
            UpdateViewModel(container.updateChecker, container.settingsRepository) as T
    }
}
