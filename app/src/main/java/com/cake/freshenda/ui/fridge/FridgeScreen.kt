package com.cake.freshenda.ui.fridge

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.components.BrandHeader
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
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val visibleBatches = remember(batches, filter) {
        if (filter == ALL) batches else batches.filter { it.storageLocation == filter }
    }
    val urgent = remember(batches, now) {
        batches.count {
            batchExpiry(it, now).status in setOf(
                FreshnessStatus.OVERDUE,
                FreshnessStatus.TODAY,
                FreshnessStatus.DUE_SOON,
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(contentType = "header") {
                BrandHeader("我的冰箱", "${batches.size} 份食材 · $urgent 份优先安排")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(STORAGE_FILTERS, key = { it.second }) { option ->
                        StorageFilter(option.first, option.second, filter) { filter = it }
                    }
                }
                FreshnessLegend()
            }
            if (filter != PANTRY) {
                item(contentType = "fridge") {
                    RefrigeratorCabinet(
                        batches = visibleBatches,
                        now = now,
                        showRefrigerated = filter != FROZEN,
                        showFrozen = filter != REFRIGERATED,
                        onAdd = onAdd,
                        onBatch = onBatch,
                    )
                }
            }
            if (filter == ALL || filter == PANTRY) {
                item(contentType = "pantry") {
                    PantryShelf(
                        batches = visibleBatches.filter { it.storageLocation == PANTRY },
                        now = now,
                        onAdd = onAdd,
                        onBatch = onBatch,
                    )
                }
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
private fun StorageFilter(
    label: String,
    value: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}

@Composable
private fun FreshnessLegend() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item { LegendItem(FreshendaColors.Primary, "新鲜") }
        item { LegendItem(FreshendaColors.DueSoon, "临期") }
        item { LegendItem(FreshendaColors.Overdue, "已过期") }
        item { LegendItem(FreshendaColors.Unknown, "未设日期") }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RefrigeratorCabinet(
    batches: List<FoodBatchEntity>,
    now: Long,
    showRefrigerated: Boolean,
    showFrozen: Boolean,
    onAdd: () -> Unit,
    onBatch: (Long) -> Unit,
) {
    val cabinetBatches = batches.filter {
        (showRefrigerated && it.storageLocation == REFRIGERATED) ||
            (showFrozen && it.storageLocation == FROZEN)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .animateContentSize(),
        color = FreshendaColors.Card,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(3.dp, FreshendaColors.Primary),
    ) {
        Box {
            Column(Modifier.padding(8.dp)) {
                CabinetCap()
                if (showRefrigerated) {
                    FridgeCompartment("冷藏上层", cabinetBatches.forSection("UPPER"), now, onBatch)
                    ShelfDivider()
                    FridgeCompartment("冷藏下层", cabinetBatches.forSection("LOWER"), now, onBatch)
                    ShelfDivider()
                    FridgeCompartment("果蔬抽屉", cabinetBatches.forSection("CRISPER"), now, onBatch, drawer = true)
                }
                if (showFrozen) {
                    if (showRefrigerated) ShelfDivider(thick = true)
                    FridgeCompartment(
                        "冷冻抽屉",
                        cabinetBatches.filter { it.storageLocation == FROZEN },
                        now,
                        onBatch,
                        drawer = true,
                    )
                }
            }
            if (cabinetBatches.isEmpty()) {
                EmptyFridgePrompt(
                    text = if (batches.isEmpty()) "冰箱还是空的" else "这个位置还没有食材",
                    onClick = onAdd,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun CabinetCap() {
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("鲜序", color = FreshendaColors.Primary, style = MaterialTheme.typography.titleMedium)
        Box(
            Modifier
                .width(54.dp)
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(FreshendaColors.Secondary),
        )
    }
}

@Composable
private fun FridgeCompartment(
    title: String,
    batches: List<FoodBatchEntity>,
    now: Long,
    onBatch: (Long) -> Unit,
    drawer: Boolean = false,
) {
    val background = if (drawer) FreshendaColors.SurfaceTint.copy(alpha = .72f) else FreshendaColors.Background.copy(alpha = .78f)
    Column(
        Modifier
            .fillMaxWidth()
            .height(if (drawer) 116.dp else 104.dp)
            .background(background, RoundedCornerShape(14.dp))
            .padding(vertical = 7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (drawer) {
                Box(
                    Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FreshendaColors.Secondary),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (batches.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(batches, key = { it.id }, contentType = { "food" }) { batch ->
                    CabinetFood(batch, now, onBatch)
                }
            }
        }
    }
}

@Composable
private fun CabinetFood(batch: FoodBatchEntity, now: Long, onBatch: (Long) -> Unit) {
    val result = remember(batch, now) { batchExpiry(batch, now) }
    Box(
        modifier = Modifier
            .size(62.dp)
            .clickable(role = Role.Button) { onBatch(batch.id) },
        contentAlignment = Alignment.Center,
    ) {
        FoodIcon(
            iconKey = batch.iconKey,
            name = "${batch.displayName}，${result.statusText}",
            size = 52.dp,
            fraction = result.fractionRemaining ?: 0f,
            status = result.status,
        )
    }
}

@Composable
private fun ShelfDivider(thick: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (thick) 12.dp else 8.dp)
            .padding(horizontal = 5.dp, vertical = 2.dp)
            .background(FreshendaColors.Primary.copy(alpha = if (thick) .55f else .30f), RoundedCornerShape(50)),
    )
}

@Composable
private fun PantryShelf(
    batches: List<FoodBatchEntity>,
    now: Long,
    onAdd: () -> Unit,
    onBatch: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        color = FreshendaColors.Card,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(2.dp, FreshendaColors.Secondary),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text("常温置物架", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.titleMedium)
            if (batches.isEmpty()) {
                EmptyFridgePrompt("置物架还是空的", onAdd, Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(batches, key = { it.id }, contentType = { "food" }) { batch ->
                        CabinetFood(batch, now, onBatch)
                    }
                }
            }
            ShelfDivider()
        }
    }
}

@Composable
private fun EmptyFridgePrompt(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(132.dp)
            .clickable(role = Role.Button, onClick = onClick),
        color = FreshendaColors.Background.copy(alpha = .96f),
        shape = CircleShape,
        border = BorderStroke(2.dp, FreshendaColors.Secondary),
        shadowElevation = 3.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("+", color = FreshendaColors.Primary, style = MaterialTheme.typography.displaySmall)
            Text(text, style = MaterialTheme.typography.labelLarge)
            Text("点击添加食材", color = FreshendaColors.Unknown, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun List<FoodBatchEntity>.forSection(section: String): List<FoodBatchEntity> =
    filter {
        it.storageLocation == REFRIGERATED &&
            (it.storageSection == section || section == "UPPER" && it.storageSection !in REFRIGERATED_SECTIONS)
    }

private val STORAGE_FILTERS = listOf(
    "全部" to ALL,
    "冷藏" to REFRIGERATED,
    "冷冻" to FROZEN,
    "常温" to PANTRY,
)
private val REFRIGERATED_SECTIONS = setOf("UPPER", "LOWER", "CRISPER")
private const val ALL = "ALL"
private const val REFRIGERATED = "REFRIGERATED"
private const val FROZEN = "FROZEN"
private const val PANTRY = "PANTRY"
