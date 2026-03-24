package com.example.echo.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun FrequencyModal(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    currentFrequency: Int,
    onDismiss: () -> Unit,
    onFrequencySet: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()

    val feelings = listOf("DISTURBED", "HEAVY", "NEUTRAL", "CALM", "RADIANT")
    val maxIdx = feelings.size - 1

    val spectrum = listOf(
        Color(0xFFFF453A), Color(0xFFFF9F0A), Color(0xFF94A3B8),
        Color(0xFF64D2FF), Color(0xFF34C759)
    )

    val neutralSilver = Color(0xFF94A3B8)

    val animatedPosition = remember { Animatable(currentFrequency.toFloat()) }
    val currentIdx = animatedPosition.value.roundToInt().coerceIn(0, maxIdx)

    val smoothActiveColor by animateColorAsState(
        targetValue = spectrum[currentIdx],
        animationSpec = tween(400),
        label = "colorAnim"
    )

    // ✅ TACTILE FEEDBACK LOGIC
    // We track the last "tick" we vibrated at. 0.1f matches our sub-line density.
    var lastTriggeredTick by remember { mutableIntStateOf((currentFrequency * 10)) }

    LaunchedEffect(animatedPosition.value) {
        val currentTick = (animatedPosition.value * 10).roundToInt()
        if (currentTick != lastTriggeredTick) {
            // TextHandleMove is a light, sharp "tick" perfect for dials
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastTriggeredTick = currentTick
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            animatedPosition.snapTo(currentFrequency.toFloat())
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(tween(600)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(400)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(remember { MutableInteractionSource() }, null) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(VoidBlack)
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                    .clickable(remember { MutableInteractionSource() }, null) { },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))))

                Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "TUNE YOUR FREQUENCY",
                            style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 8.sp, fontSize = 9.sp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.White.copy(0.3f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Calibrate your emotional signal before transmission.",
                        style = VoidTypography.bodyLarge.copy(fontSize = 12.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, lineHeight = 18.sp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(48.dp))

                    Text(
                        "I AM FEELING",
                        style = VoidTypography.labelSmall.copy(color = Color.Gray, letterSpacing = 3.sp, fontSize = 10.sp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        feelings[currentIdx],
                        style = VoidTypography.displayLarge.copy(fontSize = 30.sp, letterSpacing = 12.sp, color = smoothActiveColor, fontWeight = FontWeight.ExtraLight)
                    )

                    Spacer(Modifier.height(60.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        scope.launch {
                                            animatedPosition.animateTo(
                                                targetValue = animatedPosition.value.roundToInt().toFloat().coerceIn(0f, maxIdx.toFloat()),
                                                animationSpec = spring(dampingRatio = 0.75f, stiffness = 250f)
                                            )
                                            // Stronger vibration on "snap lock"
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val dragSensitivity = 340f
                                    val nextPos = (animatedPosition.value - dragAmount.x / dragSensitivity).coerceIn(-0.1f, maxIdx.toFloat() + 0.1f)
                                    scope.launch { animatedPosition.snapTo(nextPos) }
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val midX = size.width / 2
                            val spacing = 32.dp.toPx()

                            for (i in -15..40) {
                                val tickValue = i * 0.2f
                                if (tickValue < 0f || tickValue > maxIdx.toFloat()) continue

                                val x = midX + (tickValue - animatedPosition.value) * spacing * 5
                                val distance = abs(x - midX)
                                val focusRange = 90.dp.toPx()
                                val growth = (1f - (distance / focusRange)).coerceIn(0f, 1f)
                                val isMajor = (i % 5 == 0)
                                val alpha = (1f - distance / (size.width / 1.4f)).coerceIn(0f, 1f)

                                val baseHeight = if (isMajor) 34.dp.toPx() else 14.dp.toPx()
                                val maxHeight = 75.dp.toPx()
                                val currentHeight = baseHeight + (maxHeight - baseHeight) * growth
                                val strokeW = if (isMajor) (3.5.dp.toPx() + 1.5.dp.toPx() * growth) else 2.dp.toPx()
                                val centerY = 60.dp.toPx()

                                drawLine(Color.Black.copy(alpha = 0.4f * alpha), Offset(x + 2f, centerY - (currentHeight / 2) + 2f), Offset(x + 2f, centerY + (currentHeight / 2) + 2f), strokeW)
                                drawLine(if (isMajor) smoothActiveColor.copy(alpha = alpha) else Color.White.copy(alpha = 0.18f * alpha), Offset(x, centerY - (currentHeight / 2)), Offset(x, centerY + (currentHeight / 2)), strokeW)

                                if (isMajor && alpha > 0.6f) {
                                    val moodLabel = "MOOD ${tickValue.toInt() + 1}"
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = moodLabel,
                                        style = TextStyle(color = Color.White.copy(0.4f * alpha), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                        topLeft = Offset(x - 20.dp.toPx(), centerY + (maxHeight / 2) + 25.dp.toPx())
                                    )
                                }
                            }
                        }

                        Box(
                            Modifier
                                .padding(top = 10.dp)
                                .width(84.dp)
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        0.0f to smoothActiveColor.copy(alpha = 0.35f),
                                        0.3f to Color.Transparent,
                                        0.7f to Color.Transparent,
                                        1.0f to smoothActiveColor.copy(alpha = 0.35f)
                                    )
                                )
                                .border(0.8.dp, Color.White.copy(0.2f), RoundedCornerShape(16.dp))
                        ) {
                            Box(Modifier.align(Alignment.Center).width(2.5.dp).fillMaxHeight(0.85f).background(smoothActiveColor).blur(6.dp))
                            Box(Modifier.align(Alignment.Center).width(1.5.dp).fillMaxHeight(0.8f).background(Color.White))
                        }
                    }

                    Text(
                        text = "SLIDE TO TUNE YOUR MOOD",
                        style = VoidTypography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 8.sp,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Light
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(Modifier.height(40.dp))

                    Box(
                        Modifier
                            .fillMaxWidth().height(58.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(NeonWhite)
                            .clickable {
                                onFrequencySet(currentIdx)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SET FREQUENCY",
                            style = VoidTypography.labelSmall.copy(color = VoidBlack, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        )
                    }
                }
            }
        }
    }
}