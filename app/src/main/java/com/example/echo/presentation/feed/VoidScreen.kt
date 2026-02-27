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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.echo.presentation.components.ActiveSoulsChip
import com.example.echo.presentation.components.EchoCard
import com.example.echo.presentation.components.NotificationPanel
import com.example.echo.presentation.components.PremiumLoader
import com.example.echo.presentation.components.AboutOverlay
import com.example.echo.presentation.components.ReportDialog
import com.example.echo.presentation.components.GetStartedDialog
import com.example.echo.presentation.components.ReportSuccessOverlay
import com.example.echo.presentation.post.WhisperOverlay
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
    if (selectedEcho != null) {
        cachedEcho = selectedEcho
    }

    var echoToReport by remember { mutableStateOf<EchoDto?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(deepLinkEcho) {
        if (deepLinkEcho != null) {
            selectedEchoId = deepLinkEcho?.id
            viewModel.consumeDeepLink()
        }
    }

    LaunchedEffect(showWhisper) {
        if (!showWhisper) {
            editingEchoId = null
            editingInitialText = ""
        }
    }

    // ✅ FIXED: Auto-scroll categories row, offset slightly so it doesn't hide behind the shadow
    LaunchedEffect(selectedFeedCategory) {
        val index = categories.indexOf(selectedFeedCategory)
        if (index >= 0) {
            // Scrolls to the item just before it to ensure the selected space sits comfortably in the visible area
            categoryListState.animateScrollToItem(maxOf(0, index - 1))
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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

    BackHandler(enabled = isPanelOpen || showWhisper || showAbout || selectedEchoId != null || echoToReport != null || showGetStarted) {
        when {
            echoToReport != null -> echoToReport = null
            selectedEchoId != null -> { selectedEchoId = null; viewModel.loadFeed() }
            showWhisper -> showWhisper = false
            showAbout -> showAbout = false
            isPanelOpen -> { viewModel.closeNotificationPanel(); viewModel.loadFeed() }
        }
    }

    val isHomeCovered = isPanelOpen || showAbout || showWhisper || selectedEchoId != null || (echoToReport != null && selectedEchoId == null) || showGetStarted
    val homeBlur by animateDpAsState(targetValue = if (isHomeCovered) 10.dp else 0.dp, label = "homeBlur", animationSpec = tween(400))
    val dimAlpha by animateFloatAsState(targetValue = if (isHomeCovered) 0.6f else 0f, label = "dimAlpha", animationSpec = tween(400))
    val isDetailCovered = selectedEchoId != null && echoToReport != null
    val detailBlur by animateDpAsState(targetValue = if (isDetailCovered) 10.dp else 0.dp, label = "detailBlur", animationSpec = tween(400))

    val density = LocalDensity.current
    val refreshTriggerPx = with(density) { 80.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }
    val scope = rememberCoroutineScope()
    var pullOffsetY by remember { mutableFloatStateOf(0f) }
    var isSnapping by remember { mutableStateOf(false) }

    // Swipe state
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

                // SPACES FILTER ROW
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .zIndex(2f)
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

                // Feed Wrapper with Horizontal Swipe Detection
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection)
                    .pointerInput(selectedFeedCategory) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val minDrag = 120f
                                if (swipeDragOffset < -minDrag) { // Swiped Left (Next Space)
                                    val idx = categories.indexOf(selectedFeedCategory)
                                    if (idx < categories.size - 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFeedCategory(categories[idx + 1])
                                    }
                                } else if (swipeDragOffset > minDrag) { // Swiped Right (Prev Space)
                                    val idx = categories.indexOf(selectedFeedCategory)
                                    if (idx > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFeedCategory(categories[idx - 1])
                                    }
                                }
                                swipeDragOffset = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
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

                    // Apple-like Horizontal Slide Transition
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
                            // Permanent Space Header Card
                            item(span = { GridItemSpan(2) }) {
                                SpaceHeaderCard(space = currentCategory)
                            }

                            if (animatingEchoes.isEmpty() && !isRefreshing) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "THIS SPACE IS QUIET.",
                                                style = VoidTypography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Stone600,
                                                    letterSpacing = 2.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            // ✅ FIXED: Using Typewriter Font style for Empty State subtext
                                            Text(
                                                text = "Be the first to echo.",
                                                style = VoidTypography.bodyLarge.copy(
                                                    fontSize = 12.sp,
                                                    color = Stone600,
                                                    letterSpacing = 1.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(
                                    items = animatingEchoes,
                                    key = { it.id ?: "error_${it.hashCode()}" },
                                    contentType = { "echo" }
                                ) { echo ->
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .zIndex(1.5f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, VoidBlack),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            FloatingActionButton(
                onClick = {
                    editingEchoId = null
                    editingInitialText = ""
                    showWhisper = true
                },
                containerColor = NeonWhite,
                contentColor = VoidBlack,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(64.dp)
                    .zIndex(2f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Whisper")
            }

            if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                        .zIndex(3f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                )
            }
        }

        GlassScrim(visible = isPanelOpen, onDismiss = {
            viewModel.closeNotificationPanel()
            viewModel.loadFeed()
        })
        AnimatedVisibility(
            visible = isPanelOpen,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd).zIndex(4f)
        ) {
            NotificationPanel(
                notifications = notifications,
                onClose = { viewModel.closeNotificationPanel() },
                onNotificationClick = { echoId -> viewModel.openEchoFromNotification(echoId) { selectedEchoId = it.id } }
            )
        }

        GlassScrim(visible = showAbout, onDismiss = { showAbout = false })
        AnimatedVisibility(
            visible = showAbout,
            enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier.zIndex(5f)
        ) {
            AboutOverlay(onDismiss = { showAbout = false }, onReplayProtocol = { showAbout = false; viewModel.replayGetStarted() })
        }

        GlassScrim(visible = showWhisper, onDismiss = { showWhisper = false })
        AnimatedVisibility(
            visible = showWhisper,
            enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(tween(200)) + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter).zIndex(5f)
        ) {
            key(editingEchoId ?: "new_post") {
                WhisperOverlay(
                    onDismiss = {
                        showWhisper = false
                        editingEchoId = null
                        editingInitialText = ""
                    },
                    initialText = editingInitialText,
                    onSend = { text, category, color ->
                        if (editingEchoId != null) {
                            viewModel.updateEcho(editingEchoId!!, text)
                        } else {
                            viewModel.postEcho(text, category, color)
                        }
                        showWhisper = false
                        editingEchoId = null
                        editingInitialText = ""
                    }
                )
            }
        }

        GlassScrim(visible = selectedEchoId != null, onDismiss = {
            selectedEchoId = null
            viewModel.loadFeed()
        })

        AnimatedVisibility(
            visible = selectedEchoId != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f),
            modifier = Modifier.fillMaxSize().zIndex(6f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && detailBlur > 0.dp)
                            Modifier.blur(detailBlur)
                        else Modifier
                    )
            ) {
                if (cachedEcho != null) {
                    DetailOverlay(
                        echo = cachedEcho!!,
                        onDismiss = {
                            selectedEchoId = null
                            viewModel.loadFeed()
                        },
                        onReply = { reply -> cachedEcho!!.id?.let { viewModel.postReply(it, reply) } },
                        onUpdateReply = { replyId, newContent -> viewModel.updateReply(replyId, newContent) },
                        onEditEcho = {
                            selectedEchoId = null
                            editingEchoId = cachedEcho!!.id
                            editingInitialText = cachedEcho!!.content
                            showWhisper = true
                        },
                        onReport = { echoToReport = cachedEcho }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = echoToReport != null,
            enter = fadeIn(tween(200)) + scaleIn(tween(300), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
            modifier = Modifier.zIndex(7f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { echoToReport = null })
            ReportDialog(
                onDismiss = { echoToReport = null },
                onConfirm = { echoToReport?.id?.let { id -> viewModel.reportEcho(id) }; showReportSuccess = true }
            )
        }

        AnimatedVisibility(
            visible = showGetStarted,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.zIndex(8f)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
                GetStartedDialog(onEnterClick = { viewModel.markGetStartedSeen() })
            }
        }

        AnimatedVisibility(
            visible = showReportSuccess,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.zIndex(9f)
        ) {
            ReportSuccessOverlay(onDismiss = { showReportSuccess = false; echoToReport = null; selectedEchoId = null })
        }
    }
}

// ✅ NEW: Image Backgrounds with Premium Dark Gradient
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // 1. The Background Image
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Background for $space",
            contentScale = ContentScale.Crop, // Zooms the image to perfectly fill the box
            modifier = Modifier.fillMaxSize()
        )

        // 2. The Premium Dark Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // 3. The Text Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = VoidTypography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NeonWhite,
                    letterSpacing = 2.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = VoidTypography.bodyLarge.copy(color = NeonWhite.copy(alpha = 0.8f))
            )
        }
    }
}

@Composable
fun GlassScrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = Modifier.zIndex(3f)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() })
    }
}