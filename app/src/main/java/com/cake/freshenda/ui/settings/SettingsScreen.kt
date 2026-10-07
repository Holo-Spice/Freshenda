package com.cake.freshenda.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.cake.freshenda.ui.components.SecondaryButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cake.freshenda.BuildConfig
import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.R
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.SectionCard
import com.cake.freshenda.ui.components.UiIcon
import com.cake.freshenda.ui.components.UiSymbol
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.update.UpdateFeedback
import com.cake.freshenda.update.DownloadStatus
import com.cake.freshenda.update.UpdateUiState

@Composable
fun SettingsScreen(
    settings: UserSettings,
    catalog: FoodCatalog?,
    notificationsAllowed: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onDailySummary: (Boolean) -> Unit,
    onOpened: (Boolean) -> Unit,
    onCustom: (Boolean) -> Unit,
    onHour: (Int) -> Unit,
    onTest: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    updateState: UpdateUiState,
    onCheckUpdate: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { BrandHeader("设置", "在合适的时候，轻轻提醒。") }
        item {
            SettingCard("提醒方式") {
                SettingToggle("到期提醒", if (notificationsAllowed) "系统通知可用" else "系统通知未开启", settings.remindersEnabled) { value ->
                    if (value && !notificationsAllowed) onRequestPermission()
                    onSetEnabled(value)
                }
                SettingToggle("每日汇总", "关闭时仅提醒临期食材", settings.dailySummary, onDailySummary)
            }
        }
        item {
            SettingCard("提醒时间") {
                Text("偏好时段", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onHour((settings.reminderHour + 23) % 24) }, modifier = Modifier.semantics { contentDescription = "提前一小时" }) { UiIcon(UiSymbol.REMOVE) }
                    Text("%02d:00".format(settings.reminderHour), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"))
                    IconButton(onClick = { onHour((settings.reminderHour + 1) % 24) }, modifier = Modifier.semantics { contentDescription = "推后一小时" }) { UiIcon(UiSymbol.ADD) }
                }
                Text("普通日期提前 ${settings.normalAdvanceDays} 天，冷冻品质提前 ${settings.frozenAdvanceDays} 天。提醒可能受系统省电限制，无法保证准点。", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SettingCard("日期范围") {
                SettingToggle("开封后提醒", "仅在有明确开封日期时使用", settings.openedReminders, onOpened)
                SettingToggle("自定日期提醒", "提醒你自己设定的日期", settings.customDateReminders, onCustom)
            }
        }
        item {
            SettingCard("系统与数据") {
                OutlinedButton(onClick = onTest, modifier = Modifier.fillMaxWidth()) { Text("发送测试通知") }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("导出备份") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("从备份恢复") }
                Text("恢复前可查看备份时间与食材数量，确认后替换当前库存。", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
            }
        }
        item {
            SettingCard("资料说明") {
                Text(catalog?.let { "随 App 离线提供 ${it.foods.size} 个食材条目、${it.profiles.size} 个规则档案和 ${it.sources.size} 个资料来源。日期是储存与安排提醒，不是对实物安全的保证。" } ?: "正在读取本地食材目录…", style = MaterialTheme.typography.bodyMedium)
                Text("数据版本：${catalog?.dataVersion ?: "加载中"}", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
            }
        }
        item {
            SettingCard(stringResource(R.string.update_section)) {
                Text(stringResource(R.string.update_current_version, BuildConfig.VERSION_NAME))
                OutlinedButton(onClick = onCheckUpdate, enabled = !updateState.checking, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(when {
                        updateState.checking -> R.string.update_checking
                        updateState.download.active -> R.string.update_view_progress
                        updateState.download.status == DownloadStatus.READY -> R.string.update_install
                        else -> R.string.update_check
                    }))
                }
                val feedback = when (updateState.feedback) {
                    UpdateFeedback.UNAVAILABLE -> stringResource(R.string.update_unavailable)
                    UpdateFeedback.FAILED -> stringResource(R.string.update_failed)
                    UpdateFeedback.IGNORE_FAILED -> stringResource(R.string.update_ignore_failed)
                    UpdateFeedback.INSTALL_PERMISSION -> stringResource(R.string.update_install_permission)
                    UpdateFeedback.INSTALL_FAILED -> stringResource(R.string.update_install_failed)
                    null -> null
                }
                feedback?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionCard(title, content)
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChecked), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.labelLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = FreshendaColors.Unknown) }
        Switch(checked = checked, onCheckedChange = null)
    }
}

