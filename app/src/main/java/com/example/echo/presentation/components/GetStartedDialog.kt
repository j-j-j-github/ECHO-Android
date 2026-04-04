package com.example.echo.presentation.components

import androidx.compose.foundation.Canvas
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.echo.R
import com.example.echo.core.theme.VoidBlack
import com.example.echo.core.theme.VoidTypography
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// ─── Data Model ───────────────────────────────────────────────────────────────

data class OnboardingSlide(
    val title: String,
    val description: String
)

private val PremiumAppleFont = FontFamily.SansSerif

// ─── Main Composable ──────────────────────────────────────────────────────────

@Composable
fun GetStartedDialog(
    onEnterClick: (String) -> Unit,
    onSkip: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ✅ ADJUST THIS VALUE TO MOVE THE LOGO UP OR DOWN
    val logoOffsetY = 8.dp

    // ─── Slide Definitions ────────────────────────────────────────────────────
    val slides = listOf(
        OnboardingSlide(
            title = "",
            description = "" // Handled via custom graphical layout below
        ),
        OnboardingSlide(
            title = "ANONYMITY FIRST",
            description = "SOMETIMES THE TRUTH IS EASIER TO SHARE WHEN NO ONE KNOWS WHO YOU ARE."
        ),
        OnboardingSlide(
            title = "NOTHING LASTS FOREVER",
            description = "EVERY ECHO FADES AWAY AFTER 48 HOURS. WHAT YOU SAY WON’T STAY."
        ),
        OnboardingSlide(
            title = "TUNE YOUR FREQUENCY",
            description = "SOMEWHERE, SOMEONE IS FEELING EXACTLY WHAT YOU FEEL. CHOOSE YOUR RIGHT MOOD."
        ),
        OnboardingSlide(
            title = "ENERGY BUILDS PRESENCE",
            description = "ACTIVITY FUELS ENERGY. THE MORE YOU INTERACT, THE STRONGER YOUR ECHOES BECOME."
        )
    )

    val totalPages = slides.size + 1
    val pagerState = rememberPagerState(pageCount = { totalPages })
    var userName by remember { mutableStateOf("") }
    val isFinalPage = pagerState.currentPage == slides.size

    fun openLink(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.01f, targetValue = 0.06f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "glow_alpha"
    )

    // ✅ FORCE LAYOUT LOCK
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 1f)) {

        Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {

            // ─────────────────────────────────────────────────────────────────
            // LAYER 1 │ z=10  DYNAMIC HEADER (Starts Center, moves Top)
            // ─────────────────────────────────────────────────────────────────
            val rawOffset = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
            val logoProgress = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f).transform(rawOffset)

            val centerLogoY = 220.dp
            val currentLogoY = centerLogoY - ((centerLogoY - logoOffsetY) * logoProgress)
            val currentLogoScale = 1.0f + (0.5f * (1f - logoProgress))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = currentLogoY)
                        .graphicsLayer {
                            scaleX = currentLogoScale
                            scaleY = currentLogoScale
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "E C H O",
                        style = VoidTypography.displayLarge.copy(fontSize = 26.sp, letterSpacing = 2.sp, color = Color.White)
                    )
                    Text(
                        text = "SEEK THE SILENCE",
                        style = VoidTypography.labelSmall.copy(color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.sp)
                    )
                }
            }

            // ─── SKIP BUTTON (TOP RIGHT) ────────
            AnimatedVisibility(
                visible = !isFinalPage,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = logoOffsetY + 12.dp, end = 24.dp)
                    .align(Alignment.TopEnd)
                    .zIndex(10f),
                enter = fadeIn(animationSpec = tween(800)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                Text(
                    text = "SKIP",
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { pagerState.animateScrollToPage(slides.size, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
                        onSkip()
                    }.padding(8.dp),
                    style = VoidTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.45f))
                )
            }

            // ─── SLIDE INDICATOR (TOP LEFT - CIRCULAR PROGRESS) ────────
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = logoOffsetY + 18.dp, start = 24.dp)
                    .align(Alignment.TopStart)
                    .zIndex(10f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Animated Circular Progress Ring
                    val progress = (pagerState.currentPage + 1).toFloat() / totalPages
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        label = "circular_progress"
                    )

                    Canvas(modifier = Modifier.size(16.dp)) {
                        val strokeWidth = 1.5.dp.toPx()
                        // Background track
                        drawCircle(
                            color = Color.White.copy(alpha = 0.2f),
                            style = Stroke(width = strokeWidth)
                        )
                        // Active progress arc
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Micro Progress Text
                    Text(
                        text = "${pagerState.currentPage + 1}/$totalPages",
                        style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.5f), letterSpacing = 2.sp)
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // LAYER 2 │ z=1   SINGLE LOOPING VIDEO ENGINE
            // ─────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 120.dp).zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(300.dp).background(Brush.radialGradient(listOf(Color.White.copy(glowAlpha), Color.Transparent))).blur(70.dp))

                val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val videoAlpha = when {
                    scrollPosition <= 1f -> scrollPosition // Fades in moving from 0 -> 1
                    scrollPosition >= (slides.size - 1).toFloat() -> (slides.size.toFloat() - scrollPosition).coerceIn(0f, 1f) // Fades out entering last slide
                    else -> 1f
                }

                if (videoAlpha > 0f) {
                    SingleVideoPlayer(
                        videoRes = R.raw.v1,
                        alpha = videoAlpha
                    )
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // LAYER 3 │ z=2   PROTECTIVE GRADIENTS
            // ─────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxSize().zIndex(2f).background(
                    Brush.verticalGradient(
                        0.00f to VoidBlack.copy(alpha = 0.90f),
                        0.15f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.60f to VoidBlack.copy(alpha = 0.95f),
                        1.00f to VoidBlack
                    )
                )
            )

            // ─────────────────────────────────────────────────────────────────
            // LAYER 4 │ z=5   TEXT PAGER & IDENTITY SLIDE
            // ─────────────────────────────────────────────────────────────────
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().zIndex(5f)) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = pageOffset.absoluteValue
                val isCurrent = pagerState.currentPage == page && !pagerState.isScrollInProgress

                Box(modifier = Modifier.fillMaxSize().padding(bottom = 140.dp), contentAlignment = Alignment.BottomCenter) {
                    if (page < slides.size) {

                        // ─── SLIDE 0: CUSTOM AESTHETIC GRAPHICAL LAYOUT ───
                        if (page == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp)
                                    .graphicsLayer {
                                        alpha = (1f - absOffset * 2f).coerceIn(0f, 1f)
                                        translationX = pageOffset * 200f
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = isCurrent,
                                    enter = slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(1000, 400)) + fadeIn(animationSpec = tween(1000, 400)),
                                    exit = fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("A SANCTUARY FROM THE NOISE.", style = VoidTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 3.sp, color = Color.White.copy(0.5f)))
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(0.8f)) {
                                            Box(Modifier.height(1.dp).weight(1f).background(Color.White.copy(0.1f)))
                                            Text("  ///  ", style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.3f)))
                                            Box(Modifier.height(1.dp).weight(1f).background(Color.White.copy(0.1f)))
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text("NO PROFILES.", style = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 2.sp, color = Color.White))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("NO FOLLOWERS.", style = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 2.sp, color = Color.White))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("NO JUDGMENT.", style = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 2.sp, color = Color.White))
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Text("A PURE, ANONYMOUS STREAM OF CONSCIOUSNESS.", style = VoidTypography.labelSmall.copy(fontSize = 11.sp, lineHeight = 20.sp, letterSpacing = 2.sp, color = Color.White.copy(0.5f)), textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        } else {
                            // ─── SLIDES 1-4: STANDARD LAYOUT ───
                            val slide = slides[page]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp)
                                    .graphicsLayer {
                                        alpha = (1f - absOffset * 2f).coerceIn(0f, 1f)
                                        translationX = pageOffset * 200f
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    visible = isCurrent,
                                    enter = slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(1000, 400)) + fadeIn(animationSpec = tween(1000, 400)),
                                    exit = fadeOut(animationSpec = tween(300))
                                ) {
                                    Text(
                                        text = slide.title,
                                        style = VoidTypography.displayLarge.copy(fontSize = 22.sp, letterSpacing = 2.sp, color = Color.White),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                AnimatedVisibility(
                                    visible = isCurrent,
                                    enter = slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(1000, 600)) + fadeIn(animationSpec = tween(1000, 600)),
                                    exit = fadeOut(animationSpec = tween(300))
                                ) {
                                    Text(
                                        text = slide.description,
                                        style = VoidTypography.labelSmall.copy(fontSize = 12.sp, lineHeight = 24.sp, letterSpacing = 1.sp, color = Color.White.copy(0.55f)),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // ─── FINAL NAME SETUP SLIDE ───
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VoidBlack)
                                .graphicsLayer { alpha = (1f - absOffset * 2f).coerceIn(0f, 1f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "NAME YOUR SOUL",
                                    style = VoidTypography.displayLarge.copy(fontSize = 24.sp, letterSpacing = 3.sp, color = Color.White),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "CHOOSE A NAME ONLY YOU WILL SEE.",
                                    style = VoidTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp, color = Color.White.copy(0.4f)),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(48.dp))

                                BasicTextField(
                                    value = userName,
                                    onValueChange = { if (it.length <= 15) userName = it },
                                    textStyle = VoidTypography.displayLarge.copy(fontSize = 20.sp, letterSpacing = 1.sp, color = Color.White, textAlign = TextAlign.Center),
                                    cursorBrush = SolidColor(Color.White),
                                    singleLine = true,
                                    decorationBox = { inner ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .background(Color.White.copy(0.02f), RoundedCornerShape(12.dp))
                                                .border(0.5.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (userName.isEmpty()) Text("ENTER A NAME...", style = VoidTypography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.sp, color = Color.White.copy(0.2f)))
                                            inner()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${userName.length} / 15",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = if (userName.length >= 15) Color.White.copy(0.6f) else Color.White.copy(0.2f), textAlign = TextAlign.End)
                                )

                                Spacer(modifier = Modifier.height(48.dp))

                                val isReady = userName.isNotBlank()
                                val btnBgColor by animateColorAsState(targetValue = if (isReady) Color.White else Color.White.copy(0.05f), animationSpec = tween(500), label = "btnBg")
                                val btnTextColor by animateColorAsState(targetValue = if (isReady) Color.Black else Color.White.copy(0.2f), animationSpec = tween(500), label = "btnText")

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(25.dp))
                                        .background(btnBgColor)
                                        .clickable(enabled = isReady, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onEnterClick(userName.trim())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ENTER THE VOID",
                                        style = VoidTypography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = btnTextColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                // ✅ FOOLPROOF CLICKABLE LINKS
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = "// BY INITIATING SESSION, USER ACCEPTS",
                                        style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.3f))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "PROTOCOL_TERMS",
                                            modifier = Modifier.clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { openLink("https://www.voidecho.space/terms") },
                                            style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.6f), textDecoration = TextDecoration.Underline)
                                        )
                                        Text(
                                            text = " AND ",
                                            style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.3f))
                                        )
                                        Text(
                                            text = "PRIVACY_DIRECTIVES",
                                            modifier = Modifier.clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { openLink("https://www.voidecho.space/privacy") },
                                            style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(0.6f), textDecoration = TextDecoration.Underline)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────────
            // LAYER 6 │ z=15  BOTTOM NAV (Next Button Only)
            // ─────────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 48.dp, end = 32.dp)
                    .zIndex(15f)
            ) {
                if (!isFinalPage) {
                    Box(Modifier.size(56.dp).align(Alignment.CenterEnd).clip(CircleShape).background(Color.White).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = tween(1000, easing = FastOutSlowInEasing)) }
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// ─── Single Looping Video Player Helper ───────────────────────────────────────────

@Composable
fun SingleVideoPlayer(videoRes: Int, alpha: Float) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$videoRes")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                controllerAutoShow = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setOnTouchListener { _, _ -> true }
            }
        },
        modifier = Modifier
            .fillMaxSize(0.85f)
            .graphicsLayer { this.alpha = alpha }
    )
}