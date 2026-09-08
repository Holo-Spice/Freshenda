package com.cake.freshenda.ui.due

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.BatchRow
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.EmptyState
import com.cake.freshenda.ui.components.batchExpiry

@Composable
fun DueScreen(batches: List<FoodBatchEntity>, now: Long, onBatch: (Long) -> Unit) {
    val sorted = batches.sortedWith(compareBy<FoodBatchEntity> { order(batchExpiry(it, now).status) }.thenBy { it.effectiveDeadlineEpochMillis ?: Long.MAX_VALUE }.thenBy { it.purchasedAtEpochMillis }.thenBy { it.id })
    val groups = sorted.groupBy { groupTitle(batchExpiry(it, now).status) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BrandHeader("先吃这些", "按真实日期状态，安排下一餐") }
        if (sorted.isEmpty()) {
            item { EmptyState("暂无在库食材", "添加食材后，这里会按日期和依据排序") }
        } else {
            groups.forEach { (title, group) ->
                item { Text(title, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), style = MaterialTheme.typography.titleMedium) }
                items(group, key = { it.id }) { batch ->
                    BatchRow(batch, batchExpiry(batch, now), { onBatch(batch.id) }, Modifier.padding(horizontal = 14.dp))
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private fun order(status: FreshnessStatus): Int = when (status) {
    FreshnessStatus.OVERDUE -> 0
    FreshnessStatus.TODAY -> 1
    FreshnessStatus.DUE_SOON -> 2
    FreshnessStatus.REVIEW -> 3
    FreshnessStatus.OK -> 4
    FreshnessStatus.UNKNOWN -> 5
}

private fun groupTitle(status: FreshnessStatus): String = when (status) {
    FreshnessStatus.OVERDUE -> "日期已过"
    FreshnessStatus.TODAY -> "今天安排"
    FreshnessStatus.DUE_SOON -> "临期"
    FreshnessStatus.REVIEW -> "日期待确认"
    FreshnessStatus.OK -> "时间充裕"
    FreshnessStatus.UNKNOWN -> "未设日期"
}
