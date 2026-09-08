package com.cake.freshenda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cake.freshenda.ui.AppDeepLink
import com.cake.freshenda.ui.FreshendaApp
import com.cake.freshenda.ui.theme.FreshendaTheme

class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<AppDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink = intent.toDeepLink()
        setContent {
            FreshendaTheme {
                FreshendaApp(
                    container = (application as XianxuApplication).container,
                    deepLink = deepLink,
                    onDeepLinkConsumed = { deepLink = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.toDeepLink()
    }

    private fun Intent.toDeepLink(): AppDeepLink? {
        val destination = getStringExtra(EXTRA_DESTINATION) ?: return null
        val batchId = if (hasExtra(EXTRA_BATCH_ID)) getLongExtra(EXTRA_BATCH_ID, -1).takeIf { it > 0 } else null
        return AppDeepLink(destination, batchId)
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        const val EXTRA_BATCH_ID = "batch_id"
    }
}
