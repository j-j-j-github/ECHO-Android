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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import java.time.OffsetDateTime
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
import com.example.echo.data.remote.dto.MoodHistoryDto

@Composable
fun JourneyOverlay(
    stats: UserStatsDto?,
    allEchoes: List<EchoDto> = emptyList(),
    permanentHistory: List<MoodHistoryDto> = emptyList(),
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
    var showSuccess by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    var applyStep by remember { mutableStateOf(0) }

    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(userName ?: "") }
    var showImportSlot by remember { mutableStateOf(false) }
    var importKeyInput by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var activeHelpSection by remember { mutableStateOf<String?>(null) }

    val nameFocusRequester = remember { FocusRequester() }
    val importFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }

    LaunchedEffect(userName) { tempName = userName ?: "" }
    LaunchedEffect(isEditingName) { if (isEditingName) { delay(100); nameFocusRequester.requestFocus(); keyboardController?.show() } }
    LaunchedEffect(showImportSlot) { if (showImportSlot) { delay(100); importFocusRequester.requestFocus(); keyboardController?.show() } }
    LaunchedEffect(applyStep) { if (applyStep == 1) { delay(100); emailFocusRequester.requestFocus(); keyboardController?.show() } }

    var isVerifying by remember { mutableStateOf(false) }
    var showFullFadeSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) { if (errorMessage != null) { delay(3000); errorMessage = null } }

    val maskedSignatureId = remember(userSignatureId) {
        if (userSignatureId.length > 12) {
            userSignatureId.take(8) + "-****-****-****-" + userSignatureId.takeLast(4)
        } else "****************"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    val breathingBrightness by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val breathingWhite = Color.White.copy(alpha = breathingBrightness)

    val surfaceBrush = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
    )

    val brightRgbGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6), Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFF84CC16), Color(0xFFEAB308), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFFD946EF), Color(0xFF8B5CF6)),
        start = Offset(0f,0f), end = Offset(300f,300f)
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

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

                    Text("YOUR JOURNEY", style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 6.sp, fontSize = 10.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("A reflection of your interactions.", style = VoidTypography.bodyLarge.copy(fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center))
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryStat("ECHOES POSTED", stats?.totalEchoesPosted?.toString() ?: "0")
                        TelemetryStat("RELATES RECEIVED", stats?.totalRelatesReceived?.toString() ?: "0")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryStat("RESONANCES SENT", stats?.totalResonancesSent?.toString() ?: "0")
                        TelemetryStat("WHISPERS RECEIVED", stats?.totalResonancesReceived?.toString() ?: "0")
                    }

                    Spacer(modifier = Modifier.height(64.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(0.02f), RoundedCornerShape(20.dp))
                            .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ConsoleSectionHeader("SOUL NAME:", { activeHelpSection = if(activeHelpSection=="alias") null else "alias" }, if(activeHelpSection=="alias") "Your spectral name. Visible only to you." else null)

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

                    // ✅ FIXED: ECHO+ Section with stabilized Volumetric Glow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(surfaceBrush)
                            .border(1.dp, breathingWhite, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // ✅ FIXED: Stable Blur Implementation
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(0.6f * breathingBrightness)
                                .padding(1.dp)
                                .blur(20.dp)
                                .background(Color.White, RoundedCornerShape(24.dp))
                        )

                        AnimatedContent(
                            targetState = showSuccess,
                            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
                            modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp).fillMaxWidth().zIndex(10f),
                            label = "SuccessAnimation"
                        ) { isSuccess ->
                            if (!isSuccess) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .graphicsLayer { alpha = 0.99f }
                                            .drawWithCache {
                                                onDrawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = brightRgbGradient,
                                                        blendMode = BlendMode.SrcIn
                                                    )
                                                }
                                            }
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "THE VOID, AMPLIFIED.",
                                        style = VoidTypography.labelSmall.copy(color = NeonWhite, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Join ECHO+ for full access to advanced analytics and premium features.",
                                        style = VoidTypography.bodyLarge.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                                    )

                                    Spacer(Modifier.height(24.dp))

                                    Column(horizontalAlignment = Alignment.Start) {
                                        EchoPlusFeature(Icons.Default.Person, "Personal Reflections")
                                        Spacer(modifier = Modifier.height(10.dp))
                                        EchoPlusFeature(Icons.Default.List, "Frequency Insights")
                                        Spacer(modifier = Modifier.height(10.dp))
                                        EchoPlusFeature(Icons.Default.DateRange, "Activity Calendar")
                                        Spacer(modifier = Modifier.height(10.dp))
                                        EchoPlusFeature(Icons.Default.Star, "Peak Activity Times")
                                        Spacer(modifier = Modifier.height(10.dp))
                                        EchoPlusFeature(Icons.Default.Brush, "Echo Themes")
                                    }

                                    Spacer(Modifier.height(32.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .animateContentSize(tween(400, easing = FastOutSlowInEasing)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedContent(
                                            targetState = applyStep,
                                            transitionSpec = {
                                                (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(300)) + slideOutVertically { -it / 2 })
                                            },
                                            label = "applyAnimation"
                                        ) { step ->
                                            when (step) {
                                                0 -> {
                                                    Button(
                                                        onClick = { applyStep = 1 },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NeonWhite),
                                                        shape = RoundedCornerShape(16.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(54.dp)
                                                            .shadow(
                                                                elevation = 16.dp,
                                                                shape = RoundedCornerShape(16.dp),
                                                                spotColor = Color.White.copy(alpha = 0.4f),
                                                                ambientColor = Color.White.copy(alpha = 0.2f)
                                                            )
                                                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                                    ) {
                                                        Text("JOIN THE WAITLIST", style = VoidTypography.labelSmall.copy(color = VoidBlack, fontWeight = FontWeight.Black, letterSpacing = 1.sp))
                                                    }
                                                }
                                                1 -> {
                                                    TextField(
                                                        value = email,
                                                        onValueChange = { email = it },
                                                        placeholder = { Text("Enter your Email ID", color = Color.Gray, fontSize = 12.sp) },
                                                        modifier = Modifier.fillMaxWidth().height(54.dp).focusRequester(emailFocusRequester),
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.White.copy(0.08f), unfocusedContainerColor = Color.White.copy(0.04f),
                                                            focusedIndicatorColor = Color.White, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(16.dp),
                                                        trailingIcon = {
                                                            IconButton(onClick = {
                                                                if(email.contains("@")) {
                                                                    keyboardController?.hide()
                                                                    applyStep = 2
                                                                }
                                                            }) {
                                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = NeonWhite)
                                                            }
                                                        }
                                                    )
                                                }
                                                2 -> {
                                                    LaunchedEffect(Unit) {
                                                        onApplyEchoPlus(email)
                                                        delay(1500)
                                                        showSuccess = true
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(54.dp)
                                                            .background(Color.White.copy(0.05f), RoundedCornerShape(16.dp))
                                                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(color = NeonWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color(0xFF34C759), modifier = Modifier.size(44.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Text("SIGNAL RECEIVED", style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 4.sp, fontWeight = FontWeight.Bold))
                                    Spacer(Modifier.height(8.dp))
                                    Text("You are on the list.", style = VoidTypography.bodyLarge.copy(color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

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
fun TelemetryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Text(text = value, style = VoidTypography.displayLarge.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraLight, color = NeonWhite))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = Color.White.copy(0.4f), letterSpacing = 1.sp))
    }
}

@Composable
fun EchoPlusFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(Color.White.copy(0.05f), CircleShape)
                .border(0.5.dp, Color.White.copy(0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonWhite, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = VoidTypography.bodyLarge.copy(color = Color.White.copy(0.8f), fontSize = 13.sp)
        )
    }
}