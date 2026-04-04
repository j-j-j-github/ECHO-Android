package com.example.echo.presentation.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.NeonWhite
import com.example.echo.core.theme.VoidBlack
import com.example.echo.core.theme.VoidTypography
import kotlin.math.*

// ==========================================
// ✅ SOUL ENERGY OVERLAY (POPUP) WITH SYNCED METER
// ==========================================
@Composable
fun SoulEnergyOverlay(
    energyLevel: Float,
    onEnergyChange: (Float) -> Unit, // Kept for compatibility, though unused now
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val colorMutedGreen = Color(0xFF10B981)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(VoidBlack)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // ✅ MODIFIED: Title is fully white
                        Text(
                            text = "SOUL ENERGY",
                            style = VoidTypography.labelSmall.copy(
                                color = NeonWhite,
                                letterSpacing = 8.sp,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(Modifier.height(12.dp))
                        // ✅ MODIFIED: Separation line is vividly green
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            colorMutedGreen.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                Text(
                    "HOW TO RECHARGE",
                    style = VoidTypography.labelSmall.copy(
                        color = NeonWhite,
                        letterSpacing = 4.sp,
                        fontSize = 10.sp
                    )
                )

                Spacer(Modifier.height(16.dp))

                // ✅ MODIFIED: Added Daily Login and adjusted spacing to perfectly fit 4 items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EnergyAction("+ LOGIN", "Daily", colorMutedGreen)
                    EnergyAction("+ POST", "An Echo", colorMutedGreen)
                    EnergyAction("+ RESONATE", "Echoes", colorMutedGreen)
                    EnergyAction("+ EARN", "Relates", colorMutedGreen)
                }

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EnergyPulsar(energyLevel = energyLevel, modifier = Modifier.fillMaxSize())
                }

                Spacer(Modifier.height(32.dp))

                val percentage = (energyLevel * 100).toInt()
                val statusText = when {
                    percentage == 100 -> "REVELATION"
                    percentage >= 80 -> "SURGING"
                    percentage >= 50 -> "RISING"
                    percentage >= 20 -> "FORMING"
                    else -> "QUIET"
                }
                val statusColor = if (percentage == 100) colorMutedGreen else NeonWhite

                Text(
                    "CURRENT CAPACITY",
                    style = VoidTypography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 3.sp,
                        fontSize = 10.sp
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$percentage",
                        style = VoidTypography.displayLarge.copy(
                            fontSize = 48.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Light
                        )
                    )
                    Text(
                        text = "%",
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 18.sp,
                            color = statusColor.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    statusText,
                    style = VoidTypography.labelSmall.copy(
                        fontSize = 12.sp,
                        color = statusColor.copy(alpha = 0.8f),
                        letterSpacing = 4.sp
                    )
                )

                Spacer(Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WHAT IS SOUL ENERGY?",
                        style = VoidTypography.labelSmall.copy(
                            color = colorMutedGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Energy represents how active and helpful a soul is in the void. Build it slowly to earn the power to Spotlight your voice.",
                        style = VoidTypography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "WHAT IS SPOTLIGHT?",
                        style = VoidTypography.labelSmall.copy(
                            color = colorMutedGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "When you reach 100% capacity, you can spend it to push an Echo to the top of the main feed for maximum visibility.",
                        style = VoidTypography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ✅ MODIFIED: Slightly dropped font size to ensure 4 items fit nicely
@Composable
fun EnergyAction(title: String, subtitle: String, tintColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = VoidTypography.labelSmall.copy(
                color = tintColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = VoidTypography.bodyLarge.copy(
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        )
    }
}

private data class RenderDot(
    val x: Float, val y: Float, val z: Float,
    val color: Color, val radius: Float, val alpha: Float, val isStreakHead: Boolean = false
)

// ==========================================
// ✅ 360 GYRO DOT MATRIX SPHERE + 2D HALO TRAILS
// ==========================================
@Composable
fun EnergyPulsar(energyLevel: Float, modifier: Modifier = Modifier.size(90.dp)) {

    val context = LocalContext.current
    var gravityX by remember { mutableFloatStateOf(0f) }
    var gravityY by remember { mutableFloatStateOf(9.8f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_GRAVITY) {
                    val alpha = 0.15f
                    gravityX = gravityX + alpha * (event.values[0] - gravityX)
                    gravityY = gravityY + alpha * (event.values[1] - gravityY)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    var timeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var startMs = -1L
        while (true) {
            withFrameMillis { frameMs ->
                if (startMs < 0L) startMs = frameMs
                timeSec = (frameMs - startMs) / 1000f
            }
        }
    }

    val smoothEnergy by animateFloatAsState(
        targetValue = energyLevel.coerceIn(0f, 1f),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "energy"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.minDimension
            val cx = (size.width / 2f)
            val cy = size.height / 2f

            val R = w * 0.36f
            val colorMutedGreen = Color(0xFF10B981)
            val colorInactiveGrey = Color(0xFF64748B)
            val colorPureWhite = Color(0xFFFFFFFF)
            val isFull = smoothEnergy > 0.98f

            val renderList = mutableListOf<RenderDot>()

            // 1. Gravity Math
            val gLen = sqrt(gravityX * gravityX + gravityY * gravityY).coerceAtLeast(0.001f)
            val downX = -gravityX / gLen
            val downY = gravityY / gLen
            val surfX = -downY
            val surfY = downX
            val fillBoundary = R - (2f * R * smoothEnergy)

            // 2. Gyro Sphere Generation
            val rows = 20
            val cols = 32
            val baseTiltX = 0.26f

            for (r in 1 until rows) {
                val phi = PI.toFloat() * r / rows
                val yRaw = cos(phi)
                val radiusAtY = sin(phi)
                for (c in 0 until cols) {
                    val theta = 2f * PI.toFloat() * c / cols + (timeSec * 0.6f)
                    val xRaw = cos(theta) * radiusAtY
                    val zRaw = sin(theta) * radiusAtY
                    val yTilted = yRaw * cos(baseTiltX) - zRaw * sin(baseTiltX)
                    val zTilted = yRaw * sin(baseTiltX) + zRaw * cos(baseTiltX)
                    val px = xRaw * R
                    val py = yTilted * R
                    val pz = zTilted * R

                    val depthNorm = (pz + R) / (2f * R)
                    val maxDotRadius = w * 0.016f
                    val dotRadius = maxDotRadius * (0.2f + 0.8f * depthNorm.pow(1.5f))

                    val projectedDepth = px * downX + py * downY
                    val surfacePosition = px * surfX + py * surfY
                    val waveOffset = sin(surfacePosition * (0.05f / (w/90f)) + timeSec * 3f) * (w * 0.02f)
                    val activeBoundary = fillBoundary + waveOffset

                    val blendBand = w * 0.08f
                    val activeRatio = if (isFull) 1f else ((projectedDepth - activeBoundary) / blendBand + 0.5f).coerceIn(0f, 1f)
                    val targetColor = if (isFull) colorMutedGreen else lerp(colorInactiveGrey, colorPureWhite, activeRatio)

                    val depthAlpha = 0.15f + 0.85f * depthNorm
                    val finalAlpha = if (isFull) depthAlpha else depthAlpha * (0.4f + 0.6f * activeRatio)
                    renderList.add(RenderDot(px, py, pz, targetColor, dotRadius, finalAlpha))
                }
            }

            // 3. 2D Halo Trails (Streaks)
            val rOrbit = R * 1.15f
            val orbitSpeed = 1.2f
            val dynamicECount = 1 + (smoothEnergy * 7).toInt()

            val trackDots = 60
            for (i in 0 until trackDots) {
                val t = 2f * PI.toFloat() * i / trackDots
                renderList.add(RenderDot(rOrbit * cos(t), rOrbit * sin(t), 0f, colorPureWhite, w * 0.008f, 0.25f))
            }

            // Streaks
            for (i in 0 until dynamicECount) {
                val headAngle = timeSec * orbitSpeed + ((2.0 * PI / dynamicECount) * i).toFloat()

                val trailLength = 30
                for (j in 0 until trailLength) {
                    val trailFactor = j.toFloat() / trailLength
                    val angle = headAngle - (trailFactor * 0.4f)

                    val px = rOrbit * cos(angle)
                    val py = rOrbit * sin(angle)

                    val sizeFactor = exp(-j.toFloat() * 0.2f)
                    val alphaFactor = exp(-j.toFloat() * 0.15f)

                    renderList.add(
                        RenderDot(
                            px, py, 0f,
                            colorPureWhite,
                            w * 0.014f * sizeFactor,
                            1f * alphaFactor,
                            isStreakHead = j == 0
                        )
                    )
                }
            }

            // 4. Render
            renderList.sortBy { it.z }
            renderList.forEach { dot ->
                val screenX = cx + dot.x
                val screenY = cy + dot.y
                if (dot.isStreakHead) {
                    drawCircle(color = dot.color.copy(alpha = 0.15f), radius = dot.radius * 2.8f, center = Offset(screenX, screenY))
                    drawCircle(color = dot.color.copy(alpha = 0.4f), radius = dot.radius * 1.8f, center = Offset(screenX, screenY))
                }
                drawCircle(color = dot.color.copy(alpha = dot.alpha), radius = dot.radius, center = Offset(screenX, screenY))
            }
        }
    }
}