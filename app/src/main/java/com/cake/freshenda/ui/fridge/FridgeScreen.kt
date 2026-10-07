package com.cake.freshenda.ui.fridge

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.cake.freshenda.ui.components.FreshendaButton as Button
import androidx.compose.material3.ExtendedFloatingActionButton
import com.cake.freshenda.ui.components.ChoiceChip as FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.batchExpiry
import com.cake.freshenda.ui.components.BatchIcon
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.EmptyState
import com.cake.freshenda.ui.components.FoodIcon
import com.cake.freshenda.ui.components.freshnessColor
import com.cake.freshenda.ui.components.PressableSurface
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.ui.theme.FreshendaMotion
import com.cake.freshenda.ui.theme.FreshendaShapes
import com.cake.freshenda.ui.theme.FreshendaSpacing
import com.cake.freshenda.ui.theme.MotionMoveEase
import com.cake.freshenda.ui.components.SectionHeader
import com.cake.freshenda.ui.components.UiIcon
import com.cake.freshenda.ui.components.UiSymbol

@Composable
fun SharedTransitionScope.FridgeScreen(
    batches: List<FoodBatchEntity>,
    now: Long,
    onAdd: () -> Unit,
    onDue: () -> Unit,
    onBatch: (Long) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("ALL") }
    val grouped = remember(batches) { batches.groupBy(::sectionOf) }
    val urgent = remember(batches, now) {
        batches.count { batchExpiry(it, now).status in URGENT_STATUSES }
    }
    val sections = remember(filter, grouped) {
        SECTIONS.filter { section ->
            if (filter == "ALL") !grouped[section.id].isNullOrEmpty() else section.location == filter
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header", contentType = "header") {
                BrandHeader("我的冰箱", "好好储存，也好好吃饭。")
                InventorySummary(batches.size, urgent, onDue)
            }
            item(key = "filters", contentType = "filters") {
                LazyRow(contentPadding = PaddingValues(horizontal = FreshendaSpacing.Page), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(STORAGE_FILTERS, key = { it.second }) { (label, value) ->
                        FilterChip(
                            selected = filter == value,
                            onClick = { filter = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
            if (batches.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState("新鲜生活，从这里开始", "记下刚买的食材，\n下一餐吃什么，就更有数了。")
                        Button(onClick = onAdd) { Text("添加第一份食材") }
                    }
                }
            } else {
                items(sections, key = { it.id }, contentType = { "shelf" }) { section ->
                    StorageShelf(
                        section, grouped[section.id].orEmpty(), now, onAdd, onBatch,
                        Modifier.animateItem(tween(FreshendaMotion.Enter), tween(FreshendaMotion.Move, easing = MotionMoveEase), tween(FreshendaMotion.Exit)),
                    )
                }
            }
        }
        if (batches.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(FreshendaSpacing.Page),
                containerColor = FreshendaColors.Primary,
                contentColor = FreshendaColors.OnPrimary,
                shape = FreshendaShapes.Control,
                icon = { UiIcon(UiSymbol.ADD) },
                text = { Text("添加食材") },
            )
        }
    }
}

@Composable
private fun InventorySummary(count: Int, urgent: Int, onDue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page),
        shape = FreshendaShapes.Card,
        color = FreshendaColors.Primary,
        contentColor = FreshendaColors.OnPrimary,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("新鲜有序 · 每一餐都有数", style = MaterialTheme.typography.labelMedium, color = FreshendaColors.SurfaceTint)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(count.toString().padStart(2, '0'), style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"))
                        Text("份食材在库", modifier = Modifier.padding(bottom = 7.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FoodIcon("food_broccoli", "", size = 66.dp)
            }
            PressableSurface(onDue, Modifier.fillMaxWidth(), color = FreshendaColors.HeaderAccent, shape = FreshendaShapes.Inset) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(6.dp).background(FreshendaColors.SurfaceTint, CircleShape))
                    Text(
                        if (urgent > 0) "$urgent 份食材需要留意" else "查看食材日期与食用顺序",
                        modifier = Modifier.weight(1f),
                        color = FreshendaColors.OnPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    UiIcon(UiSymbol.NEXT, color = FreshendaColors.OnPrimary)
                }
            }
        }
    }
}

@Composable
private fun SharedTransitionScope.StorageShelf(section: StorageShelf, batches: List<FoodBatchEntity>, now: Long, onAdd: () -> Unit, onBatch: (Long) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page), color = FreshendaColors.Card, shape = FreshendaShapes.Card, shadowElevation = 1.dp) {
        Column(Modifier.padding(vertical = FreshendaSpacing.CardInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(section.title, Modifier.padding(horizontal = FreshendaSpacing.CardInset), "${batches.size} 份", FreshendaColors.Secondary)
            if (batches.isEmpty()) {
                PressableSurface(onAdd, Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.CardInset), color = FreshendaColors.GlassSoft, shape = FreshendaShapes.Control) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UiIcon(UiSymbol.ADD, color = FreshendaColors.Primary)
                        Text("这里还空着，放点食材", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
                    }
                }
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = FreshendaSpacing.CardInset), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(batches, key = { it.id }, contentType = { "food" }) { batch ->
                        val result = remember(batch, now) { batchExpiry(batch, now) }
                        PressableSurface(
                            { onBatch(batch.id) },
                            Modifier.width(96.dp).animateItem(tween(FreshendaMotion.Enter), tween(FreshendaMotion.Move, easing = MotionMoveEase), tween(FreshendaMotion.Exit)),
                            color = FreshendaColors.GlassSoft,
                            shape = FreshendaShapes.Control,
                        ) {
                            Column(Modifier.padding(horizontal = 6.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                BatchIcon(batch, result, source = "fridge", size = 64.dp)
                                Spacer(Modifier.height(8.dp))
                                Text(batch.displayName, style = MaterialTheme.typography.labelLarge, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(3.dp))
                                Text(result.statusText, style = MaterialTheme.typography.labelSmall, color = freshnessColor(result.status), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class StorageShelf(val id: String, val title: String, val location: String)
private val SECTIONS = listOf(
    StorageShelf("UPPER", "冷藏上层", "REFRIGERATED"),
    StorageShelf("LOWER", "冷藏下层", "REFRIGERATED"),
    StorageShelf("CRISPER", "果蔬抽屉", "REFRIGERATED"),
    StorageShelf("FREEZER_DRAWER", "冷冻抽屉", "FROZEN"),
    StorageShelf("SHELF", "常温置物架", "PANTRY"),
)
private fun sectionOf(batch: FoodBatchEntity): String = when (batch.storageLocation) {
    "FROZEN" -> "FREEZER_DRAWER"
    "PANTRY" -> "SHELF"
    else -> batch.storageSection.takeIf { it in setOf("UPPER", "LOWER", "CRISPER") } ?: "UPPER"
}
private val STORAGE_FILTERS = listOf("全部" to "ALL", "冷藏" to "REFRIGERATED", "冷冻" to "FROZEN", "常温" to "PANTRY")
private val URGENT_STATUSES = setOf(FreshnessStatus.OVERDUE, FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON)

