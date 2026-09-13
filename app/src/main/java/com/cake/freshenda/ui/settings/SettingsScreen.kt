package com.cake.freshenda.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.theme.FreshendaColors

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
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { BrandHeader("提醒与偏好", "在合适的时候，轻轻提醒") }
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("偏好时段"); Text("系统允许时在此时间后检查", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onHour((settings.reminderHour + 23) % 24) }) { Text("−") }
                        Text("%02d:00".format(settings.reminderHour), modifier = Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { onHour((settings.reminderHour + 1) % 24) }) { Text("＋") }
                    }
                }
                Text("普通日期提前 ${settings.normalAdvanceDays} 天；冷冻品质提前 ${settings.frozenAdvanceDays} 天。后台任务可能受省电、Doze、厂商限制或强制停止影响，不能保证准点。", style = MaterialTheme.typography.bodyMedium)
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
                Button(onClick = onTest, modifier = Modifier.fillMaxWidth()) { Text("发送测试通知") }
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("导出本地 JSON 备份") }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("从 JSON 替换恢复") }
                Text("导入会先完整解析并校验，成功后才替换当前库存。", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
            }
        }
        item {
            SettingCard("资料说明") {
                Text("随 App 离线提供 ${catalog?.foods?.size ?: 322} 个食材条目、${catalog?.profiles?.size ?: 81} 个规则档案和 ${catalog?.sources?.size ?: 21} 个资料来源。日期是储存与安排提醒，不是对实物安全的保证。", style = MaterialTheme.typography.bodyMedium)
                Text("数据版本：${catalog?.dataVersion ?: "加载中"}", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(title, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
        Surface(
            color = FreshendaColors.Glass,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, FreshendaColors.GlassBorder),
            shadowElevation = 2.dp,
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
