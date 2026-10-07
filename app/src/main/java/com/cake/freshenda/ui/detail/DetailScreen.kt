package com.cake.freshenda.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.cake.freshenda.ui.components.FreshendaButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.BatchChangeEntity
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.StorageLocation
import com.cake.freshenda.model.StorageWindow
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.FoodDatePicker
import com.cake.freshenda.ui.components.BatchIcon
import com.cake.freshenda.ui.components.locationText
import com.cake.freshenda.ui.components.quantityText
import com.cake.freshenda.ui.components.StatusBadge
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.ui.theme.FreshendaSpacing
import com.cake.freshenda.ui.components.BackButton
import com.cake.freshenda.ui.components.SectionCard
import com.cake.freshenda.ui.components.UiIcon
import com.cake.freshenda.ui.components.UiSymbol
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.serialization.json.Json

@Composable
fun SharedTransitionScope.DetailScreen(
    batch: FoodBatchEntity?,
    changes: List<BatchChangeEntity>,
    now: Long,
    source: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onConsume: (quantityMilli: Long, consumeAll: Boolean) -> Unit,
    onOpen: () -> Unit,
    onDiscard: () -> Unit,
    onMove: (StorageLocation) -> Unit,
    onSplit: (Long, StorageLocation) -> Unit,
    onSetDate: (Long) -> Unit,
) {
    if (batch == null) {
        BrandHeader("食材详情", "正在读取批次", leading = { BackButton(onBack) })
        return
    }
    val result = remember(batch, now) { ExpiryCalculator.result(batch, now) }
    val window = remember(batch.ruleSnapshot) {
        batch.ruleSnapshot?.let { runCatching { Json.decodeFromString<StorageWindow>(it) }.getOrNull() }
    }
    var showSource by rememberSaveable(batch.id) { mutableStateOf(false) }
    var showDateDialog by rememberSaveable(batch.id) { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(batch.id) { mutableStateOf(false) }
    val moveTarget = if (batch.storageLocation == StorageLocation.FROZEN.name) StorageLocation.REFRIGERATED else StorageLocation.FROZEN

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Text("食材详情", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = FreshendaColors.Unknown, textAlign = TextAlign.Center)
                TextButton(onClick = onEdit) { Text("编辑") }
            }
            Row(Modifier.fillMaxWidth().padding(FreshendaSpacing.Page), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                BatchIcon(batch, result, source = source, size = 96.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(batch.displayName, style = MaterialTheme.typography.headlineMedium)
                    StatusBadge(result.statusText, result.status)
                    Text(result.effective?.description ?: "尚未选择日期依据", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
                }
            }
            DetailCard("储存记录") {
                InfoRow("剩余数量", "${quantityText(batch.quantityMilli)} ${batch.quantityUnit}")
                InfoRow("存放位置", "${locationText(batch.storageLocation)} · ${sectionText(batch.storageSection)}")
                InfoRow("包装状态", packagingText(batch.packagingState))
                InfoRow("本阶段起点", formatInstant(batch.stageStartedAtEpochMillis, batch.businessZoneId))
                InfoRow("目标日期", batch.effectiveDisplayDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "未设日期")
                if (batch.requiresDateReview) Text("存放状态已改变，请重新确认日期。", color = FreshendaColors.DueSoon, style = MaterialTheme.typography.bodyMedium)
            }
            DetailCard("批次操作") {
                if (batch.packagingState != "OPENED") DetailAction("记录开封", onOpen)
                DetailAction(if (moveTarget == StorageLocation.FROZEN) "整批转冷冻" else "开始冷藏解冻") { onMove(moveTarget) }
                if (batch.quantityMilli > 1L) {
                    DetailAction("拆分一半并${if (moveTarget == StorageLocation.FROZEN) "转冷冻" else "冷藏解冻"}") { onSplit(batch.quantityMilli / 2, moveTarget) }
                }
                DetailAction("设置计划日期") { showDateDialog = true }
            }
            DetailCard("储存依据") {
                DetailAction(if (showSource) "收起依据与条件" else "查看依据与条件") { showSource = !showSource }
                AnimatedVisibility(
                    showSource,
                    enter = expandVertically(tween(200, easing = MotionEase)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(200, easing = MotionEase)) + fadeOut(tween(100)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(window?.condition ?: "本批次没有采用储存参考，请按包装说明或自己的计划设置日期。", style = MaterialTheme.typography.bodyMedium)
                        window?.selectionPolicy?.takeIf(String::isNotBlank)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
                        }
                    }
                }
            }
            if (changes.isNotEmpty()) {
                DetailCard("最近变更") {
                    changes.forEach { change ->
                        Text(change.note, style = MaterialTheme.typography.bodyMedium)
                        Text(formatInstant(change.changedAtEpochMillis, batch.businessZoneId), style = MaterialTheme.typography.labelSmall, color = FreshendaColors.Unknown)
                    }
                }
            }
            TextButton(onClick = { showDiscardDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page, vertical = 8.dp)) {
                Text("标记丢弃", color = FreshendaColors.Overdue)
            }
        }
        Surface(color = FreshendaColors.Background) {
            Button(
                onClick = {
                    val quantity = minOf(1000L, batch.quantityMilli)
                    onConsume(quantity, quantity == batch.quantityMilli)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page, vertical = 12.dp),
            ) { Text(if (batch.quantityMilli <= 1000L) "全部吃完" else "吃掉 1 ${batch.quantityUnit}") }
        }
    }
    if (showDateDialog) {
        FoodDatePicker(
            selectedDate = batch.effectiveDisplayDateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now().plusDays(1),
            onDismiss = { showDateDialog = false },
        ) { day -> showDateDialog = false; onSetDate(day.toEpochDay()) }
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("丢弃这份${batch.displayName}？") },
            text = { Text("剩余 ${quantityText(batch.quantityMilli)} ${batch.quantityUnit} 将从冰箱中移出。") },
            confirmButton = { TextButton(onClick = { showDiscardDialog = false; onDiscard() }) { Text("确认丢弃", color = FreshendaColors.Overdue) } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("保留") } },
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    SectionCard(title, content)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
        Text(value, Modifier.weight(1.5f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
    }
}

@Composable
private fun DetailAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, Modifier.weight(1f), textAlign = TextAlign.Start)
        UiIcon(UiSymbol.NEXT)
    }
}

private fun formatInstant(epochMillis: Long, zoneId: String) = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zoneId)).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
private fun packagingText(value: String) = when (value) { "LOOSE" -> "散装"; "UNOPENED" -> "未开封"; "OPENED" -> "已开封"; else -> value }
private fun sectionText(value: String) = when (value) { "UPPER" -> "上层"; "LOWER" -> "下层"; "CRISPER" -> "果蔬抽屉"; "FREEZER_DRAWER" -> "冷冻抽屉"; "SHELF" -> "置物区"; else -> value }
