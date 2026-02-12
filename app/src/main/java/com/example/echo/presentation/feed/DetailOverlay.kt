package com.example.echo.presentation.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // ✅ Added
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback // ✅ Added
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.core.utils.TimeUtils
import com.example.echo.core.utils.SharedPreferencesManager
import kotlinx.coroutines.delay

@Composable
fun DetailOverlay(
    echo: EchoDto,
    onDismiss: () -> Unit,
    onReply: (String) -> Unit,
    onUpdateReply: (String, String) -> Unit,
    onEditEcho: () -> Unit,
    onReport: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current // ✅ Initialize haptic manager
    val mySignature = remember { SharedPreferencesManager.getInstallationId(context) }

    var replyText by remember { mutableStateOf("") }
    var editingReplyId by remember { mutableStateOf<String?>(null) }

    val currentReplies = echo.replies.orEmpty()
    val createdInstant = remember(echo.createdAt) { TimeUtils.parseTime(echo.createdAt) }
    var countdownText by remember { mutableStateOf(TimeUtils.getCountdownString(createdInstant)) }

    LaunchedEffect(Unit) {
        while (true) {
            countdownText = TimeUtils.getCountdownString(createdInstant)
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onDismiss)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(Color(0xFF0A0A0A), RoundedCornerShape(32.dp))
                .border(1.dp, Stone800.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                .clickable(enabled = false) {}
        ) {

            // --- TOP SECTION ---
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onReport) {
                            Icon(Icons.Outlined.Flag, "Report", tint = Stone400)
                        }

                        val isEchoOwner = echo.signatureId == mySignature
                        val isEchoEditable = remember(echo.createdAt) { TimeUtils.canEdit(echo.createdAt) }
                        if (isEchoOwner && isEchoEditable) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // ✅ Tick on Edit click
                                onEditEcho()
                            }) {
                                Icon(Icons.Default.Edit, "Recast Echo", tint = Stone400, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Stone400)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 20.dp)
                    ) {
                        Text(
                            text = echo.content,
                            style = VoidTypography.displayLarge.copy(fontSize = 24.sp, lineHeight = 32.sp),
                            color = NeonWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = countdownText,
                                style = VoidTypography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    color = Stone700.copy(alpha = 0.6f)
                                )
                            )
                            if (echo.updatedAt != null) {
                                Text(
                                    text = "(EDITED)",
                                    style = VoidTypography.labelSmall.copy(fontSize = 9.sp, color = Stone800, letterSpacing = 1.sp),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(color = Stone800.copy(alpha = 0.3f))

            // --- BOTTOM SECTION (Replies) ---
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .background(Color(0xFF080808))
            ) {
                Text(
                    text = "RESONANCES",
                    style = VoidTypography.labelSmall,
                    modifier = Modifier.padding(20.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentReplies.isEmpty()) {
                        item {
                            Text(
                                "The void is silent. Be the first to whisper back.",
                                style = VoidTypography.bodyLarge.copy(color = Stone700, fontSize = 12.sp)
                            )
                        }
                    } else {
                        itemsIndexed(currentReplies) { index, reply ->
                            val isMyReply = reply.signatureId == mySignature
                            val isEditable = TimeUtils.canEdit(reply.createdAt)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Stone900.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Stone800.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = reply.content,
                                        style = VoidTypography.bodyLarge.copy(fontSize = 14.sp, color = Stone300),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isMyReply && isEditable) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // ✅ Tick on Edit Resonance
                                                editingReplyId = reply.id
                                                replyText = reply.content
                                            },
                                            modifier = Modifier.size(20.dp).offset(x = 8.dp, y = (-4).dp)
                                        ) {
                                            Icon(Icons.Default.Edit, "Edit Resonance", tint = Stone700, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "RESONANCE #${index + 1}",
                                        style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = Stone700)
                                    )
                                    if (reply.updatedAt != null) {
                                        Text(
                                            text = " • EDITED",
                                            style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = Stone800)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // INPUT AREA
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Stone900.copy(alpha = 0.4f), CircleShape)
                        .border(1.dp, Stone800.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        textStyle = VoidTypography.bodyLarge.copy(color = NeonWhite, fontSize = 14.sp),
                        cursorBrush = SolidColor(NeonWhite),
                        singleLine = true,
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = if (editingReplyId != null) "Edit resonance..." else "Whisper back...",
                                    style = VoidTypography.bodyLarge.copy(color = Stone700, fontSize = 14.sp)
                                )
                            }
                            innerTextField()
                        }
                    )
                    IconButton(onClick = {
                        if (replyText.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // ✅ Satisfying thump on resonance send
                            if (editingReplyId != null) {
                                onUpdateReply(editingReplyId!!, replyText)
                                editingReplyId = null
                            } else {
                                onReply(replyText)
                            }
                            replyText = ""
                        }
                    }) {
                        Icon(Icons.Default.Send, "Send", tint = if (replyText.isNotEmpty()) NeonWhite else Stone700)
                    }
                }
            }
        }
    }
}