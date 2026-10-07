package com.cake.freshenda.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

// Fast response with a gentle settle; Compose honors the system animation scale.
val MotionEase = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val MotionMoveEase = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

object FreshendaMotion {
    const val Press = 100
    const val Release = 160
    const val Enter = 200
    const val Exit = 120
    const val Move = 220
}
