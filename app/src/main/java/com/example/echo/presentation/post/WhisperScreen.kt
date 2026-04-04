package com.example.echo.presentation.post

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun WhisperOverlay(
    onDismiss: () -> Unit,
    onSend: (String, String, String?, Boolean) -> Unit,
    initialText: String = "",
    initialCategory: String = "",
    currentEnergy: Int = 0
) {
    var text by remember { mutableStateOf(initialText) }

    val actualInitialSpace = if (initialCategory.equals("All", ignoreCase = true)) "" else initialCategory
    var selectedSpace by remember { mutableStateOf(actualInitialSpace) }

    val spaces = listOf("Advice", "Vent", "Relationships", "Inner Thoughts")

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isEditing = initialText.isNotEmpty()

    val textUpper = text.uppercase()
    val isSuperUserPost = textUpper.contains("#CREATOR") ||
            textUpper.contains("#ALERT") ||
            textUpper.contains("#UPDATE")

    val isContentValid = text.trim().length >= 20
    val isSpaceValid = selectedSpace.isNotEmpty() || isEditing || isSuperUserPost
    val isReadyToPost = isContentValid && isSpaceValid

    val canSpotlight = currentEnergy >= 100 && !isEditing
    var useSpotlight by remember { mutableStateOf(false) }

    val rawProgress = (text.trim().length / 20f).coerceIn(0f, 1f)
    val smoothProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "smoothProgress"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (isContentValid) 90f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "arrowRotation"
    )

    val screenBackgroundColor by animateColorAsState(
        targetValue = if (useSpotlight) Color.White else VoidBlack,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "screenBackgroundColor"
    )

    val primaryTextColor by animateColorAsState(
        targetValue = if (useSpotlight) VoidBlack else NeonWhite,
        animationSpec = tween(600),
        label = "primaryTextColor"
    )

    val secondaryTextColor by animateColorAsState(
        targetValue = if (useSpotlight) VoidBlack.copy(0.6f) else Color.White.copy(0.5f),
        animationSpec = tween(600),
        label = "secondaryTextColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isContentValid) {
            if (useSpotlight) NeonWhite else VoidBlack
        } else {
            if (useSpotlight) Color.LightGray else Stone400
        },
        animationSpec = tween(durationMillis = 500),
        label = "iconTint"
    )

    val submitButtonBgColor by animateColorAsState(
        targetValue = if (useSpotlight) VoidBlack else Stone800,
        animationSpec = tween(600), label = "submitBtnBg"
    )

    val submitButtonProgressColor by animateColorAsState(
        targetValue = if (useSpotlight) VoidBlack else Color.White.copy(alpha = smoothProgress),
        animationSpec = tween(600), label = "submitBtnProgress"
    )

    val buttonShadow by animateDpAsState(
        targetValue = if (isReadyToPost) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 600),
        label = "buttonShadow"
    )

    val shakeOffset = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val iconSlideOffset = remember { Animatable(20f) }

    val infiniteTransition = rememberInfiniteTransition(label = "gradientTransition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientAngle"
    )

    val spotlightColors = listOf(
        Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFF84CC16),
        Color(0xFFEAB308), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFFD946EF),
        Color(0xFF8B5CF6)
    )

    val animatedGradient = remember(angle) {
        val rad = angle * (PI / 180f)
        val radius = 300f
        val startOffset = Offset(
            x = (cos(rad) * radius).toFloat() + radius,
            y = (sin(rad) * radius).toFloat() + radius
        )
        val endOffset = Offset(
            x = (cos(rad + PI) * radius).toFloat() + radius,
            y = (sin(rad + PI) * radius).toFloat() + radius
        )
        Brush.linearGradient(
            colors = spotlightColors,
            start = startOffset,
            end = endOffset
        )
    }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isEditing) {
            rotation.animateTo(360f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        } else {
            iconSlideOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    val secretMap = mapOf("#Alert" to "#FF453A", "#Update" to "#4ADE80", "#Creator" to "#FFFFFF")

    LaunchedEffect(focusRequester) {
        delay(200)
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {}
    }

    val handleSend = {
        val rawText = text.trim()
        var detectedColor: String? = null
        var cleanText = rawText

        secretMap.forEach { (tag, color) ->
            if (rawText.contains(tag, ignoreCase = true)) {
                detectedColor = color
                cleanText = rawText.replace(tag, "", ignoreCase = true).trim()
            }
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val finalSpace = if (isSuperUserPost && selectedSpace.isEmpty()) "All" else selectedSpace.ifEmpty { "Random" }
        onSend(cleanText, finalSpace, detectedColor, useSpotlight)
    }

    val triggerDeniedAnimation = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes { durationMillis = 300; -10f at 50; 10f at 100; -10f at 150; 10f at 200; -5f at 250 }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            .imePadding()
            // ✅ FIX: Added a 16dp gap between the keyboard and the bottom of the popup box
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {

        Box(
            modifier = Modifier
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp)
                .offset(x = shakeOffset.value.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(screenBackgroundColor)
                .border(1.dp, if (useSpotlight) VoidBlack.copy(0.1f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .clickable(enabled = false) {}
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    // ✅ We use the animated screenBackgroundColor directly here for smooth top-fade
                    .background(Brush.verticalGradient(listOf(screenBackgroundColor.copy(alpha = 0.05f), Color.Transparent)))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 28.dp, bottom = 100.dp)
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "RECAST YOUR ECHO" else if (useSpotlight) "ECHO TO SPOTLIGHT" else "RELEASE YOUR ECHO",
                            style = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 3.sp, color = primaryTextColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isReadyToPost) "READY TO BROADCAST" else if (!isContentValid) "MINIMUM 20 CHARACTERS REQUIRED" else "CHOOSE A SPACE TO POST",
                            style = VoidTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 2.sp,
                                color = if (!isReadyToPost && text.isNotEmpty()) Color(0xFFFF453A) else secondaryTextColor
                            )
                        )
                    }
                    Text(
                        text = "${text.length} / 500",
                        style = VoidTypography.labelSmall.copy(
                            color = if (!isContentValid && text.isNotEmpty()) Color(0xFFFF453A) else secondaryTextColor,
                            fontSize = 10.sp
                        )
                    )
                }

                // --- SEPARATE SPACE SECTION ---
                if (!isEditing) {
                    Text(
                        text = "CHOOSE A SPACE",
                        style = VoidTypography.labelSmall.copy(color = secondaryTextColor, letterSpacing = 2.sp, fontSize = 9.sp),
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 10.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(spaces) { space ->
                                val isSelected = space == selectedSpace
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) primaryTextColor else (if (useSpotlight) Color(0xFFF3F4F6) else Stone900))
                                        .border(1.dp, if (isSelected) Color.Transparent else (if (useSpotlight) VoidBlack.copy(0.1f) else Color.White.copy(alpha = 0.1f)), RoundedCornerShape(20.dp))
                                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selectedSpace = space }
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = space.uppercase(),
                                        style = VoidTypography.labelSmall.copy(color = if (isSelected) screenBackgroundColor else secondaryTextColor, letterSpacing = 1.sp, fontSize = 11.sp)
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.matchParentSize()) {
                            Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(screenBackgroundColor, Color.Transparent))))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, screenBackgroundColor))))
                        }
                    }
                }

                HorizontalDivider(color = if (useSpotlight) VoidBlack.copy(0.08f) else Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 24.dp))

                // --- TEXT AREA ---
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(horizontal = 24.dp, vertical = 20.dp)) {
                    if (text.isEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Write what's on your mind...",
                                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, color = secondaryTextColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                            Text(
                                text = "Choose your frequency.\nWrite your feelings.\nFind your resonance.",
                                style = VoidTypography.bodyLarge.copy(fontSize = 12.sp, color = secondaryTextColor.copy(alpha = 0.4f), lineHeight = 18.sp, letterSpacing = 1.sp)
                            )
                        }
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= 500) text = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, color = primaryTextColor, lineHeight = 30.sp),
                        cursorBrush = SolidColor(primaryTextColor),
                        modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
                    )
                }
            }

            // ✅ FIX: Use the animated screenBackgroundColor to ensure the fade is buttery smooth and perfectly matches the main background
            val floatingGradient = Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    screenBackgroundColor.copy(alpha = 0.95f),
                    screenBackgroundColor
                )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(floatingGradient)
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = VoidTypography.labelSmall.copy(letterSpacing = 2.sp, fontSize = 11.sp), color = secondaryTextColor)
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = canSpotlight) {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .then(if (useSpotlight) Modifier.background(animatedGradient) else Modifier.background(if (isContentValid) Color.White.copy(0.1f) else Color.White.copy(0.05f)))
                                .then(if (useSpotlight) Modifier.background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent, Color.Black.copy(alpha = 0.2f)))) else Modifier)
                                .border(1.dp, if (useSpotlight) Color.White.copy(0.4f) else Color.White.copy(0.2f), RoundedCornerShape(16.dp))
                                .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); useSpotlight = !useSpotlight }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Spotlight", tint = if (useSpotlight) Color.White else Color.White.copy(0.5f), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (useSpotlight) "SPOTLIGHT ON" else "SPOTLIGHT OFF",
                                    style = VoidTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp).merge(if (useSpotlight) TextStyle(color = Color.White) else TextStyle(brush = animatedGradient))
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(buttonShadow, CircleShape, spotColor = if (useSpotlight) VoidBlack.copy(0.3f) else Color.White.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(submitButtonBgColor)
                        .border(1.dp, if (useSpotlight) VoidBlack else Color.White.copy(alpha = (0.1f + (0.9f * smoothProgress)).coerceIn(0f, 1f)), CircleShape)
                        .clickable { if (isReadyToPost) handleSend() else triggerDeniedAnimation() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(submitButtonProgressColor))

                    if (isEditing) {
                        Icon(Icons.Default.Cached, contentDescription = "Recast", tint = if (isReadyToPost) VoidBlack else Stone600, modifier = Modifier.size(32.dp).rotate(rotation.value))
                    } else {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Release", tint = iconTint, modifier = Modifier.size(32.dp).offset(y = iconSlideOffset.value.dp).rotate(arrowRotation))
                    }
                }
            }
        }
    }
}