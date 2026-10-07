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
import com.cake.freshenda.expiry.DueSchedule
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.BatchRow
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.EmptyState
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.ui.theme.MotionMoveEase
import com.cake.freshenda.ui.theme.FreshendaMotion
import com.cake.freshenda.ui.theme.FreshendaSpacing
import com.cake.freshenda.ui.components.SectionHeader
import com.cake.freshenda.ui.components.freshnessColor

@Composable
fun SharedTransitionScope.DueScreen(batches: List<FoodBatchEntity>, now: Long, onBatch: (Long) -> Unit) {
    val groups = remember(batches, now) {
        DueSchedule.groups(batches, now)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(FreshendaSpacing.Gap)) {
        item(key = "header", contentType = "header") { BrandHeader("先吃这些", "日期近的排前面，先安排眼前这几餐。") }
        if (groups.isEmpty()) {
            item(key = "empty", contentType = "empty") { EmptyState("下一餐，还没安排", "添加食材后，\n这里会帮你整理需要留意的日期。") }
        } else {
            groups.forEach { (status, group) ->
                val title = groupTitle(status)
                item(key = title, contentType = "section") {
                    SectionHeader(title, Modifier.padding(horizontal = FreshendaSpacing.Page), "${group.size} 份", freshnessColor(status))
                }
                items(group, key = { it.first.id }, contentType = { "batch" }) { (batch, result) ->
                    BatchRow(
                        batch, result, { onBatch(batch.id) },
                        Modifier.padding(horizontal = FreshendaSpacing.Page).animateItem(
                            fadeInSpec = tween(FreshendaMotion.Enter),
                            placementSpec = tween(FreshendaMotion.Move, easing = MotionMoveEase),
                            fadeOutSpec = tween(FreshendaMotion.Exit),
                        ),
                    )
                }
            }
        }
    }
}

private fun groupTitle(status: FreshnessStatus): String = when (status) {
    FreshnessStatus.OVERDUE -> "日期已过"
    FreshnessStatus.TODAY -> "今天安排"
    FreshnessStatus.DUE_SOON -> "近 3 天内"
    FreshnessStatus.REVIEW -> "日期待确认"
    FreshnessStatus.OK -> "之后安排"
    FreshnessStatus.UNKNOWN -> "未设日期"
}
