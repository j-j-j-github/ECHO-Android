package com.example.echo.presentation.post

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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

    var selectedSpace by remember { mutableStateOf(initialCategory) }
    val spaces = listOf("Advice", "Vent", "Relationships", "Inner Thoughts")

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isEditing = initialText.isNotEmpty()

    val isContentValid = text.trim().length >= 20
    val isSpaceValid = selectedSpace.isNotEmpty() || isEditing
    val isReadyToPost = isContentValid && isSpaceValid

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
        onSend(cleanText, selectedSpace.ifEmpty { "Random" }, detectedColor)
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

    val overlayBgColor = Color(0xFF080808)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .offset(x = shakeOffset.value.dp)
                .height(500.dp)
                .clickable(enabled = false) {}
                .background(overlayBgColor, RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .padding(top = 24.dp, bottom = 24.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // ✅ FIXED: Chained the padding correctly
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isEditing) "RECAST YOUR ECHO" else "RELEASE YOUR ECHO",
                        style = VoidTypography.displayLarge.copy(fontSize = 18.sp, letterSpacing = 2.sp, color = NeonWhite)
                    )
                    Text(
                        if (isReadyToPost) "READY TO BROADCAST" else if (!isContentValid) "MINIMUM 20 CHARACTERS REQUIRED" else "CHOOSE A SPACE TO POST",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (!isReadyToPost && text.isNotEmpty()) Color(0xFFFF453A) else Stone600
                        )
                    )
                }
                Text("${text.length} / 500", style = VoidTypography.labelSmall.copy(color = if (!isContentValid && text.isNotEmpty()) Color(0xFFFF453A) else Stone600, fontSize = 10.sp))
            }

            // --- SEPARATE SPACE SECTION ---
            if (!isEditing) {
                Text(
                    text = "CHOOSE A SPACE",
                    style = VoidTypography.labelSmall.copy(color = Stone600, letterSpacing = 1.sp, fontSize = 10.sp),
                    // ✅ FIXED: Chained the padding correctly
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(spaces) { space ->
                            val isSelected = space == selectedSpace
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) NeonWhite else Stone900)
                                    .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedSpace = space
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = space.uppercase(),
                                    style = VoidTypography.labelSmall.copy(
                                        color = if (isSelected) Color.Black else Stone400,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }
                    }

                    // ✅ Left & Right Gradient Fades
                    Row(modifier = Modifier.matchParentSize()) {
                        Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(overlayBgColor, Color.Transparent))))
                        Spacer(modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.fillMaxHeight().width(24.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, overlayBgColor))))
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 24.dp))

            // --- TEXT AREA ---
            Box(modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (text.isEmpty()) {
                    Text("Speak into the void...", style = VoidTypography.bodyLarge.copy(color = Stone700))
                }
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    textStyle = VoidTypography.bodyLarge.copy(color = NeonWhite, fontSize = 20.sp),
                    cursorBrush = SolidColor(NeonWhite),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (isReadyToPost) handleSend() else triggerDeniedAnimation()
                    }),
                    modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
                )
            }

            // --- FOOTER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) { Text("CANCEL", style = VoidTypography.labelSmall, color = Stone600) }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isReadyToPost) NeonWhite else Stone900)
                        .clickable { if (isReadyToPost) handleSend() else triggerDeniedAnimation() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditing) {
                        Icon(imageVector = Icons.Default.Cached, contentDescription = "Recast", tint = if (isReadyToPost) VoidBlack else Stone600, modifier = Modifier.size(28.dp).rotate(rotation.value))
                    } else {
                        Icon(imageVector = Icons.Default.NorthEast, contentDescription = "Release", tint = if (isReadyToPost) VoidBlack else Stone600, modifier = Modifier.size(28.dp).offset(y = iconSlideOffset.value.dp))
                    }
                }
            }
        }
    }
}