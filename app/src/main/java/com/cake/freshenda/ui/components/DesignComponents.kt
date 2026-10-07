package com.cake.freshenda.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.FreshendaShapes
import com.cake.freshenda.ui.theme.FreshendaSpacing

enum class UiSymbol { BACK, NEXT, ADD, REMOVE, CLOSE, SEARCH, CALENDAR }

/** Same optical size and rounded stroke as the bottom navigation icons. */
@Composable
fun UiIcon(symbol: UiSymbol, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier.size(20.dp)) {
        val unit = size.minDimension / 24f
        val stroke = 1.8f * unit
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * unit, y1 * unit), Offset(x2 * unit, y2 * unit), stroke, StrokeCap.Round)
        when (symbol) {
            UiSymbol.BACK -> { segment(14f, 5f, 7f, 12f); segment(7f, 12f, 14f, 19f) }
            UiSymbol.NEXT -> { segment(9f, 5f, 16f, 12f); segment(16f, 12f, 9f, 19f) }
            UiSymbol.ADD -> { segment(5f, 12f, 19f, 12f); segment(12f, 5f, 12f, 19f) }
            UiSymbol.REMOVE -> segment(5f, 12f, 19f, 12f)
            UiSymbol.CLOSE -> { segment(6f, 6f, 18f, 18f); segment(18f, 6f, 6f, 18f) }
            UiSymbol.SEARCH -> {
                drawCircle(color, 6.5f * unit, Offset(10.5f * unit, 10.5f * unit), style = Stroke(stroke))
                segment(15.5f, 15.5f, 21f, 21f)
            }
            UiSymbol.CALENDAR -> {
                drawRoundRect(color, Offset(4f * unit, 5f * unit), androidx.compose.ui.geometry.Size(16f * unit, 16f * unit), androidx.compose.ui.geometry.CornerRadius(3f * unit), style = Stroke(stroke))
                segment(4f, 10f, 20f, 10f); segment(8f, 3f, 8f, 7f); segment(16f, 3f, 16f, 7f)
                drawCircle(color, 1.2f * unit, Offset(9f * unit, 15f * unit))
            }
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    TextButton(onClick, contentPadding = PaddingValues(end = 12.dp), modifier = Modifier.heightIn(min = 48.dp)) {
        UiIcon(UiSymbol.BACK)
        Spacer(Modifier.width(4.dp))
        Text("返回")
    }
}

@Composable
fun ChoiceChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = label,
        shape = CircleShape, border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FreshendaColors.GlassSoft,
            labelColor = FreshendaColors.Unknown,
            selectedContainerColor = FreshendaColors.Primary,
            selectedLabelColor = FreshendaColors.OnPrimary,
        ),
    )
}

@Composable
fun FreshendaButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = FreshendaShapes.Control, content = content)
}

@Composable
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = FreshendaShapes.Control,
        border = BorderStroke(1.dp, FreshendaColors.GlassBorder), content = content)
}

@Composable
fun freshendaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FreshendaColors.Card,
    unfocusedContainerColor = FreshendaColors.Card,
    focusedBorderColor = FreshendaColors.Primary,
    unfocusedBorderColor = FreshendaColors.GlassBorder,
    focusedLabelColor = FreshendaColors.Primary,
    unfocusedLabelColor = FreshendaColors.Unknown,
)

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null, accent: Color? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (accent != null) Box(Modifier.size(6.dp).background(accent, CircleShape))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = FreshendaColors.OnSurface)
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = FreshendaColors.Unknown)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = FreshendaSpacing.Page, vertical = 6.dp)) {
        SectionHeader(title, Modifier.padding(horizontal = 2.dp))
        Surface(color = FreshendaColors.Card, shape = FreshendaShapes.Card, shadowElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(FreshendaSpacing.CardInset), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
