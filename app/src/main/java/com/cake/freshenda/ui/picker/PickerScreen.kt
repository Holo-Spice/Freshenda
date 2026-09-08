package com.cake.freshenda.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.model.EvidenceSummary
import com.cake.freshenda.model.FoodDefinition
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.FoodIcon
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
    val normalized = query.trim().lowercase()
    val allFoods = catalog.foods + customFoods.map(::asDefinition)
    val foods = allFoods.filter { food ->
        (category == "ALL" || food.categoryId == category) &&
            (normalized.isBlank() || food.name.lowercase().contains(normalized) || food.aliases.any { it.lowercase().contains(normalized) } || food.id.lowercase().contains(normalized))
    }.sortedWith(compareBy<FoodDefinition> { recentIds.indexOf(it.id).let { index -> if (index < 0) Int.MAX_VALUE else index } }.thenBy { it.name })

    Column(Modifier.fillMaxSize()) {
        BrandHeader("放点什么进冰箱", "从 ${catalog.foods.size} 种离线食材中选择", leading = { TextButton(onClick = onBack) { Text("‹ 返回", color = FreshendaColors.OnPrimary) } })
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索名称或常用别名") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = category == "ALL", onClick = { category = "ALL" }, label = { Text("全部") }) }
            items(catalog.categories, key = { it.id }) { item ->
                FilterChip(selected = category == item.id, onClick = { category = item.id }, label = { Text(item.name) })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("找到 ${foods.size} 项", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showCustom = true }) { Text("＋ 自定义食材") }
        }
        LazyVerticalGrid(columns = GridCells.Adaptive(88.dp), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(foods, key = { it.id }) { food ->
                Surface(modifier = Modifier.clickable { onFood(food.id) }, color = FreshendaColors.Card, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FoodIcon(food.iconKey, food.name, size = 60.dp)
                        Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
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
