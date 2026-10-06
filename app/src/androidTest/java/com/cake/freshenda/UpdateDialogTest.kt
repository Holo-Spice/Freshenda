package com.cake.freshenda

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cake.freshenda.ui.components.UpdateDialog
import com.cake.freshenda.ui.theme.FreshendaTheme
import com.cake.freshenda.update.UpdateInfo
import com.cake.freshenda.update.UpdateResult
import com.cake.freshenda.update.UpdateUiState
import com.cake.freshenda.update.DownloadProgress
import com.cake.freshenda.update.DownloadStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UpdateDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsVersionsAndPreservesFullMigrationInstructions() {
        val details = "先在旧版导出库存备份，再卸载旧版、安装正式版并恢复库存。未导出备份就卸载会丢失本地库存。"
        val info = UpdateInfo(
            schemaVersion = 1,
            versionCode = BuildConfig.VERSION_CODE.toLong() + 1,
            versionName = "9.9.9",
            minSdk = 26,
            releaseUrl = "https://github.com/Holo-Spice/Freshenda/releases/tag/v9.9.9",
            notes = listOf("优化动效与页面布局。", "首次迁移到正式版前请导出库存备份。"),
            details = listOf(details),
            apkUrl = "https://github.com/Holo-Spice/Freshenda/releases/download/v9.9.9/Freshenda-v9.9.9-release.apk",
        )
        var ignored = false
        compose.setContent {
            FreshendaTheme {
                UpdateDialog(
                    UpdateUiState(latest = info, result = UpdateResult.AVAILABLE, dialogVisible = true),
                    onDismiss = {},
                    onIgnore = { ignored = true },
                    onRetry = {},
                    onDownload = {},
                    onCancelDownload = {},
                    onInstall = {},
                )
            }
        }
        compose.onNodeWithText(BuildConfig.VERSION_NAME).assertIsDisplayed()
        compose.onNodeWithText(info.versionName).assertIsDisplayed()
        compose.onNodeWithText(info.notes.last()).assertIsDisplayed()
        compose.onNodeWithText("查看完整说明").performClick()
        compose.onNodeWithText(details).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("忽略此版本").performClick()
        compose.runOnIdle { assertTrue(ignored) }
    }

    @Test fun downloadingCanBeDismissedOrCancelledAndReadyCanBeInstalled() {
        val state = mutableStateOf(UpdateUiState(
            latest = UpdateInfo(1, 9, "1.2.2", 26, "https://github.com/Holo-Spice/Freshenda/releases/tag/v1.2.2"),
            result = UpdateResult.AVAILABLE,
            download = DownloadProgress(DownloadStatus.DOWNLOADING, 370_000, 1_000_000),
        ))
        var dismissed = false
        var cancelled = false
        var installed = false
        compose.setContent {
            FreshendaTheme {
                UpdateDialog(state.value, onDismiss = { dismissed = true }, onIgnore = {}, onRetry = {},
                    onDownload = {}, onCancelDownload = { cancelled = true }, onInstall = { installed = true })
            }
        }
        compose.onNodeWithText("37%").assertIsDisplayed()
        compose.onNodeWithText("后台下载").performClick()
        compose.onNodeWithText("取消下载").performClick()
        compose.runOnIdle {
            assertTrue(dismissed && cancelled)
            state.value = state.value.copy(download = DownloadProgress(DownloadStatus.READY, 1_000_000, 1_000_000))
        }
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("安装更新").performClick()
        compose.runOnIdle { assertTrue(installed) }
    }
}
