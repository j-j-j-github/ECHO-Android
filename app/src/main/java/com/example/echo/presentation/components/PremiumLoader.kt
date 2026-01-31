package com.example.echo.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.echo.core.theme.NeonWhite

@Composable
fun PremiumLoader(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    pullProgress: Float // 0f to 1f based on pull distance
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")

    // Continuous rotation for when it is actually refreshing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Calculate rotation:
    // - If refreshing: spin continuously
    // - If pulling: rotate based on pull distance
    val currentRotation = if (isRefreshing) rotation else (pullProgress * 360f)

    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val size = Size(diameter, diameter)

            // 1. Draw Faded Track (Background Ring)
            drawArc(
                color = NeonWhite.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw Active Arc (The White Ring)
            // If dragging, arc grows. If refreshing, arc is fixed size but spins.
            val sweepAngle = if (isRefreshing) 270f else (pullProgress * 270f).coerceIn(0f, 270f)

            drawArc(
                color = NeonWhite,
                startAngle = currentRotation - 90f, // Start from top
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}