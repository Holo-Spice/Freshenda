package com.cake.freshenda.ui.due

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.batchExpiry
import com.cake.freshenda.ui.components.BatchRow
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.EmptyState
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase

@Composable
fun SharedTransitionScope.DueScreen(batches: List<FoodBatchEntity>, now: Long, onBatch: (Long) -> Unit) {
    val groups = remember(batches, now) {
        batches.map { it to batchExpiry(it, now) }
            .sortedWith(compareBy<Pair<FoodBatchEntity, ExpiryResult>> { order(it.second.status) }
                .thenBy { it.first.effectiveDeadlineEpochMillis ?: Long.MAX_VALUE }
                .thenBy { it.first.purchasedAtEpochMillis }.thenBy { it.first.id })
            .groupBy { groupTitle(it.second.status) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "header", contentType = "header") { BrandHeader("先吃这些", "按日期安排，让每一份食材物尽其用。") }
        if (groups.isEmpty()) {
            item(key = "empty", contentType = "empty") { EmptyState("下一餐，还没安排", "添加食材后，\n这里会帮你整理需要留意的日期。") }
        } else {
            groups.forEach { (title, group) ->
                item(key = title, contentType = "section") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("${group.size} 份", style = MaterialTheme.typography.labelMedium, color = FreshendaColors.Unknown)
                    }
                }
                items(group, key = { it.first.id }, contentType = { "batch" }) { (batch, result) ->
                    BatchRow(
                        batch, result, { onBatch(batch.id) },
                        Modifier.padding(horizontal = 24.dp).animateItem(
                            fadeInSpec = tween(160),
                            placementSpec = tween(240, easing = MotionEase),
                            fadeOutSpec = tween(120),
                        ),
                    )
                }
            }
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
    FreshnessStatus.DUE_SOON -> "这几天留意"
    FreshnessStatus.REVIEW -> "日期待确认"
    FreshnessStatus.OK -> "时间充裕"
    FreshnessStatus.UNKNOWN -> "未设日期"
}
