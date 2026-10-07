package com.cake.freshenda.ui.components

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.cake.freshenda.data.local.FoodBatchEntity
import com.cake.freshenda.model.ExpiryResult
import com.cake.freshenda.model.FreshnessStatus
import com.cake.freshenda.ui.theme.FreshendaColors
import com.cake.freshenda.ui.theme.MotionEase
import com.cake.freshenda.ui.theme.FreshendaMotion

@Composable
fun SharedTransitionScope.BatchIcon(batch: FoodBatchEntity, result: ExpiryResult, source: String, size: Dp) {
    FoodIcon(
        batch.iconKey,
        batch.displayName,
        modifier = Modifier.sharedElement(
            sharedContentState = rememberSharedContentState("$source/${batch.id}"),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            boundsTransform = { _, _ -> tween(FreshendaMotion.Move, easing = MotionEase) },
        ),
        size = size,
        fraction = result.fractionRemaining,
        status = result.status,
    )
}

@Composable
fun FoodIcon(
    iconKey: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    fraction: Float? = null,
    status: FreshnessStatus = FreshnessStatus.UNKNOWN,
) {
    Box(modifier = modifier.size(size).semantics { contentDescription = name }, contentAlignment = Alignment.Center) {
        if (fraction != null) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                drawArc(
                    color = FreshendaColors.RingTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke, stroke),
                    size = Size(this.size.width - stroke * 2, this.size.height - stroke * 2),
                    style = Stroke(stroke),
                )
                drawArc(
                    color = statusColor(status),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(stroke, stroke),
                    size = Size(this.size.width - stroke * 2, this.size.height - stroke * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Image(
            painter = painterResource(FoodIconRegistry.resolve(iconKey)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(if (fraction == null) 0.dp else 5.dp),
        )
    }
}

private fun statusColor(status: FreshnessStatus): Color = when (status) {
    FreshnessStatus.OVERDUE -> FreshendaColors.Overdue
    FreshnessStatus.TODAY, FreshnessStatus.DUE_SOON -> FreshendaColors.DueSoon
    FreshnessStatus.OK -> FreshendaColors.Primary
    FreshnessStatus.REVIEW, FreshnessStatus.UNKNOWN -> FreshendaColors.Unknown
}
