package com.example.echo.presentation.components

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
import kotlin.math.*

@Composable
fun JourneyOverlay(
    stats: UserStatsDto?,
    userEchoes: List<EchoDto> = emptyList(), // Added actual user data for graphs
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
                    .fillMaxWidth(0.92f) // Locked width to prevent stretching
                    .fillMaxHeight(0.85f) // Locked height to avoid taskbar clipping
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
                                    color = Color(0xFF94A3B8), // Neutral Mood Color
                                    letterSpacing = 8.sp,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            // Much wider line for elegant separation
                            Box(
                                Modifier
                                    .fillMaxWidth(0.8f) // Expanded from 40.dp to 80% of the container width
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF94A3B8).copy(alpha = 0.4f), // Matches the title color
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(20.dp))
                        }
                    }

// Clean vertical spacing (removed the unnecessary width here)
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
                                // EXACTLY 4 ORBITS AS REQUESTED
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

                    ConsoleSectionHeader("MOOD DISTRIBUTION:", { activeHelpSection = if(activeHelpSection=="mood") null else "mood" }, if(activeHelpSection=="mood") "Analysis of your projected emotions. Heights correlate to signal density." else null)
                    FunctionalMoodChart(userEchoes)

                    Spacer(modifier = Modifier.height(48.dp))

                    ConsoleSectionHeader("SIGNAL CALENDAR:", { activeHelpSection = if(activeHelpSection=="cal") null else "cal" }, if(activeHelpSection=="cal") "Your activity heatmap for the last 91 days. Darker pulses indicate higher volume." else null)
                    SignalHeatmap(userEchoes)

                    Spacer(modifier = Modifier.height(48.dp))

                    ConsoleSectionHeader("WAVELENGTH ACTIVITY:", { activeHelpSection = if(activeHelpSection=="wave") null else "wave" }, if(activeHelpSection=="wave") "Temporal analysis of signal peaks throughout the 24-hour cycle." else null)
                    FunctionalWavelength(moodColor, userEchoes)

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
                    // --- ECHO+ SECTION (FIXED SUCCESS LAYOUT) ---
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
                    } // End of Echo+ Box
                    Spacer(modifier = Modifier.height(32.dp))
                } // End of Main Column
            } // End of Locked Console Box
        } // End of AnimatedVisibility

        // --- THE ERROR BANNER AND SUCCESS FADE NOW SIT BELOW THIS LINE ---


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

@Composable
fun FunctionalMoodChart(echoes: List<EchoDto>) {
    val counts = remember(echoes) {
        val bins = IntArray(5) { 0 }
        echoes.forEach { if (it.mood in 0..4) bins[it.mood!!]++ }
        bins
    }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val colors = listOf(Color(0xFFFF453A), Color(0xFFFF9F0A), Color(0xFF94A3B8), Color(0xFF64D2FF), Color(0xFF34C759))
    val labels = listOf("DIST", "HEAV", "NEUT", "CALM", "RADI")

    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            counts.forEachIndexed { i, count ->
                val h = (count.toFloat() / max).coerceAtLeast(0.05f)
                Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(colors[i].copy(0.4f)).border(0.5.dp, colors[i], RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { i, label ->
                Text(text = label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = VoidTypography.labelSmall.copy(fontSize = 9.sp, color = colors[i].copy(0.7f), letterSpacing = 1.sp))
            }
        }
    }
}

@Composable
fun SignalHeatmap(echoes: List<EchoDto>) {
    val activeDays = remember(echoes) {
        echoes.map { try { Instant.parse(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate() } catch(e: Exception) { LocalDate.now() } }.groupBy { it }
    }
    val today = LocalDate.now()
    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        repeat(7) { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(14) { col -> // 14 weeks wide
                    val date = today.minusDays(((13 - col) * 7 + (6 - row)).toLong())
                    val intensity = (activeDays[date]?.size ?: 0).coerceAtMost(4)
                    val color = when(intensity) { 0 -> Color.White.copy(0.03f) 1 -> NeonWhite.copy(0.2f) 2 -> NeonWhite.copy(0.4f) 3 -> NeonWhite.copy(0.7f) else -> NeonWhite }
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
fun FunctionalWavelength(color: Color, echoes: List<EchoDto>) {
    val bins = remember(echoes) {
        val h = IntArray(24) { 0 }
        echoes.forEach { try { h[Instant.parse(it.createdAt).atZone(ZoneId.systemDefault()).hour]++ } catch(e: Exception) {} }
        h
    }
    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.02f), RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val max = bins.maxOrNull()?.coerceAtLeast(1) ?: 1
            bins.forEach { count ->
                val h = (count.toFloat() / max).coerceAtLeast(0.02f)
                Box(modifier = Modifier.weight(1f).padding(horizontal = 1.dp).fillMaxHeight(h).background(color.copy(0.6f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
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