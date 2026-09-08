package com.cake.freshenda.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.BatchChangeEntity
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.FoodIcon
import com.cake.freshenda.ui.components.StatusBadge
import com.cake.freshenda.ui.components.locationText
import com.cake.freshenda.ui.components.quantityText
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.model.StorageLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DetailScreen(
    batch: FoodBatchEntity?,
    changes: List<BatchChangeEntity>,
    now: Long,
    onBack: () -> Unit,
    onConsume: (Long) -> Unit,
    onOpen: () -> Unit,
    onDiscard: () -> Unit,
    onMove: (StorageLocation) -> Unit,
    onSplit: (Long, StorageLocation) -> Unit,
    onSetDate: (Long) -> Unit,
) {
    if (batch == null) {
        Column(Modifier.fillMaxSize()) { BrandHeader("食材详情", "正在读取批次", leading = { TextButton(onClick = onBack) { Text("‹ 返回", color = FreshendaColors.OnPrimary) } }) }
        return
    }
    val result = ExpiryCalculator.result(batch, now)
    var showSource by remember { mutableStateOf(false) }
    var showDateDialog by rememberSaveable { mutableStateOf(false) }
    val moveTarget = if (batch.storageLocation == StorageLocation.FROZEN.name) StorageLocation.REFRIGERATED else StorageLocation.FROZEN
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        BrandHeader("食材详情", locationText(batch.storageLocation), leading = { TextButton(onClick = onBack) { Text("‹ 返回", color = FreshendaColors.OnPrimary) } })
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FoodIcon(batch.iconKey, batch.displayName, Modifier.fillMaxWidth(), 132.dp, result.fractionRemaining, result.status)
            Text(batch.displayName, style = MaterialTheme.typography.headlineMedium)
            StatusBadge(result.statusText, result.status)
            Text("${result.effective?.description ?: "尚无日期依据"} · ${formatTarget(batch)}", style = MaterialTheme.typography.bodyLarge)
        }
        Timeline(batch, now)
        InfoCard("剩余数量", "${quantityText(batch.quantityMilli)} ${batch.quantityUnit}")
        InfoCard("存放位置", "${locationText(batch.storageLocation)} · ${sectionText(batch.storageSection)}")
        InfoCard("包装状态", packagingText(batch.packagingState) + if (batch.requiresDateReview) " · 日期待确认" else "")
        Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = FreshendaColors.Card, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp)) {
                TextButton(onClick = { showSource = !showSource }) { Text(if (showSource) "收起储存依据" else "查看储存依据与条件") }
                if (showSource) Text(batch.ruleSnapshot ?: "本批次没有自动采用储存参考；请按包装或自己设定。", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onConsume(minOf(1000L, batch.quantityMilli)) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (batch.quantityMilli <= 1000L) "全部吃完" else "吃掉 1 ${batch.quantityUnit}") }
            if (batch.packagingState != "OPENED") OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("记录开封") }
            OutlinedButton(onClick = { onMove(moveTarget) }, modifier = Modifier.fillMaxWidth()) { Text(if (moveTarget == StorageLocation.FROZEN) "整批转冷冻" else "开始冷藏解冻") }
            if (batch.quantityMilli > 1L) {
                OutlinedButton(onClick = { onSplit(batch.quantityMilli / 2, moveTarget) }, modifier = Modifier.fillMaxWidth()) { Text("拆分一半并${if (moveTarget == StorageLocation.FROZEN) "转冷冻" else "冷藏解冻"}") }
            }
            OutlinedButton(onClick = { showDateDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("设置自己的计划日期") }
            OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("标记丢弃") }
        }
        if (changes.isNotEmpty()) {
            Text("最近变更", modifier = Modifier.padding(horizontal = 18.dp), style = MaterialTheme.typography.titleMedium)
            changes.forEach { change -> Text("${formatInstant(change.changedAtEpochMillis, batch.businessZoneId)}  ${change.note}", modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium) }
        }
        Spacer(Modifier.height(20.dp))
    }
    if (showDateDialog) {
        CustomDateDialog(
            initial = batch.effectiveDisplayDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: LocalDate.now().plusDays(1).toString(),
            onDismiss = { showDateDialog = false },
        ) { day -> showDateDialog = false; onSetDate(day) }
    }
}

@Composable
private fun CustomDateDialog(initial: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    val parsed = runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自己的计划日期") },
        text = { OutlinedTextField(value = value, onValueChange = { value = it.take(10) }, label = { Text("yyyy-MM-dd") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun Timeline(batch: FoodBatchEntity, now: Long) {
    val zone = ZoneId.of(batch.businessZoneId)
    val start = Instant.ofEpochMilli(batch.stageStartedAtEpochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val target = batch.effectiveDisplayDateEpochDay?.let(LocalDate::ofEpochDay)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = FreshendaColors.Card, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                drawLine(FreshendaColors.Secondary, Offset(12.dp.toPx(), center.y), Offset(size.width - 12.dp.toPx(), center.y), 3.dp.toPx())
                drawCircle(FreshendaColors.Primary, 6.dp.toPx(), Offset(12.dp.toPx(), center.y))
                drawCircle(FreshendaColors.Primary, 6.dp.toPx(), Offset(size.width / 2, center.y))
                drawCircle(if (target == null) FreshendaColors.Unknown else FreshendaColors.DueSoon, 6.dp.toPx(), Offset(size.width - 12.dp.toPx(), center.y))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${start.format(SHORT)}\n起点", style = MaterialTheme.typography.bodyMedium)
                Text("${today.format(SHORT)}\n今天", style = MaterialTheme.typography.bodyMedium)
                Text("${target?.format(SHORT) ?: "--"}\n目标", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = FreshendaColors.Card, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, style = MaterialTheme.typography.labelLarge) }
    }
}

private val SHORT = DateTimeFormatter.ofPattern("MM.dd")
private fun formatTarget(batch: FoodBatchEntity) = batch.effectiveDisplayDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "未设日期"
private fun formatInstant(epochMillis: Long, zoneId: String) = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
private fun packagingText(value: String) = when (value) { "LOOSE" -> "散装"; "UNOPENED" -> "未开封"; "OPENED" -> "已开封"; else -> value }
private fun sectionText(value: String) = when (value) { "UPPER" -> "上层"; "LOWER" -> "下层"; "CRISPER" -> "果蔬抽屉"; "FREEZER_DRAWER" -> "冷冻抽屉"; "SHELF" -> "置物区"; else -> value }
