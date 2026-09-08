package com.cake.freshenda.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cake.freshenda.AppContainer
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.model.EvidenceSummary
import com.cake.freshenda.model.FoodDefinition
import com.cake.freshenda.ui.detail.DetailScreen
import com.cake.freshenda.ui.due.DueScreen
import com.cake.freshenda.ui.editor.EditorScreen
import com.cake.freshenda.ui.fridge.FridgeScreen
import com.cake.freshenda.ui.picker.PickerScreen
import com.cake.freshenda.ui.settings.SettingsScreen
import com.cake.freshenda.ui.theme.FreshendaColors
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppDeepLink(val destination: String, val batchId: Long?)

@Serializable data object FridgeRoute : NavKey
@Serializable data object DueRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object PickerRoute : NavKey
@Serializable data class EditorRoute(val foodId: String, val customName: String? = null) : NavKey
@Serializable data class DetailRoute(val batchId: Long) : NavKey

@Composable
fun FreshendaApp(container: AppContainer, deepLink: AppDeepLink?, onDeepLinkConsumed: () -> Unit) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(FridgeRoute)
    val snackbarHost = remember { SnackbarHostState() }
    val now = remember { System.currentTimeMillis() }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::exportBackup) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::inspectBackup) }

    LaunchedEffect(message) {
        message?.let { snackbarHost.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            backStack.clear()
            if (deepLink.destination == "detail" && deepLink.batchId != null) {
                backStack.add(FridgeRoute)
                backStack.add(DetailRoute(deepLink.batchId))
            } else {
                backStack.add(DueRoute)
            }
            onDeepLinkConsumed()
        }
    }

    if (ui.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (ui.error != null || ui.catalog == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(ui.error ?: "食材目录不可用") }
        return
    }
    val catalog = ui.catalog!!
    val current = backStack.lastOrNull()
    val showBottomBar = current is FridgeRoute || current is DueRoute || current is SettingsRoute

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = FreshendaColors.Card) {
                    RootNavItem("冰箱", NavMarkKind.FRIDGE, current is FridgeRoute) { backStack.clear(); backStack.add(FridgeRoute) }
                    RootNavItem("待吃", NavMarkKind.DUE, current is DueRoute) { backStack.clear(); backStack.add(DueRoute) }
                    RootNavItem("我的", NavMarkKind.USER, current is SettingsRoute) { backStack.clear(); backStack.add(SettingsRoute) }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { pop(backStack) },
            modifier = Modifier.padding(padding),
            transitionSpec = {
                (fadeIn(tween(160)) + slideInHorizontally(tween(180)) { it / 12 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { -it / 16 })
            },
            popTransitionSpec = {
                (fadeIn(tween(160)) + slideInHorizontally(tween(180)) { -it / 12 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(160)) { it / 16 })
            },
            entryProvider = entryProvider {
                entry<FridgeRoute> { FridgeScreen(ui.batches, now, { backStack.add(PickerRoute) }) { backStack.add(DetailRoute(it)) } }
                entry<DueRoute> { DueScreen(ui.batches, now) { backStack.add(DetailRoute(it)) } }
                entry<SettingsRoute> {
                    SettingsScreen(
                        ui.settings,
                        catalog,
                        container.notificationPublisher.canPublish(),
                        viewModel::setRemindersEnabled,
                        { if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        viewModel::setDailySummary,
                        viewModel::setOpenedReminders,
                        viewModel::setCustomDateReminders,
                        viewModel::setReminderHour,
                        viewModel::sendTestNotification,
                        { exportLauncher.launch("鲜序备份-${Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)}.json") },
                        { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                    )
                }
                entry<PickerRoute> {
                    PickerScreen(catalog, ui.customFoods, ui.settings.recentFoodIds, { pop(backStack) }, { backStack.add(EditorRoute(it)) }, { backStack.add(EditorRoute("__custom__", it)) })
                }
                entry<EditorRoute> { route ->
                    val savedCustom = ui.customFoods.firstOrNull { it.id == route.foodId }
                    val food = catalog.foods.firstOrNull { it.id == route.foodId } ?: savedCustom?.let(::customDefinition) ?: customDefinition(route.customName ?: "自定义食材")
                    EditorScreen(food, catalog, savedCustom, { pop(backStack) }) { draft ->
                        viewModel.addBatch(draft) { showRoot(backStack, FridgeRoute) }
                    }
                }
                entry<DetailRoute> { route ->
                    val selectedBatch by viewModel.selectedBatch.collectAsStateWithLifecycle()
                    val recentChanges by viewModel.recentChanges.collectAsStateWithLifecycle()
                    LaunchedEffect(route.batchId) { viewModel.selectBatch(route.batchId) }
                    DetailScreen(
                        selectedBatch?.takeIf { it.id == route.batchId },
                        recentChanges,
                        now,
                        { viewModel.selectBatch(null); pop(backStack) },
                        { quantity, consumeAll ->
                            viewModel.consume(route.batchId, quantity) {
                                if (consumeAll) {
                                    viewModel.selectBatch(null)
                                    showRoot(backStack, FridgeRoute)
                                }
                            }
                        },
                        { viewModel.markOpened(route.batchId) },
                        {
                            viewModel.discard(route.batchId) {
                                viewModel.selectBatch(null)
                                showRoot(backStack, FridgeRoute)
                            }
                        },
                        { viewModel.moveBatch(route.batchId, it) },
                        { quantity, target -> viewModel.splitAndMove(route.batchId, quantity, target) { childId -> backStack.add(DetailRoute(childId)) } },
                        { viewModel.setCustomDate(route.batchId, it) },
                    )
                }
            },
        )
    }

    pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("替换当前库存？") },
            text = { Text("备份时间：${Instant.ofEpochMilli(pending.envelope.exportedAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n批次数量：${pending.envelope.batches.size}\n\n确认后会替换当前库存；解析失败时不会清空数据。") },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text("确认恢复") } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text("取消") } },
        )
    }
}

private fun pop(backStack: MutableList<NavKey>) {
    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
}

private fun showRoot(backStack: MutableList<NavKey>, route: NavKey) {
    backStack.clear()
    backStack.add(route)
}

@Composable
private fun RowScope.RootNavItem(label: String, kind: NavMarkKind, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { NavMark(kind) }, label = { Text(label) })
}

private enum class NavMarkKind { FRIDGE, DUE, USER }

@Composable
private fun NavMark(kind: NavMarkKind) {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        when (kind) {
            NavMarkKind.FRIDGE -> {
                drawRoundRect(FreshendaColors.Primary, style = Stroke(stroke))
                drawLine(FreshendaColors.Primary, Offset(size.width * .12f, size.height * .45f), Offset(size.width * .88f, size.height * .45f), stroke)
            }
            NavMarkKind.DUE -> {
                drawArc(FreshendaColors.Primary, 20f, 140f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(FreshendaColors.Primary, 200f, 140f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            NavMarkKind.USER -> {
                drawCircle(FreshendaColors.Primary, size.minDimension * .18f, Offset(size.width / 2, size.height * .3f), style = Stroke(stroke))
                drawArc(FreshendaColors.Primary, 190f, 160f, false, topLeft = Offset(size.width * .2f, size.height * .45f), size = Size(size.width * .6f, size.height * .45f), style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

private fun customDefinition(name: String) = FoodDefinition(
    id = "custom-${name.hashCode().toUInt().toString(16)}",
    name = name,
    categoryId = "prepared",
    profileId = "unknown",
    iconKey = "ic_food_custom",
    iconBrief = "自定义食材通用篮子",
    iconDeliveryStatus = "AUTHORED",
    defaultStorage = "REFRIGERATED",
    defaultQuantityUnit = "份",
    evidence = EvidenceSummary("UNVERIFIED", "UNVERIFIED", "UNVERIFIED"),
    requiredState = "USER_INPUT",
)

private fun customDefinition(food: CustomFoodEntity) = customDefinition(food.name).copy(
    id = food.id,
    categoryId = food.categoryId,
    iconKey = food.iconKey,
    defaultQuantityUnit = food.defaultQuantityUnit,
)
