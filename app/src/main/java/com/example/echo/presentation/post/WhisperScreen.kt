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

@Composable
fun WhisperOverlay(
    onDismiss: () -> Unit,
    onSend: (String, String, String?) -> Unit,
    initialText: String = "",
    initialCategory: String = ""
) {
    var text by remember { mutableStateOf(initialText) }

    // ✅ AUTO-SELECT SPACE: Sets to empty if "All", otherwise uses the initialCategory
    val actualInitialSpace = if (initialCategory.equals("All", ignoreCase = true)) "" else initialCategory
    var selectedSpace by remember { mutableStateOf(actualInitialSpace) }

    val spaces = listOf("Advice", "Vent", "Relationships", "Inner Thoughts")

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isEditing = initialText.isNotEmpty()

    // ✅ SUPER USER DETECTION
    val textUpper = text.uppercase()
    val isSuperUserPost = textUpper.contains("#CREATOR") ||
            textUpper.contains("#ALERT") ||
            textUpper.contains("#UPDATE")

    // ✅ VALIDATION LOGIC
    val isContentValid = text.trim().length >= 20
    val isSpaceValid = selectedSpace.isNotEmpty() || isEditing || isSuperUserPost
    val isReadyToPost = isContentValid && isSpaceValid

    // ✅ SMOOTH FLUID GLOW CALCULATION
    // First, get the raw mathematical progress (0.0 to 1.0)
    val rawProgress = (text.trim().length / 20f).coerceIn(0f, 1f)

    // Then, animate it smoothly so the light swells and recedes organically instead of snapping
    val smoothProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "smoothProgress"
    )

    // ✅ PREMIUM ICON ROTATION & TINT
    // Rotates 90 degrees clockwise (to point right) when text reaches 20 chars
    val arrowRotation by animateFloatAsState(
        targetValue = if (isContentValid) 90f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "arrowRotation"
    )

    // Smoothly transitions the icon color from muted gray to deep black as the background brightens
    val iconTint by animateColorAsState(
        targetValue = if (isContentValid) VoidBlack else Stone400,
        animationSpec = tween(durationMillis = 500),
        label = "iconTint"
    )

    // Smoothly expands the drop shadow when fully ready
    val buttonShadow by animateDpAsState(
        targetValue = if (isReadyToPost) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 600),
        label = "buttonShadow"
    )

    val shakeOffset = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val iconSlideOffset = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isEditing) {
            rotation.animateTo(360f, tween(durationMillis = 600, easing = FastOutSlowInEasing))
        } else {
            iconSlideOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    // ✅ UPDATED SECRET MAP WITH NEW KEYWORDS
    val secretMap = mapOf("#Alert" to "#FF453A", "#Update" to "#4ADE80", "#Creator" to "#FFFFFF")

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
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

        // ✅ CATEGORY OVERRIDE: Default to "All" if Super User and no space selected
        val finalSpace = if (isSuperUserPost && selectedSpace.isEmpty()) "All" else selectedSpace.ifEmpty { "Random" }
        onSend(cleanText, finalSpace, detectedColor)
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

    // Outer Wrapper: Handles keyboard floating logic dynamically
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            .imePadding() // Float over keyboard
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {

        // --- PREMIUM GLASS CARD ---
        Box(
            modifier = Modifier
                // ✅ TASKBAR LOCK: Uses actual system WindowInsets to perfectly avoid the top status bar!
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, start = 16.dp, end = 16.dp)
                .offset(x = shakeOffset.value.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(VoidBlack)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .clickable(enabled = false) {}
        ) {
            // Elegant Top Glass Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.Transparent)))
            )

            // --- MAIN CONTENT COLUMN (Scrollable so it never breaks layout) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 28.dp, bottom = 100.dp) // Extra bottom padding for the floating button
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "RECAST YOUR ECHO" else "RELEASE YOUR ECHO",
                            style = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 3.sp, color = NeonWhite)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isReadyToPost) "READY TO BROADCAST" else if (!isContentValid) "MINIMUM 20 CHARACTERS REQUIRED" else "CHOOSE A SPACE TO POST",
                            style = VoidTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 2.sp,
                                color = if (!isReadyToPost && text.isNotEmpty()) Color(0xFFFF453A) else Color.White.copy(alpha = 0.5f)
                            )
                        )
                    }
                    Text(
                        text = "${text.length} / 500",
                        style = VoidTypography.labelSmall.copy(
                            color = if (!isContentValid && text.isNotEmpty()) Color(0xFFFF453A) else Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    )
                }

                // --- SEPARATE SPACE SECTION ---
                if (!isEditing) {
                    Text(
                        text = "CHOOSE A SPACE",
                        style = VoidTypography.labelSmall.copy(color = Color.White.copy(alpha = 0.3f), letterSpacing = 2.sp, fontSize = 9.sp),
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
                                        .background(if (isSelected) NeonWhite else Stone900)
                                        .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedSpace = space
                                        }
                                        .padding(horizontal = 18.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = space.uppercase(),
                                        style = VoidTypography.labelSmall.copy(
                                            color = if (isSelected) VoidBlack else Stone400,
                                            letterSpacing = 1.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.matchParentSize()) {
                            Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(VoidBlack, Color.Transparent))))
                            Spacer(modifier = Modifier.weight(1f))
                            Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, VoidBlack))))
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 24.dp))

                // --- TEXT AREA ---
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(horizontal = 24.dp, vertical = 20.dp)) {
                    if (text.isEmpty()) {
                        // ✅ NEW EXPANDED PLACEHOLDER
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Write what's on your mind...",
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 22.sp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                            Text(
                                text = "Choose your frequency.\n" +
                                        "Write your feelings.\n" +
                                        "Find your resonance.",
                                style = VoidTypography.bodyLarge.copy(
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    lineHeight = 18.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= 500) text = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            color = NeonWhite,
                            lineHeight = 30.sp
                        ),
                        cursorBrush = SolidColor(NeonWhite),
                        modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
                    )
                }
            }
        }

        // --- FLOATING ACTION LAYER ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    style = VoidTypography.labelSmall.copy(letterSpacing = 2.sp, fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            // --- PREMIUM DYNAMIC GLOW BUTTON ---
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(buttonShadow, CircleShape, spotColor = Color.White.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Stone800, Stone900)))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f + (0.9f * smoothProgress)), // Smoothly brightens border
                        shape = CircleShape
                    )
                    .clickable { if (isReadyToPost) handleSend() else triggerDeniedAnimation() },
                contentAlignment = Alignment.Center
            ) {
                // ✅ Smoothly expanding white core based on typing progress
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = smoothProgress))
                )

                if (isEditing) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Recast",
                        tint = if (isReadyToPost) VoidBlack else Stone600,
                        modifier = Modifier.size(32.dp).rotate(rotation.value)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Release",
                        tint = iconTint, // Smoothly darkens as background brightens
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = iconSlideOffset.value.dp)
                            .rotate(arrowRotation) // ✅ Premium 90-degree twist!
                    )
                }
            }
        }
    }
}