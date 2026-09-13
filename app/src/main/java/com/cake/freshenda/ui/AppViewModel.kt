package com.cake.freshenda.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cake.freshenda.AppContainer
import com.cake.freshenda.data.BackupEnvelope
import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.data.local.BatchChangeEntity
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.AddBatchDraft
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.model.StorageLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppUiState(
    val catalog: FoodCatalog? = null,
    val batches: List<FoodBatchEntity> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val customFoods: List<CustomFoodEntity> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

data class PendingImport(val envelope: BackupEnvelope)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val catalog = MutableStateFlow<FoodCatalog?>(null)
    private val loadError = MutableStateFlow<String?>(null)
    private val selectedId = MutableStateFlow<Long?>(null)
    private val message = MutableStateFlow<String?>(null)
    val pendingImport = MutableStateFlow<PendingImport?>(null)

    private val catalogAndInventory = combine(
        catalog,
        container.foodRepository.observeActiveBatches(),
        container.foodRepository.observeCustomFoods(),
    ) { loadedCatalog, batches, customFoods -> Triple(loadedCatalog, batches, customFoods) }

    val uiState: StateFlow<AppUiState> = combine(
        catalogAndInventory,
        container.settingsRepository.settings,
        loadError,
    ) { inventory, settings, error ->
        AppUiState(inventory.first, inventory.second, settings, inventory.third, inventory.first == null && error == null, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    val selectedBatch: StateFlow<FoodBatchEntity?> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(null) else container.foodRepository.observeBatch(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentChanges: StateFlow<List<BatchChangeEntity>> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else container.foodRepository.observeRecentChanges(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snackbarMessage: StateFlow<String?> = message

    init {
        viewModelScope.launch {
            try {
                catalog.value = container.catalogLoader.load()
            } catch (error: Exception) {
                loadError.value = "目录加载失败：${error.message ?: "未知错误"}"
            }
        }
    }

    fun selectBatch(id: Long?) { selectedId.value = id }

    fun addBatch(draft: AddBatchDraft, onSaved: (Long) -> Unit) {
        val currentCatalog = catalog.value ?: return
        viewModelScope.launch {
            try {
                val id = container.foodRepository.addBatch(draft, currentCatalog)
                container.settingsRepository.recordRecent(draft.food.id)
                onSaved(id)
            } catch (error: Exception) {
                message.value = "保存失败：${error.message ?: "请检查输入"}"
            }
        }
    }

    fun updateBatch(id: Long, draft: AddBatchDraft, onSuccess: () -> Unit) {
        val currentCatalog = catalog.value ?: return
        launchAction("已保存修改", onSuccess) {
            container.foodRepository.updateBatch(id, draft, currentCatalog)
        }
    }

    fun consume(id: Long, quantityMilli: Long, onSuccess: () -> Unit = {}) =
        launchAction("已更新剩余数量", onSuccess) { container.foodRepository.consume(id, quantityMilli) }
    fun markOpened(id: Long) = launchAction("已记录开封") {
        val currentCatalog = catalog.value ?: error("食材目录尚未加载")
        container.foodRepository.markOpened(id, currentCatalog)
    }
    fun discard(id: Long, onSuccess: () -> Unit = {}) =
        launchAction("已移出冰箱", onSuccess) { container.foodRepository.discard(id) }
    fun moveBatch(id: Long, target: StorageLocation) = launchAction("已记录新的存放阶段") { container.foodRepository.moveBatch(id, target) }
    fun splitAndMove(id: Long, quantityMilli: Long, target: StorageLocation, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                val childId = container.foodRepository.splitAndMove(id, quantityMilli, target)
                message.value = "已拆分批次并记录新位置"
                selectedId.value = childId
                onSaved(childId)
            } catch (error: Exception) {
                message.value = error.message ?: "拆分失败"
            }
        }
    }
    fun setCustomDate(id: Long, dateEpochDay: Long) = launchAction("已更新自己的计划日期") { container.foodRepository.setCustomDate(id, dateEpochDay) }
    fun setRemindersEnabled(value: Boolean) = launchAction(null) { container.settingsRepository.setRemindersEnabled(value) }
    fun setDailySummary(value: Boolean) = launchAction(null) { container.settingsRepository.setDailySummary(value) }
    fun setOpenedReminders(value: Boolean) = launchAction(null) { container.settingsRepository.setOpenedReminders(value) }
    fun setCustomDateReminders(value: Boolean) = launchAction(null) { container.settingsRepository.setCustomDateReminders(value) }
    fun setReminderHour(value: Int) = launchAction(null) { container.settingsRepository.setReminderHour(value) }

    fun sendTestNotification() {
        val sent = container.notificationPublisher.publishSummary(emptyList(), test = true)
        message.value = if (sent) "测试通知已交给系统" else "系统通知未开启，请检查权限和通知渠道"
    }

    fun exportBackup(uri: Uri) = launchAction("备份已导出") { container.backupManager.exportTo(uri) }
    fun inspectBackup(uri: Uri) = viewModelScope.launch {
        try {
            pendingImport.value = PendingImport(container.backupManager.inspect(uri))
        } catch (error: Exception) {
            message.value = "导入失败：${error.message ?: "文件格式不正确"}"
        }
    }

    fun confirmImport() = viewModelScope.launch {
        val pending = pendingImport.value ?: return@launch
        try {
            val count = container.backupManager.replaceWith(pending.envelope)
            pendingImport.value = null
            message.value = "已恢复 $count 个在库批次"
        } catch (error: Exception) {
            message.value = "恢复失败：${error.message ?: "请重试"}"
        }
    }

    fun cancelImport() { pendingImport.value = null }

    fun consumeMessage() { message.value = null }

    private fun launchAction(success: String?, onSuccess: () -> Unit = {}, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
            if (success != null) message.value = success
            onSuccess()
        } catch (error: Exception) {
            message.value = error.message ?: "操作失败"
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
    }
}
