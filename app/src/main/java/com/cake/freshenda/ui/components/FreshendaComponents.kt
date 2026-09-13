package com.cake.freshenda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.expiry.ExpiryCalculator
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.theme.FreshendaColors

@Composable
fun BrandHeader(title: String, subtitle: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(FreshendaColors.Primary, FreshendaColors.HeaderAccent),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = .07f), radius = size.minDimension * .8f, center = Offset(size.width * .96f, -size.height * .12f))
            drawCircle(FreshendaColors.SurfaceTint.copy(alpha = .09f), radius = size.minDimension * .48f, center = Offset(size.width * .72f, size.height * 1.08f))
            drawLine(FreshendaColors.GlassBorder.copy(alpha = .4f), Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), 1.dp.toPx())
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
            if (leading != null) {
                leading()
                Spacer(Modifier.height(6.dp))
            } else {
                Text("鲜序", color = FreshendaColors.OnPrimary.copy(alpha = .82f), style = MaterialTheme.typography.labelLarge)
            }
            Text(title, color = FreshendaColors.OnPrimary, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = FreshendaColors.SurfaceTint, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun BatchRow(batch: FoodBatchEntity, result: ExpiryResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        color = FreshendaColors.Glass,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, FreshendaColors.GlassBorder),
        shadowElevation = 2.dp,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FoodIcon(batch.iconKey, batch.displayName, size = 62.dp, fraction = result.fractionRemaining, status = result.status)
            Column(Modifier.weight(1f)) {
                Text(batch.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${locationText(batch.storageLocation)} · ${quantityText(batch.quantityMilli)}${batch.quantityUnit}", style = MaterialTheme.typography.bodyMedium)
                Text(result.effective?.description ?: "尚未选择日期依据", style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
            }
            StatusBadge(result.statusText, result.status)
        }
    }
}

@Composable
fun StatusBadge(text: String, status: FreshnessStatus) {
    val background = when (status) {
        FreshnessStatus.OVERDUE -> FreshendaColors.Overdue.copy(alpha = .24f)
        FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON -> FreshendaColors.DueSoon.copy(alpha = .28f)
        FreshnessStatus.OK -> FreshendaColors.SurfaceTint
        FreshnessStatus.UNKNOWN, FreshnessStatus.REVIEW -> FreshendaColors.RingTrack
    }
    Text(text, modifier = Modifier.background(background, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(FreshendaColors.SurfaceTint)
                drawLine(FreshendaColors.Primary, Offset(size.width * .3f, size.height * .5f), Offset(size.width * .7f, size.height * .5f), 4.dp.toPx(), StrokeCap.Round)
                drawLine(FreshendaColors.Primary, Offset(size.width * .5f, size.height * .3f), Offset(size.width * .5f, size.height * .7f), 4.dp.toPx(), StrokeCap.Round)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = FreshendaColors.Unknown)
    }
}

fun batchExpiry(batch: FoodBatchEntity, now: Long): ExpiryResult = ExpiryCalculator.result(batch, now)

fun locationText(value: String): String = when (value) {
    "REFRIGERATED" -> "冷藏"
    "FROZEN" -> "冷冻"
    "PANTRY" -> "常温"
    else -> value
}

fun quantityText(milli: Long): String = if (milli % 1000L == 0L) (milli / 1000L).toString() else "%.3f".format(milli / 1000.0).trimEnd('0').trimEnd('.')
