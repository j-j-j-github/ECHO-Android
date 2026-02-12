package com.example.echo.presentation.post

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.draw.clip

@Composable
fun WhisperOverlay(
    onDismiss: () -> Unit,
    onSend: (String, String?) -> Unit // Updated to accept optional color code
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // --- VALIDATION & ANIMATION STATE ---
    val isContentValid = text.trim().length >= 20
    val shakeOffset = remember { Animatable(0f) }

    // --- SECRET HASH MAPPING ---
    val secretMap = mapOf(
        "#Alert" to "#FF453A",   // Red
        "#Update" to "#4ADE80",  // Green
        "#Creator" to "#FFFFFF"  // White
    )

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Helper to process text and strip hashes before sending
    val handleSend = {
        val rawText = text.trim()
        var detectedColor: String? = null
        var cleanText = rawText

        // Identify and strip the secret hash
        secretMap.forEach { (tag, color) ->
            if (rawText.contains(tag, ignoreCase = true)) {
                detectedColor = color
                cleanText = rawText.replace(tag, "", ignoreCase = true).trim()
            }
        }

        onSend(cleanText, detectedColor)
    }

    // Function to trigger a "Void Shake" if text is too short
    val triggerDeniedAnimation = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    -10f at 50
                    10f at 100
                    -10f at 150
                    10f at 200
                    -5f at 250
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .offset(x = shakeOffset.value.dp)
                .height(400.dp)
                .clickable(enabled = false) {}
                .background(Color(0xFF080808), RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RELEASE YOUR ECHO",
                        style = VoidTypography.displayLarge.copy(
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = NeonWhite
                        )
                    )
                    Text(
                        if (isContentValid) "READY TO BROADCAST" else "MINIMUM 20 CHARACTERS REQUIRED",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (text.isNotEmpty() && !isContentValid) Color(0xFFFF453A) else Stone600
                        )
                    )
                }
                Text(
                    text = "${text.length} / 500",
                    style = VoidTypography.labelSmall.copy(
                        color = if (text.isNotEmpty() && !isContentValid) Color(0xFFFF453A) else Stone600,
                        fontSize = 10.sp
                    )
                )
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // --- TEXT AREA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        "Speak into the void...",
                        style = VoidTypography.bodyLarge.copy(color = Stone700)
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) text = it },
                    textStyle = VoidTypography.bodyLarge.copy(
                        color = NeonWhite,
                        fontSize = 20.sp
                    ),
                    cursorBrush = SolidColor(NeonWhite),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (isContentValid) handleSend() else triggerDeniedAnimation()
                    }),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester)
                )
            }

            // --- FOOTER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", style = VoidTypography.labelSmall, color = Stone600)
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isContentValid) NeonWhite else Stone900)
                        .clickable {
                            if (isContentValid) handleSend() else triggerDeniedAnimation()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "↗",
                        color = if (isContentValid) VoidBlack else Stone600,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}