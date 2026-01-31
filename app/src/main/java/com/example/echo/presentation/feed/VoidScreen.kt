package com.example.echo.presentation.feed

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.echo.core.theme.*
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.presentation.components.ActiveSoulsChip
import com.example.echo.presentation.components.EchoCard
import com.example.echo.presentation.components.NotificationPanel
import com.example.echo.presentation.components.PremiumLoader
import com.example.echo.presentation.components.AboutOverlay
import com.example.echo.presentation.components.ReportDialog
import com.example.echo.presentation.components.GetStartedDialog
import com.example.echo.presentation.components.ReportSuccessOverlay // Import the new overlay
import com.example.echo.presentation.post.WhisperOverlay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos

@Composable
fun VoidScreen(
    viewModel: VoidViewModel = hiltViewModel()
) {
    // 1. DATA COLLECTORS
    val echoes by viewModel.echoes.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeSouls.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isPanelOpen by viewModel.isNotificationPanelOpen.collectAsStateWithLifecycle()
    val showGetStarted by viewModel.showGetStarted.collectAsStateWithLifecycle()

    // 2. UI STATES
    var showWhisper by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var selectedEcho by remember { mutableStateOf<EchoDto?>(null) }
    var echoToReport by remember { mutableStateOf<EchoDto?>(null) }

    // NEW: State for Report Success Screen
    var showReportSuccess by remember { mutableStateOf(false) }

    // 3. BACK GESTURE HANDLER
    BackHandler(enabled = isPanelOpen || showWhisper || showAbout || selectedEcho != null || echoToReport != null || showGetStarted) {
        when {
            echoToReport != null -> echoToReport = null
            selectedEcho != null -> selectedEcho = null
            showWhisper -> showWhisper = false
            showAbout -> showAbout = false
            isPanelOpen -> viewModel.closeNotificationPanel()
        }
    }

    // 4. BLUR STATES
    val isHomeCovered = isPanelOpen || showAbout || showWhisper || selectedEcho != null || (echoToReport != null && selectedEcho == null) || showGetStarted
    val homeBlur by animateDpAsState(targetValue = if (isHomeCovered) 16.dp else 0.dp, label = "homeBlur", animationSpec = tween(500))

    val isDetailCovered = selectedEcho != null && echoToReport != null
    val detailBlur by animateDpAsState(targetValue = if (isDetailCovered) 16.dp else 0.dp, label = "detailBlur", animationSpec = tween(500))

    // 5. PHYSICS (No changes here)
    val density = LocalDensity.current
    val refreshTriggerPx = with(density) { 80.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }
    val scope = rememberCoroutineScope()
    var pullOffsetY by remember { mutableFloatStateOf(0f) }
    var isSnapping by remember { mutableStateOf(false) }

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
                if (available.y > 0f) {
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
                } else {
                    snapTo(0f)
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullOffsetY > 0f) snapTo(0f)
    }

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize().background(VoidBlack)
    ) {
        // LAYER 1: HOME
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && homeBlur > 0.dp) Modifier.blur(homeBlur) else Modifier)
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
                        IconButton(onClick = { viewModel.toggleNotificationPanel() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Notifications, "Notifications", tint = if (isPanelOpen) NeonWhite else Stone400, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Feed
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(nestedScrollConnection)
                ) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).height(40.dp).zIndex(0f)) {
                        if (pullOffsetY > 5f) {
                            PremiumLoader(isRefreshing = isRefreshing, pullProgress = (pullOffsetY / refreshTriggerPx).coerceIn(0f, 1f))
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().offset { IntOffset(0, pullOffsetY.roundToInt()) }.zIndex(1f)
                    ) {
                        items(echoes) { echo ->
                            FadeInItem {
                                EchoCard(
                                    content = echo.content,
                                    createdAt = echo.createdAt,
                                    onReport = { echoToReport = echo },
                                    onClick = { selectedEcho = echo }
                                )
                            }
                        }
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick = { showWhisper = true },
                containerColor = NeonWhite,
                contentColor = VoidBlack,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Whisper")
            }
        }

        // LAYER 2: OVERLAYS
        GlassScrim(visible = isPanelOpen, onDismiss = { viewModel.closeNotificationPanel() })
        AnimatedVisibility(
            visible = isPanelOpen,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd).zIndex(4f)
        ) {
            NotificationPanel(
                notifications = notifications,
                onClose = { viewModel.closeNotificationPanel() },
                onNotificationClick = { echoId -> viewModel.openEchoFromNotification(echoId) { selectedEcho = it } }
            )
        }

        GlassScrim(visible = showAbout, onDismiss = { showAbout = false })
        AnimatedVisibility(
            visible = showAbout,
            enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier.zIndex(5f)
        ) {
            AboutOverlay(
                onDismiss = { showAbout = false },
                onReplayProtocol = { showAbout = false; viewModel.replayGetStarted() }
            )
        }

        GlassScrim(visible = showWhisper, onDismiss = { showWhisper = false })
        // WHISPER OVERLAY (Updated Position)
        AnimatedVisibility(
            visible = showWhisper,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it }), // Slide in from top
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it }), // Slide out to top
            modifier = Modifier.align(Alignment.TopCenter).zIndex(5f) // Align to Top
        ) {
            WhisperOverlay(
                onDismiss = { showWhisper = false },
                onSend = { text -> viewModel.postEcho(text); showWhisper = false }
            )
        }

        // LAYER 3: DETAIL
        if (selectedEcho != null) {
            GlassScrim(visible = true, onDismiss = { selectedEcho = null })
            Box(
                modifier = Modifier.fillMaxSize().zIndex(6f)
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && detailBlur > 0.dp) Modifier.blur(detailBlur) else Modifier)
            ) {
                DetailOverlay(
                    echo = selectedEcho!!,
                    onDismiss = { selectedEcho = null },
                    onReply = { reply -> selectedEcho!!.id?.let { viewModel.postReply(it, reply) } },
                    onReport = { echoToReport = selectedEcho }
                )
            }
        }

        // LAYER 4: REPORT DIALOG
        AnimatedVisibility(
            visible = echoToReport != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier.zIndex(7f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { echoToReport = null })
            ReportDialog(
                onDismiss = { echoToReport = null },
                onConfirm = {
                    echoToReport?.id?.let { id -> viewModel.reportEcho(id) }
                    // Show success screen instead of immediately closing everything
                    showReportSuccess = true
                }
            )
        }

        // LAYER 5: GET STARTED POPUP
        AnimatedVisibility(
            visible = showGetStarted,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.zIndex(8f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))
            ) {
                GetStartedDialog(
                    onEnterClick = { viewModel.markGetStartedSeen() }
                )
            }
        }

        // LAYER 6: REPORT SUCCESS OVERLAY (Topmost)
        // This overlay handles its own dismissal after a delay
        AnimatedVisibility(
            visible = showReportSuccess,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.zIndex(9f)
        ) {
            ReportSuccessOverlay(
                onDismiss = {
                    showReportSuccess = false
                    echoToReport = null
                    selectedEcho = null
                }
            )
        }
    }
}

// ... (FadeInItem & GlassScrim helpers remain) ...
@Composable
fun FadeInItem(content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(600)) }
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha.value }) { content() }
}

@Composable
fun GlassScrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = Modifier.zIndex(3f)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() })
    }
}