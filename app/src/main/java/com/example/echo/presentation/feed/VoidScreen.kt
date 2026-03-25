package com.example.echo.presentation.feed

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.draw.shadow
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun VoidScreen(
    viewModel: VoidViewModel = hiltViewModel()
) {
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val echoes by viewModel.echoes.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeSouls.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isPanelOpen by viewModel.isNotificationPanelOpen.collectAsStateWithLifecycle()
    val showGetStarted by viewModel.showGetStarted.collectAsStateWithLifecycle()
    val hasUnread by viewModel.hasUnread.collectAsStateWithLifecycle()

    val sessionFrequency by viewModel.sessionFrequency.collectAsStateWithLifecycle()
    val showFrequencyModal by viewModel.showFrequencyModal.collectAsStateWithLifecycle()
    val showJourneyOverlay by viewModel.showJourneyOverlay.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }

    val showRatePopup by viewModel.showRatePopup.collectAsStateWithLifecycle()
    val manager = remember { com.google.android.play.core.review.ReviewManagerFactory.create(context) }

    val currentHour = java.time.LocalTime.now().hour
    val rawName = viewModel.displayName.collectAsStateWithLifecycle().value
    val justName = if (rawName.isNullOrBlank()) "soul" else rawName!!

    var displayMessage by remember { mutableStateOf<Pair<String, String>>(Pair("Welcome back,", "$justName.")) }

    var wasOffline by remember { mutableStateOf(false) }
    var showReconnected by remember { mutableStateOf(false) }

    // ✅ ECHOES FOR YOU ENGINE STATE
    var excludedForYou by remember { mutableStateOf(setOf<String>()) }
    var pendingViewedForYou by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(isOffline) {
        if (isOffline) {
            wasOffline = true
            showReconnected = false
        } else if (wasOffline) {
            showReconnected = true
            delay(4000)
            showReconnected = false
            wasOffline = false
        }
    }

    LaunchedEffect(sessionFrequency, currentHour, justName, isOffline, showReconnected) {
        if (isOffline) {
            displayMessage = Pair("Disconnected from", "the Void.")
            return@LaunchedEffect
        }
        if (showReconnected) {
            displayMessage = Pair("Connected back to", "the Void.")
            return@LaunchedEffect
        }

        while (true) {
            val greetings = when (currentHour) {
                in 5..11 -> listOf(Pair("Good morning,", "$justName."), Pair("A new dawn,", "$justName."))
                in 12..17 -> listOf(Pair("Good afternoon,", "$justName."), Pair("Mid-day reflections,", "$justName."))
                in 18..21 -> listOf(Pair("Good evening,", "$justName."), Pair("The day slows down,", "$justName."))
                else -> listOf(Pair("Welcome back,", "$justName."), Pair("Into the night,", "$justName."))
            }

            val moodHooks = when (sessionFrequency) {
                0 -> listOf(Pair("Let it all out,", "$justName."), Pair("Release the noise,", "$justName."))
                1 -> listOf(Pair("Unburden your mind,", "$justName."), Pair("Rest your thoughts here,", "$justName."))
                3 -> listOf(Pair("Share your peace,", "$justName."), Pair("A quiet moment,", "$justName."))
                4 -> listOf(Pair("Guide someone today,", "$justName."), Pair("Share your light,", "$justName."))
                else -> listOf(Pair("How are you feeling today,", "$justName?"), Pair("Start with a whisper,", "$justName."))
            }

            displayMessage = if (Math.random() > 0.8) greetings.random() else moodHooks.random()
            delay(30000L)
        }
    }

    val primaryTextColor = when {
        isOffline -> Color(0xFFFF453A).copy(alpha = 0.8f)
        showReconnected -> Color(0xFF34C759).copy(alpha = 0.8f)
        else -> Color.White.copy(alpha = 0.5f)
    }

    val secondaryTextColor = when {
        isOffline -> Color(0xFFFF453A)
        showReconnected -> Color(0xFF34C759)
        else -> NeonWhite
    }

    val currentMoodName = when (sessionFrequency) {
        0 -> "DISTURBED"
        1 -> "HEAVY"
        3 -> "CALM"
        4 -> "RADIANT"
        else -> "NEUTRAL"
    }

    var showMoodMessage by remember { mutableStateOf(false) }
    LaunchedEffect(showFrequencyModal) {
        if (!showFrequencyModal) {
            showMoodMessage = true
            delay(3500)
            showMoodMessage = false
        }
    }

    LaunchedEffect(showRatePopup) {
        if (showRatePopup) {
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val activity = context as? Activity
                    if (activity != null) manager.launchReviewFlow(activity, reviewInfo)
                }
                viewModel.resetRateTrigger()
            }
        }
    }

    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null) {
            viewModel.triggerUpdateFlow(activity, updateLauncher)
            appUpdateManager.registerListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "New version ready.",
                            actionLabel = "RESTART",
                            duration = SnackbarDuration.Indefinite
                        )
                        if (result == SnackbarResult.ActionPerformed) appUpdateManager.completeUpdate()
                    }
                }
            }
        }
    }

    val categories = viewModel.categories
    val selectedFeedCategory by viewModel.selectedFeedCategory.collectAsStateWithLifecycle()
    val categoryListState = rememberLazyListState()
    LaunchedEffect(selectedFeedCategory) {
        val index = categories.indexOf(selectedFeedCategory)
        if (index != -1) categoryListState.animateScrollToItem(index)
    }

    val filteredEchoes = remember(echoes, selectedFeedCategory) {
        if (selectedFeedCategory == "All") echoes else echoes.filter { it.category == selectedFeedCategory }
    }

    val deepLinkEcho by viewModel.deepLinkEcho.collectAsStateWithLifecycle()

    var editingEchoId by remember { mutableStateOf<String?>(null) }
    var editingInitialText by remember { mutableStateOf("") }
    var showWhisper by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val listState = rememberLazyGridState()
    var selectedEchoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deepLinkEcho) {
        deepLinkEcho?.let { echo ->
            selectedEchoId = echo.id
            viewModel.consumeDeepLink()
        }
    }

    LaunchedEffect(showGetStarted) {
        if (!showGetStarted) {
            delay(600)
            if (selectedEchoId == null && deepLinkEcho == null) viewModel.openFrequencyModal()
        }
    }

    val selectedEcho = remember(filteredEchoes, selectedEchoId, deepLinkEcho) {
        filteredEchoes.find { it.id == selectedEchoId } ?: deepLinkEcho?.takeIf { it.id == selectedEchoId }
    }

    var cachedEcho by remember { mutableStateOf<EchoDto?>(null) }
    if (selectedEcho != null) cachedEcho = selectedEcho

    var echoToReport by remember { mutableStateOf<EchoDto?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    val sessionColor by animateColorAsState(
        targetValue = when(sessionFrequency) {
            0 -> Color(0xFFFF453A)
            1 -> Color(0xFFFF9F0A)
            2 -> Color(0xFF94A3B8)
            3 -> Color(0xFF64D2FF)
            4 -> Color(0xFF34C759)
            else -> Color(0xFF94A3B8)
        },
        animationSpec = tween(1000), label = "sessionColor"
    )

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { viewModel.onRealtimeNotificationReceived() }
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

    BackHandler(enabled = isPanelOpen || showWhisper || showAbout || selectedEchoId != null || echoToReport != null || showGetStarted || showFrequencyModal || showJourneyOverlay) {
        when {
            showJourneyOverlay -> viewModel.closeJourneyOverlay()
            showFrequencyModal -> viewModel.dismissFrequencyModal()
            echoToReport != null -> echoToReport = null
            selectedEchoId != null -> { selectedEchoId = null; viewModel.loadFeed() }
            showWhisper -> showWhisper = false
            showAbout -> showAbout = false
            isPanelOpen -> { viewModel.closeNotificationPanel(); viewModel.loadFeed() }
        }
    }

    val isHomeCovered = isPanelOpen || showAbout || showWhisper || selectedEchoId != null || (echoToReport != null && selectedEchoId == null) || showGetStarted || showFrequencyModal || showJourneyOverlay
    val homeBlur by animateDpAsState(targetValue = if (isHomeCovered) 8.dp else 0.dp, label = "homeBlur", animationSpec = tween(400))
    val dimAlpha by animateFloatAsState(targetValue = if (isHomeCovered) 0.3f else 0f, label = "dimAlpha", animationSpec = tween(400))

    val density = LocalDensity.current
    val refreshTriggerPx = with(density) { 80.dp.toPx() }
    val maxDragPx = with(density) { 150.dp.toPx() }
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

                val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

                if (source == NestedScrollSource.Drag && available.y > 0f && isAtTop) {
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
        if (!isRefreshing) {
            excludedForYou = excludedForYou + pendingViewedForYou
            pendingViewedForYou = emptySet()
            if (pullOffsetY > 0f) snapTo(0f)
        }
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

                // Aesthetic Gradient Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 0.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // --- ELEGANT LEFT-ALIGNED GREETING ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                        .height(72.dp)
                        .zIndex(2f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Crossfade(
                        targetState = showMoodMessage,
                        animationSpec = tween(durationMillis = 1200),
                        label = "GreetingFade"
                    ) { isShowingMood ->
                        if (isShowingMood) {
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                                Text(
                                    text = "Frequency set to",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 20.sp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = sessionColor)) { append(currentMoodName) }
                                        if (currentMoodName != "DISTURBED") {
                                            withStyle(style = SpanStyle(color = NeonWhite)) { append(" for this session.") }
                                        }
                                    },
                                    style = TextStyle(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                )
                            }
                        } else {
                            AnimatedContent(
                                targetState = displayMessage,
                                transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart,
                                label = "HookFade"
                            ) { currentMessage ->
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp)) {
                                    Text(
                                        text = currentMessage.first.toString(),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 20.sp,
                                            color = primaryTextColor,
                                            fontWeight = FontWeight.Normal
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentMessage.second.toString(),
                                        style = TextStyle(
                                            fontFamily = FontFamily.Serif,
                                            fontSize = 28.sp,
                                            color = secondaryTextColor,
                                            fontWeight = FontWeight.Light,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Spaces Filter
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).zIndex(2f)
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

                        val animatingEchoes = if (isOffline) emptyList() else {
                            if (currentCategory == "All") echoes else echoes.filter { it.category == currentCategory }
                        }

                        val echoesForYou = remember(animatingEchoes, sessionFrequency, excludedForYou) {
                            val eligible = animatingEchoes.filter {
                                it.replies.isNullOrEmpty() && !excludedForYou.contains(it.id)
                            }
                            eligible.sortedBy { abs((it.mood ?: 2) - sessionFrequency) }.take(5)
                        }

                        val echoesForYouIds = echoesForYou.mapNotNull { it.id }.toSet()

                        val mainFeedEchoes = animatingEchoes.filter { it.id !in echoesForYouIds }

                        val now = Instant.now()
                        val sixHoursAgo = now.minus(6, ChronoUnit.HOURS)

                        val recentEchoes = mainFeedEchoes.filter {
                            try { Instant.parse(it.createdAt).isAfter(sixHoursAgo) } catch (e: Exception) { false }
                        }
                        val olderEchoes = mainFeedEchoes.filter {
                            try { Instant.parse(it.createdAt).isBefore(sixHoursAgo) } catch (e: Exception) { true }
                        }

                        LazyVerticalGrid(
                            state = listState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(span = { GridItemSpan(2) }) { SpaceHeaderCard(space = currentCategory) }

                            if (isOffline) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "YOU ARE NOT CONNECTED.",
                                                style = VoidTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Stone400, letterSpacing = 2.sp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Waiting for a signal...",
                                                style = VoidTypography.bodyLarge.copy(fontSize = 12.sp, color = Stone400)
                                            )
                                        }
                                    }
                                }
                            } else if (animatingEchoes.isEmpty() && !isRefreshing) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "THIS SPACE IS QUIET.",
                                                style = VoidTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Stone400, letterSpacing = 2.sp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Be the first to Echo.",
                                                style = VoidTypography.bodyLarge.copy(fontSize = 12.sp, color = Stone400)
                                            )
                                        }
                                    }
                                }
                            }
                            else {
                                if (echoesForYou.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "ECHOES FOR YOU",
                                                style = VoidTypography.labelSmall.copy(color = NeonWhite, fontSize = 10.sp, letterSpacing = 4.sp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(modifier = Modifier.width(24.dp).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                                        }
                                    }

                                    item(span = { GridItemSpan(2) }) {
                                        val pagerState = rememberPagerState(pageCount = { echoesForYou.size })
                                        val pagerScope = rememberCoroutineScope()

                                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                HorizontalPager(
                                                    state = pagerState,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 0.dp),
                                                    pageSpacing = 12.dp
                                                ) { page ->
                                                    val echo = echoesForYou[page]

                                                    EchoBannerCard(
                                                        echoId = echo.id ?: "", content = echo.content, createdAt = echo.createdAt, updatedAt = echo.updatedAt, colorCode = echo.colorCode, signatureId = echo.signatureId, category = echo.category, replies = echo.replies,
                                                        onReport = { echoToReport = echo },
                                                        onEdit = { editingEchoId = echo.id; editingInitialText = echo.content; showWhisper = true },
                                                        onClick = {
                                                            echo.id?.let { pendingViewedForYou = pendingViewedForYou + it }
                                                            selectedEchoId = echo.id
                                                        },
                                                        onRelate = { echo.id?.let { viewModel.relateToEcho(it) } },
                                                        onUnrelate = { echo.id?.let { viewModel.unrelateFromEcho(it) } }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(max(0, pagerState.currentPage - 1)) } },
                                                    enabled = pagerState.currentPage > 0,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Prev", tint = if (pagerState.currentPage > 0) Stone400 else Color.Transparent)
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                                    repeat(echoesForYou.size) { index ->
                                                        val isSelected = pagerState.currentPage == index
                                                        val dotWidth by animateDpAsState(if (isSelected) 16.dp else 6.dp, label = "dotWidth")
                                                        val dotColor by animateColorAsState(if (isSelected) NeonWhite else Color.White.copy(0.3f), label = "dotColor")
                                                        Box(modifier = Modifier.height(6.dp).width(dotWidth).clip(CircleShape).background(dotColor))
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { pagerScope.launch { pagerState.animateScrollToPage(min(echoesForYou.size - 1, pagerState.currentPage + 1)) } },
                                                    enabled = pagerState.currentPage < echoesForYou.size - 1,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = if (pagerState.currentPage < echoesForYou.size - 1) Stone400 else Color.Transparent)
                                                }
                                            }
                                        }
                                    }
                                }

                                // ✅ REMOVED ANIMATED VISIBILITY WRAPPER FROM ALL FEED CARDS
                                if (recentEchoes.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "RECENT",
                                                style = VoidTypography.labelSmall.copy(color = NeonWhite, fontSize = 10.sp, letterSpacing = 4.sp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(modifier = Modifier.width(24.dp).height(1.dp).background(Color.White.copy(alpha = 0.3f)))
                                        }
                                    }
                                    items(items = recentEchoes, key = { it.id ?: "error_${it.hashCode()}" }) { echo ->
                                        EchoCard(
                                            echoId = echo.id ?: "", content = echo.content, createdAt = echo.createdAt, updatedAt = echo.updatedAt, colorCode = echo.colorCode, signatureId = echo.signatureId, category = echo.category, replies = echo.replies,
                                            onReport = { echoToReport = echo },
                                            onEdit = { editingEchoId = echo.id; editingInitialText = echo.content; showWhisper = true },
                                            onClick = { selectedEchoId = echo.id },
                                            onRelate = { echo.id?.let { viewModel.relateToEcho(it) } },
                                            onUnrelate = { echo.id?.let { viewModel.unrelateFromEcho(it) } }
                                        )
                                    }
                                }

                                if (olderEchoes.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "OLDER",
                                                style = VoidTypography.labelSmall.copy(color = Stone400, fontSize = 10.sp, letterSpacing = 4.sp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(modifier = Modifier.width(24.dp).height(1.dp).background(Color(0xFF78716C)))
                                        }
                                    }
                                    items(items = olderEchoes, key = { it.id ?: "error_${it.hashCode()}" }) { echo ->
                                        EchoCard(
                                            echoId = echo.id ?: "", content = echo.content, createdAt = echo.createdAt, updatedAt = echo.updatedAt, colorCode = echo.colorCode, signatureId = echo.signatureId, category = echo.category, replies = echo.replies,
                                            onReport = { echoToReport = echo },
                                            onEdit = { editingEchoId = echo.id; editingInitialText = echo.content; showWhisper = true },
                                            onClick = { selectedEchoId = echo.id },
                                            onRelate = { echo.id?.let { viewModel.relateToEcho(it) } },
                                            onUnrelate = { echo.id?.let { viewModel.unrelateFromEcho(it) } }
                                        )
                                    }
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
                    .height(180.dp)
                    .zIndex(1.5f)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, VoidBlack.copy(alpha = 0.95f))))
            )

            // ✅ DASHBOARD ANIMATED COLORS
            val dialBgColor by animateColorAsState(
                targetValue = if (isOffline) Stone900 else Color.White,
                animationSpec = tween(800),
                label = "dialBgColor"
            )
            val leftDialContentColor by animateColorAsState(
                targetValue = if (isOffline) Stone400 else sessionColor,
                animationSpec = tween(800),
                label = "leftDialContentColor"
            )
            val dialIconColor by animateColorAsState(
                targetValue = if (isOffline) Stone400 else Color.Black,
                animationSpec = tween(800),
                label = "dialIconColor"
            )

            // DASHBOARD
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                // 1. LEFT DIAL: Frequency
                Box(
                    modifier = Modifier
                        .offset(x = (-64).dp, y = 16.dp)
                        .size(52.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .background(color = dialBgColor, shape = CircleShape)
                        .clickable(enabled = !isOffline) { // ✅ DISABLED OFFLINE
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openFrequencyModal()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        val strokeWidth = 3.dp.toPx()
                        val gap = size.width / 4
                        val heights = listOf(0.4f, 0.8f, 1.0f, 0.6f, 0.4f)
                        heights.forEachIndexed { index, heightMultiplier ->
                            val x = index * gap
                            val barHeight = size.height * heightMultiplier
                            val shrinkAmount = (size.height - barHeight) / 2
                            drawLine(color = leftDialContentColor, start = Offset(x, shrinkAmount), end = Offset(x, size.height - shrinkAmount), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                        }
                    }
                }

                // 2. RIGHT DIAL: Journey Analytics
                Box(
                    modifier = Modifier
                        .offset(x = 64.dp, y = 16.dp)
                        .size(52.dp)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .background(color = dialBgColor, shape = CircleShape)
                        .clickable(enabled = !isOffline) { // ✅ DISABLED OFFLINE
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleJourneyOverlay()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = "Journey", tint = dialIconColor, modifier = Modifier.size(26.dp))
                }

                // 3. CENTER DIAL: The Whisper Button
                Box(
                    modifier = Modifier
                        .offset(y = (-12).dp)
                        .size(72.dp)
                        .shadow(elevation = 20.dp, shape = CircleShape)
                        .background(color = dialBgColor, shape = CircleShape)
                        .clickable(enabled = !isOffline) { // ✅ DISABLED OFFLINE
                            editingEchoId = null
                            editingInitialText = ""
                            showWhisper = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Whisper", tint = dialIconColor, modifier = Modifier.size(36.dp))
                }
            }

            if (dimAlpha > 0f) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)).zIndex(3f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { })
            }
        }

        // --- OVERLAYS ---
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).zIndex(100f)) { data ->
            Snackbar(containerColor = NeonWhite, contentColor = VoidBlack, actionColor = Color(0xFFFF453A), snackbarData = data, shape = RoundedCornerShape(16.dp))
        }

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
                WhisperOverlay(
                    onDismiss = { showWhisper = false },
                    initialText = editingInitialText,
                    onSend = { t, cat, col ->
                        if (isOffline) {
                            scope.launch { snackbarHostState.showSnackbar("Saved offline. Will post when connected.") }
                        }
                        if (editingEchoId != null) viewModel.updateEcho(editingEchoId!!, t) else viewModel.postEcho(t, cat, col)
                        showWhisper = false
                    }
                )
            }
        }

        GlassScrim(visible = selectedEchoId != null, onDismiss = { selectedEchoId = null; viewModel.loadFeed() })
        AnimatedVisibility(visible = selectedEchoId != null, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut(targetScale = 0.9f), modifier = Modifier.fillMaxSize().zIndex(6f)) {
            Box(modifier = Modifier.fillMaxSize()) {
                cachedEcho?.let { echo ->
                    DetailOverlay(
                        echo = echo,
                        onDismiss = { selectedEchoId = null; viewModel.loadFeed() },
                        onReply = { r ->
                            if (isOffline) {
                                scope.launch { snackbarHostState.showSnackbar("Reply saved offline. Will post when connected.") }
                            }
                            echo.id?.let { viewModel.postReply(it, r) }
                        },
                        onUpdateReply = { id, c -> viewModel.updateReply(id, c) },
                        onEditEcho = { selectedEchoId = null; editingEchoId = echo.id; editingInitialText = echo.content; showWhisper = true },
                        onReport = { echoToReport = echo },
                        onRelate = { echo.id?.let { viewModel.relateToEcho(it) } },
                        onUnrelate = { echo.id?.let { viewModel.unrelateFromEcho(it) } }
                    )
                }
            }
        }

        AnimatedVisibility(visible = showJourneyOverlay, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = Modifier.zIndex(4f)) {
            Box(
                modifier = Modifier.fillMaxSize().clickable(
                    remember { MutableInteractionSource() },
                    null
                ) { viewModel.closeJourneyOverlay() })
        }
        AnimatedVisibility(
            visible = showJourneyOverlay,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(5f)
        ) {
            JourneyOverlay(
                stats = viewModel.userStats.collectAsState().value,
                allEchoes = echoes,
                dominantMood = viewModel.dominantMood.collectAsState().value,
                activityStreak = viewModel.activityStreak.collectAsState().value,
                userName = viewModel.displayName.collectAsState().value,
                userSignatureId = viewModel.getUserId(),
                onUpdateName = { newName -> viewModel.updateDisplayName(newName) },
                onRestoreIdentity = { newId -> viewModel.restoreIdentity(newId) },
                onDismiss = { viewModel.closeJourneyOverlay() },
                onApplyEchoPlus = { email -> viewModel.submitEchoPlusApplication(email) }
            )
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
        Box(modifier = Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { onDismiss() })
    }
}