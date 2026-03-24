package com.example.echo.presentation.components

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.UserStatsDto
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.horizontalScroll

@Composable
fun JourneyOverlay(
    stats: UserStatsDto?,
    allEchoes: List<EchoDto> = emptyList(), // ✅ CHANGED: Must pass ALL echoes now to find resonance on stranger's posts
    dominantMood: Int?,
    activityStreak: Int,
    userName: String?,
    userSignatureId: String,
    onUpdateName: (String) -> Unit,
    onRestoreIdentity: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onApplyEchoPlus: (String) -> Unit
) {
    val moodColor = when (dominantMood) {
        0 -> Color(0xFFFF453A)
        1 -> Color(0xFFFF9F0A)
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFF64D2FF)
        4 -> Color(0xFF34C759)
        else -> Color.White.copy(alpha = 0.4f)
    }

    val moodName = when (dominantMood) {
        0 -> "DISTURBED"
        1 -> "HEAVY"
        2 -> "NEUTRAL"
        3 -> "CALM"
        4 -> "RADIANT"
        else -> "OBSERVER"
    }

    // --- TRUE INFINITE ENGINE ---
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

    val nucleusPulse = 0.85f + 0.15f * ((sin(timeSec * (2f * PI.toFloat() / 3f)) + 1f) / 2f)

    var email by remember { mutableStateOf("") }
    var isApplying by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Identity State
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName ?: "") }
    var showImportSlot by remember { mutableStateOf(false) }
    var importKeyInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Help State
    var activeHelpSection by remember { mutableStateOf<String?>(null) }

    // Auto Keyboard Focus Requesters
    val nameFocusRequester = remember { FocusRequester() }
    val importFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }

    LaunchedEffect(userName) { tempName = userName ?: "" }
    LaunchedEffect(isEditingName) { if (isEditingName) { delay(100); nameFocusRequester.requestFocus(); keyboardController?.show() } }
    LaunchedEffect(showImportSlot) { if (showImportSlot) { delay(100); importFocusRequester.requestFocus(); keyboardController?.show() } }
    LaunchedEffect(isApplying) { if (isApplying) { delay(100); emailFocusRequester.requestFocus(); keyboardController?.show() } }

    // Unified Sync Animation Logic
    var isVerifying by remember { mutableStateOf(false) }
    var showFullFadeSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) { if (errorMessage != null) { delay(3000); errorMessage = null } }

    // Masking
    val maskedSignatureId = remember(userSignatureId) {
        if (userSignatureId.length > 12) {
            userSignatureId.take(8) + "-****-****-****-" + userSignatureId.takeLast(4)
        } else "****************"
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // --- LOCKED MAIN CONSOLE LAYER ---
        AnimatedVisibility(
            visible = !showFullFadeSuccess,
            enter = fadeIn(), exit = fadeOut(tween(800))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(VoidBlack)
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))))

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HEADER
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SOUL CONSOLE",
                                style = VoidTypography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 8.sp,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color(0xFF94A3B8).copy(alpha = 0.4f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // JOURNEY & ATOM
                    Text("YOUR JOURNEY", style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 6.sp, fontSize = 10.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("A reflection of your emotions over time.", style = VoidTypography.bodyLarge.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center))
                    Spacer(modifier = Modifier.height(40.dp))

                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f; val centerY = size.height / 2f; val centerOffset = Offset(centerX, centerY)
                            drawCircle(color = moodColor.copy(alpha = 0.02f), radius = size.width / 2f)
                            val systemRotationDeg = (timeSec * 36f) % 360f
                            rotate(systemRotationDeg) {
                                val shellWidth = size.width * 0.4f; val shellHeight = size.height * 0.9f
                                val shellLeft = (size.width - shellWidth) / 2f; val shellTop = (size.height - shellHeight) / 2f
                                val a = shellWidth / 2f; val b = shellHeight / 2f; val dashPhase = timeSec * 100f
                                fun drawPremiumPlane(angle: Float, speed: Float, color: Color, electronCount: Int) {
                                    rotate(angle) {
                                        drawOval(color = color.copy(alpha = 0.1f), topLeft = Offset(shellLeft, shellTop), size = Size(shellWidth, shellHeight), style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(40f, 60f), dashPhase)))
                                        for (i in 0 until electronCount) {
                                            val t = timeSec * 2f * PI.toFloat() * speed + ((2.0 * PI / electronCount) * i).toFloat()
                                            val eX = centerX + a * cos(t); val eY = centerY + b * sin(t)
                                            val depthScale = 0.5f + (0.5f * (sin(t) + 1f) / 2f); val depthAlpha = 0.2f + (0.8f * (sin(t) + 1f) / 2f)
                                            drawCircle(color = color.copy(alpha = 0.2f * depthAlpha), radius = 8.dp.toPx() * depthScale, center = Offset(eX, eY))
                                            drawCircle(color = color.copy(alpha = depthAlpha), radius = 3.5.dp.toPx() * depthScale, center = Offset(eX, eY))
                                            drawCircle(color = Color.White.copy(alpha = depthAlpha), radius = 1.2.dp.toPx() * depthScale, center = Offset(eX, eY))
                                        }
                                    }
                                }
                                drawPremiumPlane(0f, 1.2f, moodColor, 3)
                                drawPremiumPlane(60f, -0.8f, Color.White, 2)
                                drawPremiumPlane(120f, 1.5f, moodColor, 2)
                                drawPremiumPlane(-60f, -1.1f, Color.White, 3)
                            }
                            drawCircle(brush = Brush.radialGradient(colors = listOf(Color.White, moodColor.copy(alpha = 0.8f), Color.Transparent), center = centerOffset, radius = 24.dp.toPx() * nucleusPulse), radius = 24.dp.toPx() * nucleusPulse, center = centerOffset)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Text("DOMINANT FREQUENCY", style = VoidTypography.labelSmall.copy(color = Color.White.copy(alpha = 0.4f), letterSpacing = 3.sp, fontSize = 10.sp))
                    Text(moodName, style = VoidTypography.displayLarge.copy(fontSize = 24.sp, letterSpacing = 8.sp, color = moodColor, fontWeight = FontWeight.ExtraLight))
                    Spacer(modifier = Modifier.height(56.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TelemetryStat("ECHOES POSTED", stats?.totalEchoesPosted?.toString() ?: "0"); TelemetryStat("ACTIVITY STREAK", "$activityStreak days") }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TelemetryStat("RESONANCES SENT", stats?.totalResonancesSent?.toString() ?: "0"); TelemetryStat("WHISPERS RECEIVED", stats?.totalResonancesReceived?.toString() ?: "0") }

                    Spacer(modifier = Modifier.height(64.dp))

                    // ==========================================
                    // --- FUNCTIONAL DATA GRAPHS ---
                    // ==========================================

                    ConsoleSectionHeader("MOOD DISTRIBUTION (7 DAYS):", { activeHelpSection = if(activeHelpSection=="mood") null else "mood" }, if(activeHelpSection=="mood") "Analysis of your projected emotions and resonances over the last week." else null)
                    FunctionalMoodChart(allEchoes, userSignatureId, scrollState)

                    Spacer(modifier = Modifier.height(48.dp))

                    ConsoleSectionHeader("SIGNAL CALENDAR:", { activeHelpSection = if(activeHelpSection=="cal") null else "cal" }, if(activeHelpSection=="cal") "Your activity heatmap for the last 91 days combining posts and resonances." else null)
                    SignalHeatmap(allEchoes, userSignatureId)
                    ActivityLegend()

                    Spacer(modifier = Modifier.height(48.dp))

                    ConsoleSectionHeader("WAVELENGTH ACTIVITY:", { activeHelpSection = if(activeHelpSection=="wave") null else "wave" }, if(activeHelpSection=="wave") "Temporal analysis of signal peaks throughout the 24-hour cycle." else null)
                    FunctionalWavelength(allEchoes, userSignatureId)
                    ActivityLegend()

                    Spacer(modifier = Modifier.height(64.dp))

                    // ==========================================
                    // --- IDENTITY MODULE ---
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(0.02f), RoundedCornerShape(20.dp))
                            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ConsoleSectionHeader("SOUL ALIAS:", { activeHelpSection = if(activeHelpSection=="alias") null else "alias" }, if(activeHelpSection=="alias") "Your spectral name. Visible only to you." else null)

                        if (!isEditingName) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { isEditingName = true }
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (userName.isNullOrBlank()) "SET YOUR NAME" else userName.uppercase(),
                                    style = VoidTypography.bodyLarge.copy(color = VoidBlack, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                )
                            }
                        } else {
                            TextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.8f).focusRequester(nameFocusRequester),
                                textStyle = TextStyle(textAlign = TextAlign.Center, color = VoidBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = VoidBlack, cursorColor = VoidBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    IconButton(onClick = { onUpdateName(tempName); isEditingName = false; keyboardController?.hide() }) {
                                        Icon(Icons.Default.Check, "Save", tint = VoidBlack)
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        ConsoleSectionHeader("ECHO SOUL KEY:", { activeHelpSection = if(activeHelpSection=="key") null else "key" }, if(activeHelpSection=="key") "Your master sequence to restore identity and journey statistics across devices." else null, isRed = false)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.White.copy(0.05f), RoundedCornerShape(8.dp))
                                .clickable { clipboardManager.setText(AnnotatedString(userSignatureId)) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = maskedSignatureId, style = VoidTypography.bodySmall.copy(color = Color(0xFFFF453A), fontSize = 13.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold), maxLines = 1, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White.copy(0.5f), modifier = Modifier.size(14.dp))
                        }

                        Spacer(Modifier.height(16.dp))

                        TextButton(onClick = { showImportSlot = !showImportSlot }) {
                            Text("IMPORT EXISTING SOUL", style = VoidTypography.labelSmall.copy(color = Color.White.copy(0.6f), fontSize = 9.sp, letterSpacing = 1.sp))
                        }

                        AnimatedVisibility(visible = showImportSlot) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                                TextField(
                                    value = importKeyInput,
                                    onValueChange = { importKeyInput = it },
                                    placeholder = { Text("Paste Echo Soul Key...", color = Color.Gray, fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).focusRequester(importFocusRequester),
                                    enabled = !isVerifying,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.02f),
                                        focusedIndicatorColor = NeonWhite, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    trailingIcon = {
                                        if (importKeyInput.isNotEmpty() && !isVerifying) {
                                            IconButton(onClick = { importKeyInput = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White.copy(0.5f), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )
                                Button(
                                    onClick = {
                                        if (importKeyInput.isNotBlank()) {
                                            if (importKeyInput.trim() == userSignatureId) {
                                                errorMessage = "Soul already connected to this device."
                                            } else {
                                                isVerifying = true
                                                keyboardController?.hide()
                                            }
                                        }
                                    },
                                    enabled = !isVerifying && importKeyInput.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonWhite),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VoidBlack, strokeWidth = 2.dp)
                                    else Text("SYNC IDENTITY", style = VoidTypography.labelSmall.copy(color = VoidBlack, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    // ==========================================
                    // --- ECHO+ SECTION ---
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(moodColor.copy(alpha = 0.05f))
                            .border(0.5.dp, moodColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp).padding(top = 40.dp)) {
                            listOf(Color(0xFFFF453A), Color(0xFFFF9F0A), Color(0xFF64D2FF), Color(0xFF34C759)).forEachIndexed { i, col ->
                                drawPath(Path().apply { moveTo(0f, size.height * (0.8f - i * 0.1f)); cubicTo(size.width * 0.25f, size.height * (0.2f + i * 0.15f), size.width * 0.5f, size.height * (0.9f - i * 0.1f), size.width * 0.75f, size.height * (0.1f + i * 0.1f)); lineTo(size.width, size.height * 0.6f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, brush = Brush.verticalGradient(listOf(col.copy(alpha = 0.4f), Color.Transparent)))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp).fillMaxSize()
                        ) {
                            if (!showSuccess) {
                                Icon(Icons.Default.AutoAwesome, null, tint = moodColor, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Checkout ECHO+", style = VoidTypography.labelSmall.copy(color = NeonWhite, fontWeight = FontWeight.Bold, letterSpacing = 3.sp))
                                Spacer(Modifier.height(8.dp))
                                Text("Unlock emotion patterns, special Echo colors, priority Echoes, and full history tracking.", style = VoidTypography.bodyLarge.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp), modifier = Modifier.padding(horizontal = 10.dp))
                                Spacer(Modifier.height(24.dp))
                                if (!isApplying) {
                                    Button(onClick = { isApplying = true }, colors = ButtonDefaults.buttonColors(containerColor = NeonWhite), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                        Text("JOIN WAITLIST", style = VoidTypography.labelSmall.copy(color = VoidBlack, fontWeight = FontWeight.Bold))
                                    }
                                } else {
                                    TextField(value = email, onValueChange = { email = it }, placeholder = { Text("Enter your Email ID", color = Color.Gray, fontSize = 12.sp) }, modifier = Modifier.fillMaxWidth().height(54.dp).focusRequester(emailFocusRequester), colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(0.08f), unfocusedContainerColor = Color.White.copy(0.04f), focusedIndicatorColor = moodColor, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White), shape = RoundedCornerShape(12.dp), trailingIcon = { IconButton(onClick = { if(email.contains("@")) { onApplyEchoPlus(email); showSuccess = true } }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = NeonWhite) } })
                                }
                            } else {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF34C759), modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("SIGNAL RECEIVED", style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 4.sp, fontWeight = FontWeight.Bold))
                                Spacer(Modifier.height(8.dp))
                                Text("You've been added to the ECHO+ waitlist.", style = VoidTypography.bodyLarge.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ==========================================
        // --- TOP ALIGN SYSTEM ERROR BANNER ---
        // ==========================================
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp).zIndex(200f)
        ) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1A1A1C)).border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(text = errorMessage ?: "", style = VoidTypography.bodySmall.copy(color = Color.White, fontSize = 12.sp))
            }
        }

        // ==========================================
        // --- CINEMATIC SUCCESS FADE (Unblurred) ---
        // ==========================================
        LaunchedEffect(isVerifying) {
            if (isVerifying) {
                val success = onRestoreIdentity(importKeyInput)
                isVerifying = false
                if (success) showFullFadeSuccess = true
                else errorMessage = "Soul signature not found in the Void."
            }
        }

        AnimatedVisibility(
            visible = showFullFadeSuccess,
            enter = fadeIn(tween(800)),
            exit = fadeOut(tween(600)),
            modifier = Modifier.fillMaxSize().zIndex(500f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.98f)),
                contentAlignment = Alignment.Center
            ) {
                val checkmarkPercent = remember { Animatable(0f) }
                LaunchedEffect(Unit) { checkmarkPercent.animateTo(1f, tween(800, easing = LinearOutSlowInEasing)) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0xFF34C759).copy(0.2f), style = Stroke(width = 1.dp.toPx()))
                            val path = Path().apply {
                                moveTo(size.width * 0.25f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.7f)
                                lineTo(size.width * 0.75f, size.height * 0.35f)
                            }
                            drawPath(path, color = Color(0xFF34C759), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round), alpha = checkmarkPercent.value)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text("LINK ESTABLISHED", style = VoidTypography.labelSmall.copy(color = Color(0xFF34C759), letterSpacing = 6.sp, fontSize = 10.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("Identity synced.", style = VoidTypography.bodyLarge.copy(color = Color.White.copy(0.4f), fontSize = 12.sp))
                }
            }
            LaunchedEffect(Unit) {
                delay(3000)
                showFullFadeSuccess = false
                showImportSlot = false
                importKeyInput = ""
                onDismiss()
            }
        }
    }
}

@Composable
fun ConsoleSectionHeader(title: String, onHelp: () -> Unit, helpContent: String?, isRed: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = VoidTypography.labelSmall.copy(color = if (isRed) Color(0xFFFF453A) else NeonWhite, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp))
            Spacer(Modifier.width(10.dp))
            Icon(Icons.Outlined.HelpOutline, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp).clickable { onHelp() })
        }
        AnimatedVisibility(visible = helpContent != null) {
            Text(text = helpContent ?: "", style = VoidTypography.bodySmall.copy(color = Color.White.copy(0.5f), fontSize = 11.sp, lineHeight = 16.sp), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

// ✅ DYNAMIC LEGEND FOR GREEN ACTIVITY CODES
@Composable
fun ActivityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(Color(0xFF4ADE80).copy(alpha = 0.6f), "Echoed")
        Spacer(Modifier.width(16.dp))
        LegendItem(Color(0xFF4ADE80).copy(alpha = 0.25f), "Resonated")
        Spacer(Modifier.width(16.dp))
        LegendItem(Color(0xFF00FF66), "Both")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = VoidTypography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(0.5f)))
    }
}

// ✅ MASTER UPGRADE: True Gaussian Blur Glow (Single Seamless Line)
@Composable
fun FunctionalMoodChart(
    allEchoes: List<EchoDto>,
    userSignatureId: String,
    scrollState: ScrollState // Left here so you don't have to change JourneyOverlay.kt
) {
    val counts = remember(allEchoes, userSignatureId) {
        val bins = IntArray(5) { 0 }
        val oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS)

        allEchoes.forEach { echo ->
            val mood = echo.mood ?: return@forEach
            if (echo.signatureId == userSignatureId) {
                try { if (Instant.parse(echo.createdAt).isAfter(oneWeekAgo)) bins[mood]++ } catch(e: Exception) {}
            }
            echo.replies?.forEach { reply ->
                try {
                    val replyClass = reply!!::class.java
                    val sigId = (replyClass.methods.find { it.name.equals("getSignatureId", ignoreCase = true) }?.invoke(reply) as? String) ?: (replyClass.methods.find { it.name.equals("getUserId", ignoreCase = true) }?.invoke(reply) as? String)
                    val createdAt = replyClass.methods.find { it.name.equals("getCreatedAt", ignoreCase = true) }?.invoke(reply) as? String
                    if (sigId == userSignatureId && createdAt != null) {
                        if (Instant.parse(createdAt).isAfter(oneWeekAgo)) bins[mood]++
                    }
                } catch(e: Exception) {}
            }
        }
        bins
    }

    val totalCount = counts.sum().coerceAtLeast(1)
    val baseColors = listOf(
        Color(0xFFFF3B30), Color(0xFFFF9500),
        Color(0xFFB0BEC5), // Metallic Bluish Silver
        Color(0xFF5AC8FA), Color(0xFF4CD964)
    )
    val baseLabels = listOf("DISTURBED", "HEAVY", "NEUTRAL", "CALM", "RADIANT")

    val legendOrder = remember(counts) {
        (0..4).map { i -> Triple(baseLabels[i], baseColors[i], counts[i]) }
            .sortedByDescending { it.third }
    }

    val ringOrder = remember(counts) {
        val others = (1..4).map { i -> Triple(baseLabels[i], baseColors[i], counts[i]) }
            .sortedByDescending { it.third }
        others + Triple(baseLabels[0], baseColors[0], counts[0])
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2400,
                easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(12.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 2.dp.toPx()
                val activeWidth = 4.dp.toPx()
                val gap = 16.dp.toPx()

                val safePadding = 16.dp.toPx()
                val outerRadius = (size.minDimension / 2f) - safePadding

                ringOrder.forEachIndexed { i, stat ->
                    val radius = outerRadius - (i * gap)
                    val percentage = if (counts.sum() > 0) stat.third.toFloat() / totalCount else 0f
                    val currentSweep = (percentage * 360f) * animationProgress.value

                    // 1. Faded Background Track (Orbit Path)
                    drawCircle(
                        color = stat.second.copy(alpha = 0.25f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = trackWidth)
                    )

                    if (currentSweep > 0f) {
                        val rect = Rect(
                            Offset(center.x - radius, center.y - radius),
                            Size(radius * 2, radius * 2)
                        )

                        // 2. TRUE GAUSSIAN BLUR GLOW FOR THE ENTIRE LINE
                        drawIntoCanvas { canvas ->
                            val glowPaint = Paint().apply {
                                color = stat.second.copy(alpha = 0.6f)
                                style = PaintingStyle.Stroke
                                strokeWidth = 10.dp.toPx() // Wider than core for dispersion
                                strokeCap = StrokeCap.Round
                            }
                            // This applies a native, mathematically perfect blur to the path
                            glowPaint.asFrameworkPaint().maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)

                            canvas.drawArc(rect, -90f, currentSweep, false, glowPaint)
                        }

                        // 3. Solid Core Line (Drawn sharply over the blur)
                        drawArc(
                            color = stat.second,
                            startAngle = -90f,
                            sweepAngle = currentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = activeWidth, cap = StrokeCap.Round)
                        )

                        // 4. Trigonometry to find arc tip for the hot particle head
                        val angleInRadians = Math.toRadians((-90f + currentSweep).toDouble())
                        val dotX = center.x + radius * cos(angleInRadians).toFloat()
                        val dotY = center.y + radius * sin(angleInRadians).toFloat()

                        val moodColor = stat.second
                        val glowRadius = 12.dp.toPx() // Hotter/Brighter field at the tip

                        // High-intensity radial burst at the head
                        val glowBrush = Brush.radialGradient(
                            colors = listOf(
                                moodColor,
                                moodColor.copy(0.5f),
                                Color.Transparent
                            ),
                            center = Offset(dotX, dotY),
                            radius = glowRadius
                        )

                        // Head Diffused Glow
                        drawCircle(brush = glowBrush, radius = glowRadius, center = Offset(dotX, dotY))
                        // Head Physical Core
                        drawCircle(color = moodColor, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
                        // Piercing White Center
                        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // --- LEGEND (Order: Highest -> Lowest) ---
        Column(modifier = Modifier.fillMaxWidth()) {
            legendOrder.forEach { stat ->
                val percentage = if (counts.sum() > 0) ((stat.third.toFloat() / totalCount) * 100).roundToInt() else 0
                val isZero = percentage == 0

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isZero) stat.second.copy(alpha = 0.3f) else stat.second))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = stat.first,
                            style = VoidTypography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (isZero) Color.White.copy(0.3f) else Color.White.copy(0.8f),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Text(
                        text = "$percentage%",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 14.sp,
                            color = if (isZero) Color.White.copy(0.3f) else NeonWhite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

// ✅ UPGRADED HEATMAP: Premium Interactive Monthly Calendar
@Composable
fun SignalHeatmap(allEchoes: List<EchoDto>, userSignatureId: String) {
    val activeDays = remember(allEchoes, userSignatureId) {
        val map = mutableMapOf<LocalDate, Pair<Boolean, Boolean>>() // Pair(hasPost, hasRes)
        allEchoes.forEach { echo ->
            if (echo.signatureId == userSignatureId) {
                try {
                    val date = Instant.parse(echo.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                    val current = map[date] ?: Pair(false, false)
                    map[date] = current.copy(first = true)
                } catch(e: Exception) {}
            }

            echo.replies?.forEach { reply ->
                try {
                    val replyClass = reply!!::class.java
                    val sigId = (replyClass.methods.find { it.name.equals("getSignatureId", ignoreCase = true) }?.invoke(reply) as? String)
                        ?: (replyClass.methods.find { it.name.equals("getUserId", ignoreCase = true) }?.invoke(reply) as? String)
                    val createdAt = replyClass.methods.find { it.name.equals("getCreatedAt", ignoreCase = true) }?.invoke(reply) as? String

                    if (sigId == userSignatureId && createdAt != null) {
                        val date = Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                        val current = map[date] ?: Pair(false, false)
                        map[date] = current.copy(second = true)
                    }
                } catch(e: Exception) {}
            }
        }
        map
    }

    val today = LocalDate.now()
    var currentMonthStart by remember { mutableStateOf(today.withDayOfMonth(1)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.02f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // --- CALENDAR HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { currentMonthStart = currentMonthStart.minusMonths(1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous Month", tint = Stone400)
            }

            Text(
                text = "${currentMonthStart.month.name} ${currentMonthStart.year}".uppercase(),
                style = VoidTypography.labelSmall.copy(
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    color = NeonWhite,
                    fontWeight = FontWeight.Bold
                )
            )

            IconButton(
                onClick = { currentMonthStart = currentMonthStart.plusMonths(1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next Month", tint = Stone400)
            }
        }

        // --- DAYS OF WEEK ROW ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = VoidTypography.labelSmall.copy(color = Stone400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- CALENDAR GRID (Crossfades when changing months) ---
        AnimatedContent(
            targetState = currentMonthStart,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "Calendar Transition"
        ) { monthDate ->

            val daysInMonth = monthDate.lengthOfMonth()
            // 7 = Sunday. We want Sunday to be index 0.
            val startDayOfWeek = if (monthDate.dayOfWeek.value == 7) 0 else monthDate.dayOfWeek.value

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                var dayCounter = 1
                for (row in 0..5) {
                    if (dayCounter > daysInMonth) break

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0..6) {
                            if (row == 0 && col < startDayOfWeek || dayCounter > daysInMonth) {
                                // Empty Placeholder Box
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val currentDate = monthDate.withDayOfMonth(dayCounter)
                                val activity = activeDays[currentDate] ?: Pair(false, false)
                                val isToday = currentDate == today

                                // Logic for Box Color
                                val boxColor = when {
                                    activity.first && activity.second -> Color(0xFF00FF66)
                                    activity.first -> Color(0xFF4ADE80).copy(alpha = 0.6f)
                                    activity.second -> Color(0xFF4ADE80).copy(alpha = 0.25f)
                                    else -> Color.White.copy(alpha = 0.03f)
                                }

                                // Logic to ensure numbers are readable against the background
                                val textColor = when {
                                    activity.first && activity.second -> VoidBlack
                                    activity.first -> VoidBlack
                                    activity.second -> NeonWhite
                                    else -> Stone400
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(boxColor)
                                        .border(
                                            width = if (isToday) 1.dp else 0.dp,
                                            color = if (isToday) NeonWhite.copy(alpha = 0.5f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayCounter.toString(),
                                        style = VoidTypography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (activity.first || activity.second) FontWeight.Bold else FontWeight.Normal,
                                            color = textColor
                                        )
                                    )
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ UPGRADED WAVELENGTH: Dynamic Green Heights + Color Tracking
@Composable
fun FunctionalWavelength(allEchoes: List<EchoDto>, userSignatureId: String) {
    val bins = remember(allEchoes, userSignatureId) {
        val postBins = IntArray(24) { 0 }
        val resBins = IntArray(24) { 0 }

        allEchoes.forEach { echo ->
            if (echo.signatureId == userSignatureId) {
                try {
                    val hour = Instant.parse(echo.createdAt).atZone(ZoneId.systemDefault()).hour
                    postBins[hour]++
                } catch(e: Exception) {}
            }

            echo.replies?.forEach { reply ->
                try {
                    val replyClass = reply!!::class.java
                    val sigId = (replyClass.methods.find { it.name.equals("getSignatureId", ignoreCase = true) }?.invoke(reply) as? String)
                        ?: (replyClass.methods.find { it.name.equals("getUserId", ignoreCase = true) }?.invoke(reply) as? String)
                    val createdAt = replyClass.methods.find { it.name.equals("getCreatedAt", ignoreCase = true) }?.invoke(reply) as? String

                    if (sigId == userSignatureId && createdAt != null) {
                        val hour = Instant.parse(createdAt).atZone(ZoneId.systemDefault()).hour
                        resBins[hour]++
                    }
                } catch(e: Exception) {}
            }
        }
        Pair(postBins, resBins)
    }

    val postBins = bins.first
    val resBins = bins.second
    val totalMax = maxOf(postBins.maxOrNull() ?: 1, resBins.maxOrNull() ?: 1)

    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            for (i in 0..23) {
                val post = postBins[i]
                val res = resBins[i]
                val maxVal = maxOf(post, res)

                val h = (maxVal.toFloat() / totalMax).coerceAtLeast(0.02f)
                val color = if (post > 0 && res > 0) Color(0xFF166534) // Both (Dark)
                else if (post > 0) Color(0xFF4ADE80).copy(0.6f) // Post (Light)
                else if (res > 0) Color(0xFF4ADE80).copy(0.25f) // Resonated (Lightest)
                else Color.White.copy(0.03f) // None

                Box(modifier = Modifier.weight(1f).padding(horizontal = 1.dp).fillMaxHeight(h).background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val times = listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM")
            times.forEach { t -> Text(text = t, style = VoidTypography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(0.4f))) }
        }
    }
}

@Composable
fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Text(text = value, style = VoidTypography.displayLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraLight, color = NeonWhite))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = Color.White.copy(0.4f), letterSpacing = 1.sp))
    }
}