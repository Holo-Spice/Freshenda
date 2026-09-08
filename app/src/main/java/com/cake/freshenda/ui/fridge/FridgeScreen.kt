package com.cake.freshenda.ui.fridge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.EmptyState
import com.cake.freshenda.ui.components.FoodIcon
import com.cake.freshenda.ui.components.batchExpiry
import com.cake.freshenda.ui.theme.FreshendaColors

@Composable
fun FridgeScreen(
    batches: List<FoodBatchEntity>,
    now: Long,
    onAdd: () -> Unit,
    onBatch: (Long) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val filtered = if (filter == "ALL") batches else batches.filter { it.storageLocation == filter }
    val urgent = batches.count { batchExpiry(it, now).status in setOf(FreshnessStatus.OVERDUE, FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                BrandHeader("我的冰箱", "${batches.size} 份食材 · $urgent 份优先安排")
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StorageFilter("全部", "ALL", filter) { filter = it }
                    StorageFilter("冷藏", "REFRIGERATED", filter) { filter = it }
                    StorageFilter("冷冻", "FROZEN", filter) { filter = it }
                    StorageFilter("常温", "PANTRY", filter) { filter = it }
                }
            }
            if (filtered.isEmpty()) {
                item { EmptyState("冰箱还是空的", if (batches.isEmpty()) "添加第一份食材，日期与批次会保存在本机" else "这个位置暂时没有食材") }
            } else {
                val groups = filtered.groupBy { it.storageSection }
                groups.forEach { (section, sectionBatches) ->
                    item {
                        FridgeShelf(sectionName(section), sectionBatches, now, onBatch)
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = FreshendaColors.Primary,
            contentColor = FreshendaColors.OnPrimary,
            shape = CircleShape,
        ) { Text("+", style = MaterialTheme.typography.headlineMedium) }
    }
}

@Composable
private fun RowScope.StorageFilter(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label) }, modifier = Modifier.weight(1f))
}

@Composable
private fun FridgeShelf(title: String, batches: List<FoodBatchEntity>, now: Long, onBatch: (Long) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).border(BorderStroke(1.dp, FreshendaColors.Secondary), RoundedCornerShape(22.dp)),
        color = FreshendaColors.SurfaceTint.copy(alpha = .55f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(vertical = 14.dp)) {
            Text(title, modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(batches, key = { it.id }) { batch ->
                    val result = batchExpiry(batch, now)
                    Column(
                        Modifier.width(92.dp).clickable { onBatch(batch.id) }.background(FreshendaColors.Background.copy(alpha = .75f), RoundedCornerShape(16.dp)).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FoodIcon(batch.iconKey, batch.displayName, size = 64.dp, fraction = result.fractionRemaining, status = result.status)
                        Text(batch.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                        Text(result.statusText, maxLines = 1, color = statusTextColor(result.status), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun sectionName(value: String): String = when (value) {
    "UPPER" -> "冷藏 · 上层"
    "LOWER" -> "冷藏 · 下层"
    "CRISPER" -> "冷藏 · 果蔬抽屉"
    "FREEZER_DRAWER" -> "冷冻 · 抽屉"
    "SHELF" -> "常温 · 置物区"
    else -> value
}

private fun statusTextColor(status: FreshnessStatus) = when (status) {
    FreshnessStatus.OVERDUE -> FreshendaColors.Overdue
    FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON -> FreshendaColors.DueSoon
    FreshnessStatus.OK -> FreshendaColors.Primary
    FreshnessStatus.UNKNOWN, FreshnessStatus.REVIEW -> FreshendaColors.Unknown
}
