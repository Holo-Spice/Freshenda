package com.cake.freshenda

import android.graphics.Bitmap
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.platform.app.InstrumentationRegistry
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.data.UserSettings
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.FoodCatalog
import com.cake.freshenda.model.AddBatchDraft
import com.cake.freshenda.ui.DueRoute
import com.cake.freshenda.ui.FridgeRoute
import com.cake.freshenda.ui.DetailRoute
import com.cake.freshenda.ui.EditRoute
import com.cake.freshenda.ui.due.DueScreen
import com.cake.freshenda.ui.fridge.FridgeScreen
import com.cake.freshenda.ui.detail.DetailScreen
import com.cake.freshenda.ui.editor.EditorScreen
import com.cake.freshenda.ui.picker.PickerScreen
import com.cake.freshenda.ui.settings.SettingsScreen
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.FreshendaTheme
import com.cake.freshenda.update.UpdateUiState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class FoodExperienceTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 10, 7)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun soonerDatesAppearBeforeFrozenFoodAndRowsRemainClickable() {
        val batches = listOf(
            batch(1, "pork_strips", "猪肉丝", 5, "FROZEN", "LABEL"),
            batch(2, "shiitake", "鲜香菇", 2),
            batch(3, "spinach", "菠菜", 2),
            batch(4, "fresh_noodles", "鲜面条", 3, kind = "LABEL"),
            batch(5, "pork_cubes", "猪肉丁", 5, "FROZEN", "LABEL"),
        )
        var clicked = 0L
        compose.setContent {
            FreshendaTheme {
                SharedTransitionLayout(Modifier.fillMaxSize().background(FreshendaColors.Background)) {
                    NavDisplay(
                        backStack = rememberNavBackStack(DueRoute),
                        entryProvider = entryProvider { entry<DueRoute> { DueScreen(batches, now) { clicked = it } } },
                    )
                }
            }
        }
        compose.onNodeWithText("近 3 天内").assertIsDisplayed()
        compose.onNodeWithText("鲜香菇").assertIsDisplayed()
        compose.onNodeWithText("猪肉丝").performScrollTo().assertIsDisplayed()
        val nearY = compose.onNodeWithText("鲜面条").fetchSemanticsNode().boundsInRoot.top
        val laterY = compose.onNodeWithText("猪肉丝").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Three days should precede five days", nearY < laterY)
        compose.onNodeWithText("之后安排").assertIsDisplayed()
        compose.onNodeWithText("先吃这些").performScrollTo()
        screenshot("food-due-chronological.png")
        compose.onNodeWithText("猪肉丝").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1L, clicked) }
    }

    @Test fun searchingAnAliasEscapesThePreviousCategoryAndSelectsTheFood() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = Json.decodeFromString<FoodCatalog>(context.assets.open("food_catalog.v1.json").bufferedReader().use { it.readText() })
        var selected = ""
        var clearFocus: () -> Unit = {}
        compose.setContent {
            FreshendaTheme {
                val focusManager = LocalFocusManager.current
                clearFocus = { focusManager.clearFocus(force = true) }
                Box(Modifier.fillMaxSize().background(FreshendaColors.Background)) {
                    PickerScreen(catalog, emptyList(), emptyList(), onBack = {}, onFood = { selected = it }, onCustom = {})
                }
            }
        }
        compose.onNodeWithText("绿叶菜").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("肉丝")
        compose.onNodeWithText("全部").assertIsSelected()
        compose.onNodeWithText("猪肉丝").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("pork_strips", selected) }
        compose.onNode(hasSetTextAction()).performTextReplacement("鸡丁")
        compose.onNodeWithText("鸡肉丁").assertIsDisplayed()
        compose.runOnIdle { clearFocus() }
        screenshot("food-search-chicken.png")
        compose.onNodeWithText("鸡肉丁").performClick()
        compose.runOnIdle { assertEquals("chicken_cubes", selected) }
        compose.onNode(hasSetTextAction()).performTextReplacement("肉")
        compose.runOnIdle { clearFocus() }
        screenshot("food-search-meat.png")
        compose.onNode(hasSetTextAction()).performTextReplacement("猪肉")
        compose.runOnIdle { clearFocus() }
        screenshot("style-pork-icons.png")
    }

    @Test fun inventoryDetailsAndEditorStayUsableOnCompactScreensWithLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = Json.decodeFromString<FoodCatalog>(context.assets.open("food_catalog.v1.json").bufferedReader().use { it.readText() })
        val batches = listOf(batch(1, "pork_strips", "猪肉丝", 5, "FROZEN", "LABEL"), batch(2, "shiitake", "鲜香菇", 2), batch(3, "spinach", "菠菜", 2))
        val compact = mutableStateOf(false)
        var saved: AddBatchDraft? = null
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (compact.value) 1.4f else 1f)) {
                FreshendaTheme {
                    Box(if (compact.value) Modifier.width(320.dp) else Modifier.fillMaxSize()) {
                        SharedTransitionLayout(Modifier.fillMaxSize().background(FreshendaColors.Background)) {
                            val stack = rememberNavBackStack(FridgeRoute)
                            NavDisplay(backStack = stack, entryProvider = entryProvider {
                                entry<FridgeRoute> {
                                    FridgeScreen(batches, now, {}, { stack.add(DueRoute) }) { stack.add(DetailRoute(it)) }
                                }
                                entry<DueRoute> {
                                    DueScreen(batches, now) { stack.add(DetailRoute(it, "due")) }
                                }
                                entry<DetailRoute> { route ->
                                    DetailScreen(batches.first { it.id == route.batchId }, emptyList(), now, route.source,
                                        onBack = { stack.removeAt(stack.lastIndex) }, onEdit = { stack.add(EditRoute(route.batchId)) },
                                        onConsume = { _, _ -> }, onOpen = {}, onDiscard = {}, onMove = {}, onSplit = { _, _ -> }, onSetDate = {})
                                }
                                entry<EditRoute> { route ->
                                    val batch = batches.first { it.id == route.batchId }
                                    EditorScreen(catalog.foods.first { it.id == batch.foodDefinitionId }, catalog, null, batch,
                                        onBack = { stack.removeAt(stack.lastIndex) }, onSave = { saved = it })
                                }
                            })
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("我的冰箱").assertIsDisplayed()
        screenshot("style-fridge.png")
        compose.onNodeWithText("猪肉丝").performScrollTo().performClick()
        compose.onNodeWithText("食材详情").assertIsDisplayed()
        screenshot("style-detail.png")
        compose.onNodeWithText("编辑").performClick()
        compose.onNodeWithText("修改食材").assertIsDisplayed()
        screenshot("style-editor.png")
        compose.runOnIdle { compact.value = true }
        compose.onNodeWithContentDescription("增加数量").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("2")).assertIsDisplayed()
        compose.onNodeWithText("保存修改").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2000L, saved?.quantityMilli) }
        compose.onNodeWithText("修改食材").performScrollTo()
        screenshot("style-editor-compact.png")
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("2 份食材需要留意").performScrollTo().performClick()
        compose.onNodeWithText("先吃这些").assertIsDisplayed()
        val name = compose.onNodeWithText("鲜香菇", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val badge = compose.onAllNodesWithText("2天后", useUnmergedTree = true)[0].fetchSemanticsNode().boundsInRoot
        assertTrue("On compact screens the status moves below the food name", badge.top > name.bottom)
        screenshot("style-due-compact.png")
    }

    @Test fun settingsKeepWholeRowSwitchesAndHourControlsAccessible() {
        val settings = mutableStateOf(UserSettings())
        compose.setContent {
            FreshendaTheme {
                Box(Modifier.fillMaxSize().background(FreshendaColors.Background)) {
                    SettingsScreen(settings.value, null, true,
                        onSetEnabled = { settings.value = settings.value.copy(remindersEnabled = it) },
                        onRequestPermission = {}, onDailySummary = { settings.value = settings.value.copy(dailySummary = it) },
                        onOpened = {}, onCustom = {}, onHour = { settings.value = settings.value.copy(reminderHour = it) },
                        onTest = {}, onExport = {}, onImport = {}, updateState = UpdateUiState(), onCheckUpdate = {})
                }
            }
        }
        screenshot("style-settings.png")
        compose.onNodeWithText("到期提醒").performClick()
        compose.onNodeWithContentDescription("推后一小时").performClick()
        compose.runOnIdle {
            assertTrue(settings.value.remindersEnabled)
            assertEquals(21, settings.value.reminderHour)
        }
        compose.onNodeWithText("21:00").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun batch(id: Long, food: String, name: String, days: Long, storage: String = "REFRIGERATED", kind: String = "SUGGESTED") = FoodBatchEntity(
        id = id, foodDefinitionId = food, displayName = name, iconKey = "food_$food", quantityMilli = 1000, quantityUnit = "份",
        storageLocation = storage, storageSection = if (storage == "FROZEN") "FREEZER_DRAWER" else "CRISPER",
        physicalState = "WHOLE", packagingState = "LOOSE", maturityState = "NOT_APPLICABLE", preparation = "NONE",
        purchasedAtEpochMillis = now - 86_400_000, stageStartedAtEpochMillis = now - 86_400_000, businessZoneId = zone.id,
        effectiveDeadlineEpochMillis = ExpiryCalculator.labelDeadline(today.plusDays(days).toEpochDay(), zone.id),
        effectiveDisplayDateEpochDay = today.plusDays(days).toEpochDay(), effectiveAnchorEpochMillis = now - 86_400_000,
        effectiveDeadlineKind = kind, ruleDataVersion = "test", createdAtEpochMillis = now, updatedAtEpochMillis = now,
    )
}
