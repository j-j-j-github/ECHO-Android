package com.example.echo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import kotlinx.coroutines.delay

@Composable
fun ReportSuccessOverlay(
    onDismiss: () -> Unit
) {
    // Animation states for a smooth fade-in and scale effect
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        // Animate in
        alpha.animateTo(1f, tween(500))
        scale.animateTo(1f, tween(500))

        // Wait for 2 seconds
        delay(2000)

        // Animate out
        alpha.animateTo(0f, tween(300))
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)) // Dark background
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            // Simple "eye" icon simulation
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
                    .border(1.dp, Stone600, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(NeonWhite, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "W A T C H E D",
                style = VoidTypography.bodyLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    color = NeonWhite,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "THE VOID HAS BEEN NOTIFIED.",
                style = VoidTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = Stone600,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}