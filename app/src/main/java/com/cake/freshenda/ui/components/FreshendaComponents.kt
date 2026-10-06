package com.cake.freshenda.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import java.math.BigDecimal

@Composable
fun BrandHeader(title: String, subtitle: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        if (leading != null) {
            leading()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(7.dp).background(FreshendaColors.Primary, CircleShape))
                Text("鲜序 / FRESHENDA", style = MaterialTheme.typography.labelSmall, color = FreshendaColors.Unknown)
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = FreshendaColors.OnSurface)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
    }
}

/** Keep press motion on the graphics layer; Surface owns ripple, semantics and cancellation. */
@Composable
fun PressableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = FreshendaColors.Card,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) .975f else 1f,
        animationSpec = tween(if (pressed) 100 else 160, easing = MotionEase),
        label = "cardPress",
    )
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        color = color,
        shape = shape,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun SharedTransitionScope.BatchRow(batch: FoodBatchEntity, result: ExpiryResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PressableSurface(onClick, modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BatchIcon(batch, result, source = "due", size = 60.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(batch.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${locationText(batch.storageLocation)} · ${quantityText(batch.quantityMilli)} ${batch.quantityUnit}", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
                Text(result.effective?.description ?: "尚未选择日期依据", style = MaterialTheme.typography.labelSmall, color = FreshendaColors.Unknown)
            }
            StatusBadge(result.statusText, result.status)
        }
    }
}

@Composable
fun StatusBadge(text: String, status: FreshnessStatus) {
    val color = freshnessColor(status)
    Text(
        text,
        modifier = Modifier.background(color.copy(alpha = .09f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

fun freshnessColor(status: FreshnessStatus): Color = when (status) {
    FreshnessStatus.OVERDUE -> FreshendaColors.Overdue
    FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON -> FreshendaColors.DueSoon
    FreshnessStatus.OK -> FreshendaColors.Primary
    FreshnessStatus.UNKNOWN, FreshnessStatus.REVIEW -> FreshendaColors.Unknown
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(104.dp).background(FreshendaColors.GlassSoft, CircleShape), contentAlignment = Alignment.Center) {
            FoodIcon("food_avocado", "", size = 76.dp)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown, textAlign = TextAlign.Center)
    }
}

fun batchExpiry(batch: FoodBatchEntity, now: Long): ExpiryResult = ExpiryCalculator.result(batch, now)

fun locationText(value: String): String = when (value) {
    "REFRIGERATED" -> "冷藏"
    "FROZEN" -> "冷冻"
    "PANTRY" -> "常温"
    else -> value
}

fun quantityText(milli: Long): String = BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()

