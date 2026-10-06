package com.cake.freshenda.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FreshendaColorScheme = lightColorScheme(
    primary = FreshendaColors.Primary,
    onPrimary = FreshendaColors.OnPrimary,
    primaryContainer = FreshendaColors.SurfaceTint,
    onPrimaryContainer = FreshendaColors.OnSurface,
    secondary = FreshendaColors.Secondary,
    onSecondary = FreshendaColors.OnPrimary,
    secondaryContainer = FreshendaColors.SurfaceTint,
    onSecondaryContainer = FreshendaColors.Primary,
    tertiary = FreshendaColors.Primary,
    onTertiary = FreshendaColors.OnPrimary,
    tertiaryContainer = FreshendaColors.GlassSoft,
    onTertiaryContainer = FreshendaColors.OnSurface,
    background = FreshendaColors.Background,
    onBackground = FreshendaColors.OnSurface,
    surface = FreshendaColors.Card,
    onSurface = FreshendaColors.OnSurface,
    surfaceVariant = FreshendaColors.Glass,
    onSurfaceVariant = FreshendaColors.Unknown,
    outline = FreshendaColors.Secondary,
    outlineVariant = FreshendaColors.GlassBorder,
    surfaceContainer = FreshendaColors.GlassSoft,
    surfaceContainerLow = FreshendaColors.Card,
    surfaceContainerHigh = FreshendaColors.Background,
    surfaceContainerHighest = FreshendaColors.RingTrack,
    surfaceContainerLowest = FreshendaColors.Card,
    surfaceBright = FreshendaColors.Card,
    surfaceDim = FreshendaColors.Background,
    surfaceTint = FreshendaColors.Primary,
    inverseSurface = FreshendaColors.Primary,
    inverseOnSurface = FreshendaColors.OnPrimary,
    inversePrimary = FreshendaColors.SurfaceTint,
    error = FreshendaColors.Overdue,
    onError = FreshendaColors.OnPrimary,
    errorContainer = FreshendaColors.Overdue.copy(alpha = .1f),
    onErrorContainer = FreshendaColors.Overdue,
)

@Composable
fun FreshendaTheme(content: @Composable () -> Unit) {
    // 状态色承担固定语义，因此不使用动态取色覆盖品牌配色。
    MaterialTheme(
        colorScheme = FreshendaColorScheme,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
