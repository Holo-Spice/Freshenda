package com.cake.freshenda.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.AddBatchDraft
import com.cake.freshenda.model.DateBasis
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.model.FoodDefinition
import com.cake.freshenda.model.FoodPhysicalState
import com.cake.freshenda.model.MaturityState
import com.cake.freshenda.model.PackagingState
import com.cake.freshenda.model.StorageLocation
import com.cake.freshenda.model.StorageSection
import com.cake.freshenda.ui.components.BrandHeader
import com.cake.freshenda.ui.components.FoodIcon
import com.cake.freshenda.ui.theme.FreshendaColors
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun EditorScreen(
    food: FoodDefinition,
    catalog: FoodCatalog,
    customFood: CustomFoodEntity?,
    batch: FoodBatchEntity? = null,
    onBack: () -> Unit,
    onSave: (AddBatchDraft) -> Unit,
) {
    val profile = catalog.profiles.firstOrNull { it.id == food.profileId } ?: catalog.profiles.first { it.id == "unknown" }
    val editorKey = batch?.id ?: food.id
    val initialBasis = batch?.let(::dateBasis) ?: if (food.id.startsWith("custom-")) DateBasis.CUSTOM else if (food.profileId in setOf("packaged", "milk", "yogurt")) DateBasis.LABEL else DateBasis.SUGGESTED
    var storage by rememberSaveable(editorKey) { mutableStateOf(batch?.storageLocation?.let { runCatching { StorageLocation.valueOf(it) }.getOrNull() } ?: runCatching { StorageLocation.valueOf(food.defaultStorage) }.getOrDefault(StorageLocation.REFRIGERATED)) }
    var quantity by rememberSaveable(editorKey) { mutableStateOf(batch?.quantityMilli?.let(::quantityInput) ?: "1") }
    var unit by rememberSaveable(editorKey) { mutableStateOf(batch?.quantityUnit ?: food.defaultQuantityUnit) }
    var startDate by rememberSaveable(editorKey) { mutableStateOf(batch?.let(::startDate) ?: LocalDate.now().toString()) }
    var basis by rememberSaveable(editorKey) { mutableStateOf(initialBasis) }
    var targetDate by rememberSaveable(editorKey) { mutableStateOf(batch?.let { targetDate(it, initialBasis) } ?: LocalDate.now().plusDays(customFood?.refrigeratedDays?.toLong() ?: 3).toString()) }
    var packaging by rememberSaveable(editorKey) { mutableStateOf(batch?.packagingState?.let { runCatching { PackagingState.valueOf(it) }.getOrNull() } ?: if (food.profileId in setOf("packaged", "milk", "yogurt", "hard_cheese")) PackagingState.UNOPENED else PackagingState.LOOSE) }
    var physical by rememberSaveable(editorKey) { mutableStateOf(batch?.physicalState?.let { runCatching { FoodPhysicalState.valueOf(it) }.getOrNull() } ?: defaultPhysical(food.profileId)) }
    var maturity by rememberSaveable(editorKey) { mutableStateOf(batch?.maturityState?.let { runCatching { MaturityState.valueOf(it) }.getOrNull() } ?: if (requiresMaturity(food.profileId)) MaturityState.UNKNOWN else MaturityState.NOT_APPLICABLE) }
    val rawWindow = when (storage) {
        StorageLocation.REFRIGERATED -> if (packaging == PackagingState.OPENED) profile.openedRefrigerated ?: profile.refrigerated else profile.refrigerated
        StorageLocation.FROZEN -> profile.frozen
        StorageLocation.PANTRY -> profile.pantry
    }
    var preparation by rememberSaveable(editorKey, storage.name) { mutableStateOf(batch?.preparation ?: "NONE") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        BrandHeader(if (batch == null) "添加食材" else "修改食材", "食材与位置 → 数量 → 起算点 → 日期依据", leading = { TextButton(onClick = onBack) { Text("‹ 返回", color = FreshendaColors.OnPrimary) } })
        Surface(
            Modifier.fillMaxWidth().padding(14.dp),
            color = FreshendaColors.Glass,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, FreshendaColors.GlassBorder),
            shadowElevation = 2.dp,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FoodIcon(food.iconKey, food.name, size = 72.dp)
                Column { Text(food.name, style = MaterialTheme.typography.titleLarge); Text(catalog.categories.firstOrNull { it.id == food.categoryId }?.name ?: "自定义", color = FreshendaColors.Unknown) }
            }
        }
        FormSection("存放位置") {
            ChoiceRow(StorageLocation.entries, storage, { storage = it }) { when (it) { StorageLocation.REFRIGERATED -> "冷藏"; StorageLocation.FROZEN -> "冷冻"; StorageLocation.PANTRY -> "常温" } }
            Text("位置用于整理，不代表 App 读取了真实冰箱温度。", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
        }
        FormSection("数量") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { quantity = adjustQuantity(quantity, -1) }) { Text("−") }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { char -> char.isDigit() || char == '.' }.take(12) }, label = { Text("数量") }, singleLine = true, modifier = Modifier.weight(1f).keepVisibleWithIme())
                OutlinedTextField(value = unit, onValueChange = { unit = it.take(6) }, label = { Text("单位") }, singleLine = true, modifier = Modifier.weight(1f).keepVisibleWithIme())
                OutlinedButton(onClick = { quantity = adjustQuantity(quantity, 1) }) { Text("＋") }
            }
        }
        FormSection("必要状态") {
            Text("包装", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(PackagingState.entries, packaging, { packaging = it }) { when (it) { PackagingState.LOOSE -> "散装"; PackagingState.UNOPENED -> "未开封"; PackagingState.OPENED -> "已开封" } }
            if (food.categoryId in setOf("meat", "seafood", "prepared")) {
                Text("食物状态", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(physicalChoices(food.categoryId), physical, { physical = it }) { physicalLabel(it) }
            }
            if (requiresMaturity(food.profileId)) {
                Text("成熟度", style = MaterialTheme.typography.labelLarge)
                ChoiceRow(listOf(MaturityState.UNRIPE, MaturityState.RIPE, MaturityState.UNKNOWN), maturity, { maturity = it }) { when (it) { MaturityState.UNRIPE -> "未熟"; MaturityState.RIPE -> "成熟"; else -> "不确定" } }
            }
            if (storage == StorageLocation.FROZEN && rawWindow.status == "CONDITIONAL" && rawWindow.preparation != "NONE") {
                FilterChip(selected = preparation == rawWindow.preparation, onClick = { preparation = if (preparation == rawWindow.preparation) "NONE" else rawWindow.preparation }, label = { Text(if (preparation == rawWindow.preparation) "已记录完成对应冷冻处理" else "尚未记录冷冻处理") })
            }
        }
        FormSection("购买／本阶段起点") {
            OutlinedTextField(value = startDate, onValueChange = { startDate = it.take(10) }, label = { Text("日期（yyyy-MM-dd）") }, singleLine = true, modifier = Modifier.fillMaxWidth().keepVisibleWithIme())
            Text("只知道日期时按当天 00:00 起算，不按录入时刻刷新。", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
        }
        FormSection("日期依据") {
            ChoiceRow(DateBasis.entries.toList(), basis, { basis = it }) { when (it) { DateBasis.SUGGESTED -> "储存参考"; DateBasis.LABEL -> "包装日期"; DateBasis.CUSTOM -> "自己设定"; DateBasis.NONE -> "暂不设置" } }
            if (basis == DateBasis.LABEL || basis == DateBasis.CUSTOM) {
                OutlinedTextField(value = targetDate, onValueChange = { targetDate = it.take(10) }, label = { Text(if (basis == DateBasis.LABEL) "包装标注日期" else "计划食用日期") }, singleLine = true, modifier = Modifier.fillMaxWidth().keepVisibleWithIme())
            }
            if (basis == DateBasis.SUGGESTED) {
                val draft = previewDraft(food, storage, quantity, unit, startDate, basis, packaging, physical, maturity, preparation)
                val matched = draft?.let { ExpiryCalculator.suggestedWindow(it, profile) }
                if (matched != null) {
                    Text("${profile.title}：${matched.defaultValue}${unitLabel(matched.unit)}（${evidenceLabel(food, storage)}）", style = MaterialTheme.typography.titleMedium)
                    Text(matched.condition, style = MaterialTheme.typography.bodyMedium)
                    Text(matched.selectionPolicy, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
                } else {
                    Text(if (rawWindow.status == "CONDITIONAL") "条件尚未匹配，保存后日期待确认" else rawWindow.condition, color = FreshendaColors.Overdue, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 18.dp)) }
        Button(onClick = {
            val draft = previewDraft(food, storage, quantity, unit, startDate, basis, packaging, physical, maturity, preparation, targetDate)
            if (draft == null) error = "请检查数量和日期格式" else { error = null; onSave(draft) }
        }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp)) { Text(if (batch == null) "放进冰箱" else "保存修改") }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun Modifier.keepVisibleWithIme(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) {
        if (focused) requester.bringIntoView()
    }
    return bringIntoViewRequester(requester).onFocusChanged { focused = it.isFocused }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, onSelect: (T) -> Unit, label: (T) -> String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value -> FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label(value), maxLines = 1) }) }
    }
}

private fun previewDraft(
    food: FoodDefinition,
    storage: StorageLocation,
    quantity: String,
    unit: String,
    startDate: String,
    basis: DateBasis,
    packaging: PackagingState,
    physical: FoodPhysicalState,
    maturity: MaturityState,
    preparation: String,
    targetDate: String? = null,
): AddBatchDraft? = runCatching {
    val quantityMilli = BigDecimal(quantity).multiply(BigDecimal(1000)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
    require(quantityMilli > 0 && unit.isNotBlank())
    val start = LocalDate.parse(startDate)
    val target = targetDate?.let(LocalDate::parse)
    val zone = ZoneId.systemDefault()
    AddBatchDraft(
        food = food,
        storageLocation = storage,
        storageSection = defaultSection(storage, food.categoryId),
        quantityMilli = quantityMilli,
        quantityUnit = unit.trim(),
        stageStartedEpochMillis = start.atStartOfDay(zone).toInstant().toEpochMilli(),
        businessZoneId = zone.id,
        dateBasis = basis,
        labelDateEpochDay = if (basis == DateBasis.LABEL) requireNotNull(target).toEpochDay() else null,
        customDateEpochDay = if (basis == DateBasis.CUSTOM) requireNotNull(target).toEpochDay() else null,
        packagingState = packaging,
        physicalState = physical,
        maturityState = maturity,
        completedPreparation = preparation,
    )
}.getOrNull()

private fun defaultSection(storage: StorageLocation, category: String): StorageSection = when (storage) {
    StorageLocation.FROZEN -> StorageSection.FREEZER_DRAWER
    StorageLocation.PANTRY -> StorageSection.SHELF
    StorageLocation.REFRIGERATED -> when (category) { "fruit", "leafy", "roots", "gourds", "beans", "herbs", "mushrooms" -> StorageSection.CRISPER; "meat", "seafood" -> StorageSection.LOWER; else -> StorageSection.UPPER }
}

private fun adjustQuantity(value: String, delta: Int): String = ((value.toBigDecimalOrNull() ?: BigDecimal.ONE) + BigDecimal(delta)).coerceAtLeast(BigDecimal.ONE).stripTrailingZeros().toPlainString()
private fun quantityInput(quantityMilli: Long) = BigDecimal.valueOf(quantityMilli, 3).stripTrailingZeros().toPlainString()
private fun startDate(batch: FoodBatchEntity) = Instant.ofEpochMilli(batch.stageStartedAtEpochMillis).atZone(ZoneId.of(batch.businessZoneId)).toLocalDate().toString()
private fun dateBasis(batch: FoodBatchEntity) = when (batch.effectiveDeadlineKind) {
    "LABEL" -> DateBasis.LABEL
    "CUSTOM" -> DateBasis.CUSTOM
    "SUGGESTED" -> DateBasis.SUGGESTED
    else -> when {
        batch.labelDateEpochDay != null -> DateBasis.LABEL
        batch.customDeadlineEpochMillis != null -> DateBasis.CUSTOM
        batch.suggestedDeadlineEpochMillis != null || batch.requiresDateReview -> DateBasis.SUGGESTED
        else -> DateBasis.NONE
    }
}
private fun targetDate(batch: FoodBatchEntity, basis: DateBasis) = when (basis) {
    DateBasis.LABEL -> batch.labelDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() }
    DateBasis.CUSTOM -> batch.customDeadlineEpochMillis?.let { LocalDate.ofEpochDay(ExpiryCalculator.displayDate(it, batch.businessZoneId)).toString() }
    else -> null
} ?: LocalDate.now().plusDays(3).toString()
private fun requiresMaturity(profileId: String) = profileId in setOf("pear", "peach", "nectarine", "plum", "apricot", "kiwi", "mango", "papaya", "banana_ripe", "avocado", "guava")
private fun defaultPhysical(profileId: String) = when (profileId) { "ground" -> FoodPhysicalState.GROUND; "red_stew" -> FoodPhysicalState.CUT; "egg_boiled", "leftover_meat", "soup" -> FoodPhysicalState.COOKED; else -> FoodPhysicalState.WHOLE }
private fun physicalChoices(category: String) = if (category == "prepared") listOf(FoodPhysicalState.COOKED, FoodPhysicalState.WHOLE) else listOf(FoodPhysicalState.WHOLE, FoodPhysicalState.CUT, FoodPhysicalState.GROUND, FoodPhysicalState.COOKED)
private fun physicalLabel(value: FoodPhysicalState) = when (value) { FoodPhysicalState.WHOLE -> "完整"; FoodPhysicalState.CUT -> "切开"; FoodPhysicalState.GROUND -> "肉馅"; FoodPhysicalState.COOKED -> "熟食"; FoodPhysicalState.PREPARED_FOR_FREEZING -> "已处理" }
private fun unitLabel(unit: String?) = when (unit) { "DAY" -> "天"; "WEEK" -> "周"; "MONTH" -> "个月"; else -> "" }
private fun evidenceLabel(food: FoodDefinition, storage: StorageLocation) = when (storage) { StorageLocation.REFRIGERATED -> food.evidence.refrigerated; StorageLocation.FROZEN -> food.evidence.frozen; StorageLocation.PANTRY -> food.evidence.pantry }.let { if (it == "CATEGORY") "同类参考" else if (it == "DIRECT") "直接资料" else "条件参考" }
