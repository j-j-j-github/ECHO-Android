package com.example.echo.presentation.post

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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import kotlinx.coroutines.delay

@Composable
fun WhisperOverlay(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
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
                .height(400.dp) // Fixed Height
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
                    // UPDATED: Uses displayLarge (Serif) like Home Screen
                    Text(
                        text = "RELEASE YOUR ECHO",
                        style = VoidTypography.displayLarge.copy(
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = NeonWhite
                        )
                    )
                    Text(
                        "BROADCASTING TO THE VOID",
                        style = VoidTypography.labelSmall.copy(fontSize = 8.sp)
                    )
                }
                Text(
                    text = "${text.length} / 500",
                    style = VoidTypography.labelSmall.copy(color = Stone600, fontSize = 10.sp)
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
                        "Type your message...",
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
                        if (text.isNotBlank()) onSend(text)
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
                    Text("CANCEL", style = VoidTypography.labelSmall)
                }

                Button(
                    onClick = { onSend(text) },
                    enabled = text.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonWhite,
                        disabledContainerColor = Stone800
                    ),
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "↗",
                        color = if (text.isNotEmpty()) VoidBlack else Stone400,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}