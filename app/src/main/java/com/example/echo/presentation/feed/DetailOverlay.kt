package com.example.echo.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
    onReport: () -> Unit,
    onRelate: () -> Unit,
    onUnrelate: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val mySignature = remember { SharedPreferencesManager.getInstallationId(context) }

    var replyText by remember { mutableStateOf("") }
    var editingReplyId by remember { mutableStateOf<String?>(null) }

    // ✅ KEYBOARD DETECTION
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // ✅ RELATE STATE
    var isRelated by remember { mutableStateOf(false) }

    val currentReplies = echo.replies.orEmpty()
    val createdInstant = remember(echo.createdAt) { TimeUtils.parseTime(echo.createdAt) }
    var countdownText by remember { mutableStateOf(TimeUtils.getCountdownString(createdInstant)) }

    // --- SUPER USER LOGIC ---
    val upperColor = echo.colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isAlert = upperColor == "#FF453A"
    val isUpdate = upperColor == "#4ADE80"
    val isSuperUser = isCreator || isAlert || isUpdate

    val accentColor = when {
        isCreator -> Color(0xFFFFFFFF)
        isAlert -> Color(0xFFFF453A)
        isUpdate -> Color(0xFF4ADE80)
        else -> try { Color(android.graphics.Color.parseColor(echo.colorCode)) } catch (e: Exception) { Color.White }
    }

    val relateActiveColor = Color(0xFFFF453A) // Void Red
    val resonanceBgColor = Color(0xFF121212) // Deep premium grey

    // ✅ RELATE BUTTON COLOR ANIMATIONS
    val buttonBg by animateColorAsState(targetValue = if (isRelated) relateActiveColor else Color.Transparent, animationSpec = tween(200), label = "bg")
    val buttonText by animateColorAsState(targetValue = if (isRelated) Color.White else Stone400, animationSpec = tween(200), label = "text")
    val buttonBorder by animateColorAsState(
        targetValue = if (isRelated) Color.Transparent else Stone400.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "border"
    )

    // ✅ RELATE BUTTON ICON ANIMATIONS (Concentric Rings)
    val c1OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 4.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1x")
    val c2OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 12.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2x")
    val c1Radius by animateDpAsState(targetValue = if (isRelated) 6.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1r")
    val c2Radius by animateDpAsState(targetValue = if (isRelated) 2.5.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2r")

    LaunchedEffect(Unit) {
        while (true) {
            countdownText = TimeUtils.getCountdownString(createdInstant)
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = configuration.screenHeightDp.dp * 0.85f)
                .clip(RoundedCornerShape(32.dp))
                .background(VoidBlack)
                .border(1.dp, if(isSuperUser) accentColor.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {

            // =========================================================
            // ✅ OVERLAPPING BOX ARCHITECTURE
            // =========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // LAYER 1: THE SCROLLING TEXT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(100.dp))

                    Text(
                        text = echo.content,
                        style = VoidTypography.displayLarge.copy(
                            fontSize = 24.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center
                        ),
                        color = if (isSuperUser) accentColor else NeonWhite
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // ✅ RELATE BUTTON
                    if (!isSuperUser) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(buttonBg)
                                .border(1.dp, buttonBorder, RoundedCornerShape(24.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isRelated = !isRelated
                                    if (isRelated) onRelate() else onUnrelate()
                                }
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ ANIMATED CONCENTRIC RINGS
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val startX = c1OffsetX.toPx()
                                val endX = c2OffsetX.toPx()
                                val r1 = c1Radius.toPx()
                                val r2 = c2Radius.toPx()

                                drawCircle(color = buttonText, radius = r1, center = Offset(startX, size.height / 2), style = Stroke(1.5.dp.toPx()))
                                drawCircle(color = buttonText, radius = r2, center = Offset(endX, size.height / 2), style = Stroke(1.5.dp.toPx()))

                                val lineStart = startX + r1
                                val lineEnd = endX - r2
                                // The line naturally disappears as the circles intersect
                                if (lineEnd > lineStart) {
                                    drawLine(color = buttonText, start = Offset(lineStart, size.height / 2), end = Offset(lineEnd, size.height / 2), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isRelated) "RELATED" else "RELATE",
                                style = VoidTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                                color = buttonText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = countdownText, style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Stone400))

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // LAYER 2: THE HEADER & GRADIENT MASK
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to VoidBlack,
                                0.5f to VoidBlack,
                                0.8f to VoidBlack.copy(0.5f),
                                1.0f to Color.Transparent
                            )
                        )
                ) {
                    val gradientTopColor = if (isSuperUser) accentColor.copy(alpha = 0.12f) else Color.White.copy(0.06f)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(gradientTopColor, Color.Transparent)))
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .padding(top = 4.dp)
                    ) {
                        Row(Modifier.align(Alignment.CenterStart)) {
                            if (!isSuperUser) {
                                IconButton(onClick = onReport) { Icon(Icons.Outlined.Flag, null, tint = Stone400, modifier = Modifier.size(20.dp)) }
                            } else {
                                Text("BROADCAST", color = accentColor.copy(alpha = 0.8f), style = VoidTypography.labelSmall.copy(letterSpacing = 2.sp, fontSize = 9.sp), modifier = Modifier.padding(start = 12.dp))
                            }

                            if (echo.signatureId == mySignature && TimeUtils.canEdit(echo.createdAt) && !isSuperUser) {
                                IconButton(onClick = onEditEcho) { Icon(Icons.Default.Edit, null, tint = Stone400, modifier = Modifier.size(18.dp)) }
                            }
                        }

                        Text(
                            text = (echo.category ?: "THE VOID").uppercase(),
                            style = VoidTypography.labelSmall.copy(letterSpacing = 3.sp, fontSize = 10.sp, color = Stone400),
                            modifier = Modifier.align(Alignment.Center)
                        )

                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Icon(Icons.Default.Close, null, tint = Stone400, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // =========================================================
            // 3. INTEGRATED RESONANCE SECTION
            // =========================================================
            if (!isSuperUser) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(resonanceBgColor)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.05f))

                    AnimatedVisibility(
                        visible = !isKeyboardOpen,
                        enter = fadeIn(tween(400, delayMillis = 100)) + expandVertically(spring(dampingRatio = 0.85f, stiffness = 200f), expandFrom = Alignment.Top),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(300, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top)
                    ) {
                        Column {
                            Text(
                                text = if (currentReplies.isEmpty()) "THE VOID IS SILENT" else "RESONANCES",
                                style = VoidTypography.labelSmall.copy(color = Stone700, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp)
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = configuration.screenHeightDp.dp * 0.45f)
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (currentReplies.isEmpty()) {
                                    item { Text("Be the first to resonate...", style = VoidTypography.bodyLarge.copy(color = Stone800, fontSize = 12.sp)) }
                                } else {
                                    itemsIndexed(currentReplies) { index, reply ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black, RoundedCornerShape(14.dp))
                                                .border(1.dp, Color.White.copy(0.04f), RoundedCornerShape(14.dp))
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = "RESONANCE #${index + 1}",
                                                style = VoidTypography.labelSmall.copy(
                                                    color = Stone400,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                modifier = Modifier.padding(bottom = 8.dp)
                                            )
                                            Text(reply.content, style = VoidTypography.bodyLarge.copy(fontSize = 14.sp, color = Stone300))

                                            if (reply.signatureId == mySignature && TimeUtils.canEdit(reply.createdAt)) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "TAP TO EDIT",
                                                    modifier = Modifier.clickable {
                                                        editingReplyId = reply.id
                                                        replyText = reply.content
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    },
                                                    style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = Stone700, letterSpacing = 1.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(20.dp)) }
                            }
                        }
                    }

                    // ✅ PREMIUM ARROW SEND BUTTON
                    val isSendReady = replyText.isNotBlank()
                    val sendBgColor by animateColorAsState(targetValue = if (isSendReady) Color.White else Color.Transparent, animationSpec = tween(300), label = "sendBg")
                    val sendIconColor by animateColorAsState(targetValue = if (isSendReady) VoidBlack else Stone700, animationSpec = tween(300), label = "sendIcon")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Stone900.copy(0.5f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                        text = if (editingReplyId != null) "Editing..." else "Whisper back...",
                                        style = VoidTypography.bodyLarge.copy(color = Stone700, fontSize = 14.sp)
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(sendBgColor)
                                .clickable(enabled = isSendReady) {
                                    if (isSendReady) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (editingReplyId != null) {
                                            onUpdateReply(editingReplyId!!, replyText)
                                            editingReplyId = null
                                        } else {
                                            onReply(replyText)
                                        }
                                        replyText = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Send",
                                tint = sendIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}