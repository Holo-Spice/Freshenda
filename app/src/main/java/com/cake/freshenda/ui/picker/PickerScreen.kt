package com.cake.freshenda.ui.picker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.model.EvidenceSummary
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.model.FoodDefinition
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.FoodIcon
import com.cake.freshenda.ui.components.PressableSurface
import com.cake.freshenda.ui.theme.FreshendaColors

@Composable
fun PickerScreen(
    catalog: FoodCatalog,
    customFoods: List<CustomFoodEntity>,
    recentIds: List<String>,
    onBack: () -> Unit,
    onFood: (String) -> Unit,
    onCustom: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("ALL") }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val allFoods = remember(catalog, customFoods, recentIds) {
        val recentOrder = recentIds.withIndex().associate { it.value to it.index }
        (catalog.foods + customFoods.map(::asDefinition)).sortedWith(
            compareBy<FoodDefinition> { recentOrder[it.id] ?: Int.MAX_VALUE }.thenBy { it.name },
        )
    }
    val foods = remember(allFoods, query, category) {
        val term = query.trim()
        allFoods.filter { food ->
            (category == "ALL" || food.categoryId == category) &&
                (term.isEmpty() || food.name.contains(term, ignoreCase = true) ||
                    food.aliases.any { it.contains(term, ignoreCase = true) } || food.id.contains(term, ignoreCase = true))
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        BrandHeader("添加食材", "选一种食材，开始记录新鲜", leading = { TextButton(onClick = onBack) { Text("‹ 返回", color = FreshendaColors.Primary) } })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索食材名称或别名") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.semantics { contentDescription = "清空搜索" }) { Text("×") }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = category == "ALL", onClick = { category = "ALL" }, label = { Text("全部") }) }
            items(catalog.categories, key = { it.id }) { item ->
                FilterChip(selected = category == item.id, onClick = { category = item.id }, label = { Text(item.name) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("找到 ${foods.size} 项", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showCustom = true }) { Text("＋ 自定义食材") }
        }
        if (foods.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("还没找到这份食材", style = MaterialTheme.typography.titleMedium)
                Text("换个名称试试，或添加自定义食材。", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown, textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(foods, key = { it.id }, contentType = { "food" }) { food ->
                    PressableSurface(onClick = { onFood(food.id) }, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            FoodIcon(food.iconKey, food.name, size = 64.dp)
                            Text(food.name, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
    if (showCustom) {
        CustomFoodDialog(onDismiss = { showCustom = false }) { name ->
            showCustom = false
            onCustom(name)
        }
    }
}

private fun asDefinition(food: CustomFoodEntity) = FoodDefinition(
    id = food.id,
    name = food.name,
    categoryId = food.categoryId,
    profileId = "unknown",
    iconKey = food.iconKey,
    iconBrief = "自定义食材",
    iconDeliveryStatus = "AUTHORED",
    defaultStorage = "REFRIGERATED",
    defaultQuantityUnit = food.defaultQuantityUnit,
    evidence = EvidenceSummary("UNVERIFIED", "UNVERIFIED", "UNVERIFIED"),
    requiredState = "USER_INPUT",
)

@Composable
private fun CustomFoodDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义食材") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("下一步") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
