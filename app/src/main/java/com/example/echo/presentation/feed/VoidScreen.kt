package com.example.echo.presentation.feed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.echo.R
import com.example.echo.core.theme.*
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.presentation.components.*
import com.example.echo.presentation.post.WhisperOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

@Composable
fun VoidScreen(
    viewModel: VoidViewModel = hiltViewModel()
) {
    val echoes by viewModel.echoes.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeSouls.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isPanelOpen by viewModel.isNotificationPanelOpen.collectAsStateWithLifecycle()
    val showGetStarted by viewModel.showGetStarted.collectAsStateWithLifecycle()
    val hasUnread by viewModel.hasUnread.collectAsStateWithLifecycle()

    // Frequency States
    val sessionFrequency by viewModel.sessionFrequency.collectAsStateWithLifecycle()
    val showFrequencyModal by viewModel.showFrequencyModal.collectAsStateWithLifecycle()

    // ✅ STRICT CHAINED LOGIC: Frequency Modal ONLY triggers if GetStarted is false (completed)
    LaunchedEffect(showGetStarted) {
        if (!showGetStarted) {
            delay(600) // Wait for GetStarted to fully exit
            viewModel.openFrequencyModal()
        }
    }

    val categories = viewModel.categories
    val selectedFeedCategory by viewModel.selectedFeedCategory.collectAsStateWithLifecycle()

    val filteredEchoes = remember(echoes, selectedFeedCategory) {
        if (selectedFeedCategory == "All") echoes else echoes.filter { it.category == selectedFeedCategory }
    }

    val deepLinkEcho by viewModel.deepLinkEcho.collectAsStateWithLifecycle()
    var editingEchoId by remember { mutableStateOf<String?>(null) }
    var editingInitialText by remember { mutableStateOf("") }
    var showWhisper by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val listState = rememberLazyGridState()
    val categoryListState = rememberLazyListState()
    var selectedEchoId by remember { mutableStateOf<String?>(null) }

    val selectedEcho = remember(filteredEchoes, selectedEchoId, deepLinkEcho) {
        filteredEchoes.find { it.id == selectedEchoId } ?: deepLinkEcho?.takeIf { it.id == selectedEchoId }
    }

    var cachedEcho by remember { mutableStateOf<EchoDto?>(null) }
    if (selectedEcho != null) cachedEcho = selectedEcho

    var echoToReport by remember { mutableStateOf<EchoDto?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // ✅ SESSION COLOR: Solid vibrant color for the new frequency icon
    val sessionColor by animateColorAsState(
        targetValue = when(sessionFrequency) {
            0 -> Color(0xFFFF453A) // Disturbed
            1 -> Color(0xFFFF9F0A) // Heavy
            2 -> Color(0xFF94A3B8) // Neutral
            3 -> Color(0xFF64D2FF) // Calm
            4 -> Color(0xFF34C759) // Radiant
            else -> Color(0xFF94A3B8)
        },
        animationSpec = tween(1000), label = "sessionColor"
    )

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.onRealtimeNotificationReceived()
            }
        }
        val filter = IntentFilter("com.example.echo.NEW_NOTIFICATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadFeed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = isPanelOpen || showWhisper || showAbout || selectedEchoId != null || echoToReport != null || showGetStarted || showFrequencyModal) {
        when {
            showFrequencyModal -> viewModel.dismissFrequencyModal()
            echoToReport != null -> echoToReport = null
            selectedEchoId != null -> { selectedEchoId = null; viewModel.loadFeed() }
            showWhisper -> showWhisper = false
            showAbout -> showAbout = false
            isPanelOpen -> { viewModel.closeNotificationPanel(); viewModel.loadFeed() }
        }
    }

    val isHomeCovered = isPanelOpen || showAbout || showWhisper || selectedEchoId != null || (echoToReport != null && selectedEchoId == null) || showGetStarted || showFrequencyModal
    val homeBlur by animateDpAsState(targetValue = if (isHomeCovered) 8.dp else 0.dp, label = "homeBlur", animationSpec = tween(400))
    val dimAlpha by animateFloatAsState(targetValue = if (isHomeCovered) 0.6f else 0f, label = "dimAlpha", animationSpec = tween(400))

    val density = LocalDensity.current
    val refreshTriggerPx = with(density) { 80.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }
    val scope = rememberCoroutineScope()
    var pullOffsetY by remember { mutableFloatStateOf(0f) }
    var isSnapping by remember { mutableStateOf(false) }
    var swipeDragOffset by remember { mutableFloatStateOf(0f) }

    fun snapTo(target: Float) {
        if (isSnapping) return
        isSnapping = true
        scope.launch {
            val start = pullOffsetY
            val delta = target - start
            val duration = 220
            val startTime = withFrameNanos { it }
            while (true) {
                val t = ((withFrameNanos { it } - startTime) / 1_000_000f / duration).coerceIn(0f, 1f)
                pullOffsetY = start + delta * t
                if (t >= 1f) break
            }
            pullOffsetY = target
            isSnapping = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing || isSnapping) return Offset.Zero
                if (pullOffsetY > 0f && available.y < 0f) {
                    val newOffset = (pullOffsetY + available.y).coerceAtLeast(0f)
                    val consumed = pullOffsetY - newOffset
                    pullOffsetY = newOffset
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing || isSnapping) return Offset.Zero
                if (source == NestedScrollSource.Drag && available.y > 0f) {
                    val newOffset = (pullOffsetY + available.y * 0.45f).coerceAtMost(maxDragPx)
                    pullOffsetY = newOffset
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isRefreshing || isSnapping) return Velocity.Zero
                if (pullOffsetY >= refreshTriggerPx) {
                    viewModel.refresh()
                    snapTo(refreshTriggerPx)
                    return Velocity.Zero
                } else if (pullOffsetY > 0f) {
                    snapTo(0f)
                    return Velocity.Zero
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullOffsetY > 0f) snapTo(0f)
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && homeBlur > 0.dp)
                        Modifier.blur(homeBlur)
                    else Modifier
                )
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(VoidGradientTop))
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                        .zIndex(2f)
                ) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) { ActiveSoulsChip(count = activeCount) }
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "E C H O", style = VoidTypography.displayLarge.copy(fontSize = 26.sp, letterSpacing = 1.sp))
                        Text(text = "SEEK THE SILENCE", style = VoidTypography.labelSmall)
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAbout = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.HelpOutline, "About", tint = Stone400, modifier = Modifier.size(24.dp))
                        }
                        val infiniteTransition = rememberInfiniteTransition(label = "Blink")
                        val blinkAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "BlinkAlpha"
                        )
                        IconButton(onClick = { viewModel.toggleNotificationPanel() }, modifier = Modifier.size(28.dp)) {
                            if (hasUnread) {
                                Icon(Icons.Filled.Notifications, "New", tint = NeonWhite.copy(alpha = blinkAlpha), modifier = Modifier.size(28.dp))
                            } else {
                                Icon(Icons.Outlined.Notifications, "Notifications", tint = if (isPanelOpen) NeonWhite else Stone400, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                // Spaces Filter
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).zIndex(2f)
                ) {
                    LazyRow(
                        state = categoryListState,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedFeedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) NeonWhite else Stone900)
                                    .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFeedCategory(category)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.uppercase(),
                                    style = VoidTypography.labelSmall.copy(
                                        color = if (isSelected) VoidBlack else Stone400,
                                        letterSpacing = 1.sp,
                                        fontSize = 12.sp
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

                // Feed Content
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(selectedFeedCategory) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val minDrag = 120f
                                if (swipeDragOffset < -minDrag) {
                                    val idx = categories.indexOf(selectedFeedCategory)
                                    if (idx < categories.size - 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFeedCategory(categories[idx + 1])
                                    }
                                } else if (swipeDragOffset > minDrag) {
                                    val idx = categories.indexOf(selectedFeedCategory)
                                    if (idx > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFeedCategory(categories[idx - 1])
                                    }
                                }
                                swipeDragOffset = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                swipeDragOffset += dragAmount
                            }
                        )
                    }
                ) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).height(40.dp).zIndex(0f)) {
                        if (pullOffsetY > 5f) {
                            PremiumLoader(isRefreshing = isRefreshing, pullProgress = (pullOffsetY / refreshTriggerPx).coerceIn(0f, 1f))
                        }
                    }

                    AnimatedContent(
                        targetState = selectedFeedCategory,
                        transitionSpec = {
                            val targetIdx = categories.indexOf(targetState)
                            val initialIdx = categories.indexOf(initialState)
                            val slideDir = if (targetIdx > initialIdx) 1 else -1

                            (fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(400, easing = EaseOutQuart)) { it * slideDir / 2 })
                                .togetherWith(fadeOut(animationSpec = tween(250)) + slideOutHorizontally(animationSpec = tween(400, easing = EaseOutQuart)) { -it * slideDir / 2 })
                        },
                        label = "SpaceTransition",
                        modifier = Modifier.fillMaxSize().graphicsLayer { translationY = pullOffsetY }.zIndex(1f)
                    ) { currentCategory ->
                        val animatingEchoes = if (currentCategory == "All") echoes else echoes.filter { it.category == currentCategory }

                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(2) }) { SpaceHeaderCard(space = currentCategory) }

                            if (animatingEchoes.isEmpty() && !isRefreshing) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "THIS SPACE IS QUIET.",
                                                style = VoidTypography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = com.example.echo.core.theme.Stone600,
                                                    letterSpacing = 2.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Be the first to Echo.",
                                                style = VoidTypography.bodyLarge.copy(fontSize = 12.sp, color = com.example.echo.core.theme.Stone600)
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(items = animatingEchoes, key = { it.id ?: "error_${it.hashCode()}" }) { echo ->
                                    EchoCard(
                                        echoId = echo.id ?: "",
                                        content = echo.content,
                                        createdAt = echo.createdAt,
                                        updatedAt = echo.updatedAt,
                                        colorCode = echo.colorCode,
                                        signatureId = echo.signatureId,
                                        category = echo.category,
                                        replies = echo.replies,
                                        onReport = { echoToReport = echo },
                                        onEdit = {
                                            editingEchoId = echo.id
                                            editingInitialText = echo.content
                                            showWhisper = true
                                        },
                                        onClick = { selectedEchoId = echo.id }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // The Dark Fade Gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(180.dp)
                    .zIndex(1.5f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, VoidBlack.copy(alpha = 0.95f))))
            )

            // PREMIUM CONSOLE CLUSTER
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).zIndex(2f)) {
                Surface(color = NeonWhite, shape = RoundedCornerShape(32.dp), shadowElevation = 12.dp) {
                    Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {

                        // ✅ DYNAMIC FREQUENCY ICON (Mood Colored Waveform)
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openFrequencyModal()
                        }, contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val strokeWidth = 2.5.dp.toPx()
                                val gap = size.width / 4
                                val heights = listOf(0.4f, 0.8f, 1.0f, 0.6f, 0.4f) // Waveform pattern

                                heights.forEachIndexed { index, heightMultiplier ->
                                    val x = index * gap
                                    val barHeight = size.height * heightMultiplier
                                    val shrinkAmount = (size.height - barHeight) / 2

                                    drawLine(
                                        color = sessionColor, // Uses the solid active mood color
                                        start = Offset(x, shrinkAmount),
                                        end = Offset(x, size.height - shrinkAmount),
                                        strokeWidth = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(VoidBlack.copy(alpha = 0.1f)))

                        // 2. WHISPER ICON
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).clickable {
                            editingEchoId = null; editingInitialText = ""; showWhisper = true
                        }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, "Whisper", tint = VoidBlack, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            if (dimAlpha > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)).zIndex(3f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { })
            }
        }

        // --- OVERLAYS ---

        GlassScrim(visible = isPanelOpen, onDismiss = { viewModel.closeNotificationPanel(); viewModel.loadFeed() })
        AnimatedVisibility(visible = isPanelOpen, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd).zIndex(4f)) {
            NotificationPanel(notifications = notifications, onClose = { viewModel.closeNotificationPanel() },
                onNotificationClick = { id -> viewModel.openEchoFromNotification(id) { selectedEchoId = it.id } })
        }

        GlassScrim(visible = showAbout, onDismiss = { showAbout = false })
        AnimatedVisibility(visible = showAbout, enter = fadeIn() + scaleIn(initialScale = 0.95f), exit = fadeOut() + scaleOut(targetScale = 0.95f), modifier = Modifier.zIndex(5f)) {
            AboutOverlay(onDismiss = { showAbout = false }, onReplayProtocol = { showAbout = false; viewModel.replayGetStarted() })
        }

        GlassScrim(visible = showWhisper, onDismiss = { showWhisper = false })
        AnimatedVisibility(visible = showWhisper, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }, modifier = Modifier.align(Alignment.TopCenter).zIndex(5f)) {
            key(editingEchoId ?: "new") {
                WhisperOverlay(onDismiss = { showWhisper = false }, initialText = editingInitialText,
                    onSend = { t, cat, col -> if (editingEchoId != null) viewModel.updateEcho(editingEchoId!!, t) else viewModel.postEcho(t, cat, col); showWhisper = false })
            }
        }

        GlassScrim(visible = selectedEchoId != null, onDismiss = { selectedEchoId = null; viewModel.loadFeed() })
        AnimatedVisibility(visible = selectedEchoId != null, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f), modifier = Modifier.fillMaxSize().zIndex(6f)) {
            // ✅ BLUR REMOVED FROM OVERLAY BOX
            Box(modifier = Modifier.fillMaxSize()) {
                cachedEcho?.let { echo ->
                    DetailOverlay(echo = echo, onDismiss = { selectedEchoId = null; viewModel.loadFeed() },
                        onReply = { r -> echo.id?.let { viewModel.postReply(it, r) } },
                        onUpdateReply = { id, c -> viewModel.updateReply(id, c) },
                        onEditEcho = { selectedEchoId = null; editingEchoId = echo.id; editingInitialText = echo.content; showWhisper = true },
                        onReport = { echoToReport = echo })
                }
            }
        }

        AnimatedVisibility(visible = echoToReport != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(10f)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { echoToReport = null })
            ReportDialog(onDismiss = { echoToReport = null }, onConfirm = { echoToReport?.id?.let { viewModel.reportEcho(it) }; showReportSuccess = true })
        }

        AnimatedVisibility(visible = showGetStarted, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(11f)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
                GetStartedDialog(onEnterClick = { viewModel.markGetStartedSeen() })
            }
        }

        AnimatedVisibility(visible = showReportSuccess, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(12f)) {
            ReportSuccessOverlay(onDismiss = { showReportSuccess = false; echoToReport = null; selectedEchoId = null })
        }

        // ✅ FINAL TOP-LEVEL MODAL (Slide animation handled inside component)
        FrequencyModal(
            modifier = Modifier.zIndex(100f),
            isVisible = showFrequencyModal && !showGetStarted,
            currentFrequency = sessionFrequency,
            onDismiss = { viewModel.dismissFrequencyModal() },
            onFrequencySet = { viewModel.setFrequency(it) }
        )
    }
}

@Composable
fun SpaceHeaderCard(space: String) {
    val title = space.uppercase()
    val description = when (space) {
        "All" -> "Every Echo. Every voice. Unfiltered."
        "Advice" -> "Ask for guidance. Offer perspective. Real questions, real answers."
        "Vent" -> "Say what you’ve been holding in. No judgment. Just release."
        "Relationships" -> "Love, loss, confusion, and connection. The art of being human."
        else -> "The quiet ideas and realizations. The things you don’t say out loud."
    }

    val imageRes = when (space) {
        "All" -> R.drawable.all
        "Advice" -> R.drawable.advice
        "Vent" -> R.drawable.vent
        "Relationships" -> R.drawable.relationships
        else -> R.drawable.innerthoughts
    }

    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp))) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.9f)))))
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Bottom) {
            Text(text = title, style = VoidTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NeonWhite, letterSpacing = 2.sp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, style = VoidTypography.bodyLarge.copy(color = NeonWhite.copy(alpha = 0.8f)))
        }
    }
}

@Composable
fun GlassScrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = Modifier.zIndex(3f)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(remember { MutableInteractionSource() }, null) { onDismiss() })
    }
}