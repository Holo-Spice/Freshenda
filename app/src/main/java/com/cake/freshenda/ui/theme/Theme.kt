package com.cake.freshenda.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FreshendaColorScheme = lightColorScheme(
    primary = FreshendaColors.Primary,
    onPrimary = FreshendaColors.OnPrimary,
    primaryContainer = FreshendaColors.SurfaceTint,
    onPrimaryContainer = FreshendaColors.OnSurface,
    secondary = FreshendaColors.Secondary,
    background = FreshendaColors.Background,
    onBackground = FreshendaColors.OnSurface,
    surface = FreshendaColors.Background,
    onSurface = FreshendaColors.OnSurface,
    surfaceVariant = FreshendaColors.Card,
    onSurfaceVariant = FreshendaColors.OnSurface,
    error = FreshendaColors.Overdue,
)

@Composable
fun FreshendaTheme(content: @Composable () -> Unit) {
    // 状态色承担固定语义，因此不使用动态取色覆盖品牌配色。
    MaterialTheme(colorScheme = FreshendaColorScheme, typography = Typography, content = content)
}
