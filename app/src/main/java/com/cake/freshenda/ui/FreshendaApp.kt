package com.cake.freshenda.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cake.freshenda.AppContainer
import com.cake.freshenda.data.local.CustomFoodEntity
import com.cake.freshenda.model.EvidenceSummary
import com.cake.freshenda.model.FoodDefinition
import com.cake.freshenda.ui.components.UpdateDialog
import com.cake.freshenda.ui.detail.DetailScreen
import com.cake.freshenda.ui.due.DueScreen
import com.cake.freshenda.ui.editor.EditorScreen
import com.cake.freshenda.ui.fridge.FridgeScreen
import com.cake.freshenda.ui.picker.PickerScreen
import com.cake.freshenda.ui.settings.SettingsScreen
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.update.UpdateViewModel
import com.cake.freshenda.update.UpdateFeedback
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class AppDeepLink(val destination: String, val batchId: Long?)

@Serializable data object FridgeRoute : NavKey
@Serializable data object DueRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object PickerRoute : NavKey
@Serializable data class EditorRoute(val foodId: String, val customName: String? = null) : NavKey
@Serializable data class EditRoute(val batchId: Long) : NavKey
@Serializable data class DetailRoute(val batchId: Long, val source: String = "fridge") : NavKey

@Composable
fun FreshendaApp(container: AppContainer, deepLink: AppDeepLink?, onDeepLinkConsumed: () -> Unit) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container))
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val updateViewModel: UpdateViewModel = viewModel(factory = UpdateViewModel.Factory(container))
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun launchInstaller() {
        scope.launch {
            try {
                val intent = updateViewModel.installIntent()
                if (intent == null) updateViewModel.installFeedback(UpdateFeedback.INSTALL_FAILED)
                else lifecycleOwner.lifecycle.withResumed { context.startActivity(intent) }
            } catch (_: ActivityNotFoundException) {
                updateViewModel.installFeedback(UpdateFeedback.INSTALL_FAILED)
            } catch (_: SecurityException) {
                updateViewModel.installFeedback(UpdateFeedback.INSTALL_FAILED)
            }
        }
    }
    val installPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) {
            updateViewModel.consumeInstallRequest()
            launchInstaller()
        } else updateViewModel.installFeedback(UpdateFeedback.INSTALL_PERMISSION)
    }
    fun requestInstall() {
        updateViewModel.consumeInstallRequest()
        if (context.packageManager.canRequestPackageInstalls()) {
            launchInstaller()
        } else {
            updateViewModel.installFeedback(UpdateFeedback.INSTALL_PERMISSION)
            try {
                installPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            } catch (_: ActivityNotFoundException) {
                updateViewModel.installFeedback(UpdateFeedback.INSTALL_FAILED)
            }
        }
    }
    LaunchedEffect(updateState.installRequested) {
        if (updateState.installRequested) lifecycleOwner.lifecycle.withResumed { requestInstall() }
    }
    var notificationsAllowed by remember { mutableStateOf(container.notificationPublisher.canPublish()) }
    DisposableEffect(lifecycleOwner, updateViewModel) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                updateViewModel.setForeground(true)
                updateViewModel.check()
                notificationsAllowed = container.notificationPublisher.canPublish()
            } else if (event == Lifecycle.Event.ON_STOP) {
                updateViewModel.setForeground(false)
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            updateViewModel.setForeground(true)
            updateViewModel.check()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            updateViewModel.setForeground(false)
        }
    }
    val message by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(FridgeRoute)
    val snackbarHost = remember { SnackbarHostState() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val rootState = rememberSaveableStateHolder()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                now = System.currentTimeMillis()
                delay(60_000)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsAllowed = container.notificationPublisher.canPublish() }
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
        modifier = Modifier.fillMaxSize(),
        containerColor = FreshendaColors.Background,
        snackbarHost = { SnackbarHost(snackbarHost, Modifier.padding(bottom = 82.dp)) },
    ) { padding ->
        SharedTransitionLayout(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Box(Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { pop(backStack) },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FreshendaColors.Background)
                        .clipToBounds(),
                    transitionSpec = {
                        if (initialState.entries.last().metadata["root"] == true && targetState.entries.last().metadata["root"] == true) {
                            fadeIn(tween(140, easing = MotionEase)) togetherWith fadeOut(tween(100))
                        } else {
                            (slideInHorizontally(tween(260, easing = MotionEase)) { it / 12 } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally(tween(260, easing = MotionEase)) { -it / 24 } + fadeOut(tween(140)))
                        }
                    },
                    popTransitionSpec = {
                        (slideInHorizontally(tween(260, easing = MotionEase)) { -it / 24 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(260, easing = MotionEase)) { it / 12 } + fadeOut(tween(180)))
                    },
                    predictivePopTransitionSpec = {
                        (slideInHorizontally(tween(260, easing = MotionEase)) { -it / 24 } + fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(260, easing = MotionEase)) { it / 12 } + fadeOut(tween(180)))
                    },
                    entryProvider = entryProvider {
                        entry<FridgeRoute>(metadata = RootMetadata) {
                            rootState.SaveableStateProvider("fridge") {
                                ScreenFrame(root = true) {
                                    FridgeScreen(ui.batches, now, { backStack.add(PickerRoute) }, { showRoot(backStack, DueRoute) }) { backStack.add(DetailRoute(it)) }
                                }
                            }
                        }
                        entry<DueRoute>(metadata = RootMetadata) {
                            rootState.SaveableStateProvider("due") {
                                ScreenFrame(root = true) { DueScreen(ui.batches, now) { backStack.add(DetailRoute(it, source = "due")) } }
                            }
                        }
                        entry<SettingsRoute>(metadata = RootMetadata) {
                            ScreenFrame(root = true) {
                                SettingsScreen(
                                    ui.settings,
                                    catalog,
                                    notificationsAllowed,
                                    viewModel::setRemindersEnabled,
                                    { if (Build.VERSION.SDK_INT >= 33) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    viewModel::setDailySummary,
                                    viewModel::setOpenedReminders,
                                    viewModel::setCustomDateReminders,
                                    viewModel::setReminderHour,
                                    viewModel::sendTestNotification,
                                    { exportLauncher.launch("鲜序备份-${Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)}.json") },
                                    { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                                    updateState = updateState,
                                    onCheckUpdate = { updateViewModel.check(manual = true) },
                                )
                            }
                        }
                        entry<PickerRoute> {
                            ScreenFrame {
                                PickerScreen(catalog, ui.customFoods, ui.settings.recentFoodIds, { pop(backStack) }, { backStack.add(EditorRoute(it)) }, { backStack.add(EditorRoute("__custom__", it)) })
                            }
                        }
                        entry<EditorRoute> { route ->
                            ScreenFrame {
                                val savedCustom = ui.customFoods.firstOrNull { it.id == route.foodId }
                                val food = catalog.foods.firstOrNull { it.id == route.foodId } ?: savedCustom?.let(::customDefinition) ?: customDefinition(route.customName ?: "自定义食材")
                                EditorScreen(food, catalog, savedCustom, onBack = { pop(backStack) }, saving = saving) { draft ->
                                    viewModel.addBatch(draft) {
                                        rootState.removeState("fridge")
                                        showRoot(backStack, FridgeRoute)
                                    }
                                }
                            }
                        }
                        entry<EditRoute> { route ->
                            ScreenFrame {
                                val batch = ui.batches.firstOrNull { it.id == route.batchId }
                                if (batch == null) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("食材不存在或已移出冰箱") }
                                } else {
                                    val savedCustom = ui.customFoods.firstOrNull { it.id == batch.foodDefinitionId }
                                    val food = catalog.foods.firstOrNull { it.id == batch.foodDefinitionId }
                                        ?: savedCustom?.let(::customDefinition)
                                        ?: customDefinition(batch.displayName).copy(
                                            id = batch.foodDefinitionId,
                                            iconKey = batch.iconKey,
                                            defaultStorage = batch.storageLocation,
                                            defaultQuantityUnit = batch.quantityUnit,
                                        )
                                    EditorScreen(food, catalog, savedCustom, batch, { pop(backStack) }, saving = saving) { draft ->
                                        viewModel.updateBatch(route.batchId, draft) { pop(backStack) }
                                    }
                                }
                            }
                        }
                        entry<DetailRoute> { route ->
                            ScreenFrame {
                                val selectedBatch by viewModel.selectedBatch.collectAsStateWithLifecycle()
                                val recentChanges by viewModel.recentChanges.collectAsStateWithLifecycle()
                                LaunchedEffect(route.batchId) { viewModel.selectBatch(route.batchId) }
                                DetailScreen(
                                    selectedBatch?.takeIf { it.id == route.batchId } ?: ui.batches.firstOrNull { it.id == route.batchId },
                                    recentChanges,
                                    now,
                                    route.source,
                                    { viewModel.selectBatch(null); pop(backStack) },
                                    { backStack.add(EditRoute(route.batchId)) },
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
                        }
                    },
                )
                AnimatedVisibility(
                    visible = showBottomBar,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(180)) + slideInVertically(tween(200, easing = MotionEase)) { it / 6 },
                    exit = fadeOut(tween(100)) + slideOutVertically(tween(200, easing = MotionEase)) { it / 6 },
                ) {
                    NavigationBar(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        containerColor = FreshendaColors.Card,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0),
                    ) {
                        RootNavItem("冰箱", NavMarkKind.FRIDGE, current is FridgeRoute) { if (current !is FridgeRoute) showRoot(backStack, FridgeRoute) }
                        RootNavItem("待吃", NavMarkKind.DUE, current is DueRoute) { if (current !is DueRoute) showRoot(backStack, DueRoute) }
                        RootNavItem("设置", NavMarkKind.SETTINGS, current is SettingsRoute) { if (current !is SettingsRoute) showRoot(backStack, SettingsRoute) }
                    }
                }
            }
        }
    }

    if (pendingImport == null && updateState.dialogVisible) {
        UpdateDialog(
            updateState,
            onDismiss = updateViewModel::dismissUpdate,
            onIgnore = updateViewModel::ignoreUpdate,
            onRetry = { updateViewModel.check(manual = true) },
            onDownload = updateViewModel::downloadUpdate,
            onCancelDownload = updateViewModel::cancelDownload,
            onInstall = ::requestInstall,
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

@Composable
private fun ScreenFrame(root: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = if (root) 80.dp else 0.dp)
            .background(FreshendaColors.Background)
            .clipToBounds(),
    ) {
        content()
    }
}

private val RootMetadata = mapOf("root" to true)

private fun pop(backStack: MutableList<NavKey>) {
    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
}

private fun showRoot(backStack: MutableList<NavKey>, route: NavKey) {
    backStack.clear()
    backStack.add(route)
}

@Composable
private fun RowScope.RootNavItem(label: String, kind: NavMarkKind, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { NavMark(kind) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = FreshendaColors.Primary,
            selectedTextColor = FreshendaColors.Primary,
            indicatorColor = FreshendaColors.SurfaceTint.copy(alpha = .72f),
            unselectedIconColor = FreshendaColors.Unknown,
            unselectedTextColor = FreshendaColors.Unknown,
        ),
    )
}

private enum class NavMarkKind { FRIDGE, DUE, SETTINGS }

@Composable
private fun NavMark(kind: NavMarkKind) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height
        when (kind) {
            NavMarkKind.FRIDGE -> {
                drawRoundRect(color, Offset(w * .2f, h * .08f), Size(w * .6f, h * .84f), CornerRadius(3.dp.toPx()), style = Stroke(stroke))
                drawLine(color, Offset(w * .2f, h * .43f), Offset(w * .8f, h * .43f), stroke)
                drawLine(color, Offset(w * .33f, h * .23f), Offset(w * .33f, h * .31f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w * .33f, h * .57f), Offset(w * .33f, h * .7f), stroke, StrokeCap.Round)
            }
            NavMarkKind.DUE -> {
                drawCircle(color, w * .38f, style = Stroke(stroke))
                drawLine(color, center, Offset(w * .5f, h * .27f), stroke, StrokeCap.Round)
                drawLine(color, center, Offset(w * .67f, h * .59f), stroke, StrokeCap.Round)
            }
            NavMarkKind.SETTINGS -> {
                listOf(.25f to .65f, .5f to .35f, .75f to .6f).forEach { (y, x) ->
                    drawLine(color, Offset(w * .15f, h * y), Offset(w * .85f, h * y), stroke, StrokeCap.Round)
                    drawCircle(FreshendaColors.Card, w * .1f, Offset(w * x, h * y))
                    drawCircle(color, w * .1f, Offset(w * x, h * y), style = Stroke(stroke))
                }
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
