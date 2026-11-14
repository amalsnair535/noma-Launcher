package com.freelauncher.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.IconPackManager
import com.freelauncher.app.data.service.InstalledAppMatcher
import com.freelauncher.app.data.service.ScribbleRecognizer
import com.freelauncher.app.ui.components.ClockStyle
import com.freelauncher.app.ui.components.MinimalistClock
import com.freelauncher.app.ui.components.ScribbleSearchOverlay
import com.freelauncher.app.ui.components.getContextualGreeting
import com.freelauncher.app.ui.util.DateTimeUtils
import com.freelauncher.app.ui.util.GesturalAction
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.ScribbleGestureCoordinator
import com.freelauncher.app.ui.util.ScribblePoint
import com.freelauncher.app.ui.util.ScribbleStroke
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    currentTimeFlow: kotlinx.coroutines.flow.StateFlow<Date>,
    onNavigate: (LauncherScreen) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onLaunchApp: (AppItem) -> Unit = {},
    onLongPressApp: (AppItem) -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSearchWithQuery: (String) -> Unit = {},
    onOpenDialer: () -> Unit = {},
    onOpenClock: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onDoubleTapToSleep: () -> Unit = {},
    onClockStyleChanged: (ClockStyle) -> Unit = {},
    onTimeCardOffsetChanged: (Float, Float) -> Unit = { _, _ -> },
    onResetTimeCardOffset: () -> Unit = {},
    onTimeCardScaleChanged: (Float) -> Unit = {},
    onClockEditModeToggled: (Boolean) -> Unit = {},
    onToggleUltraFocus: () -> Unit = {},
    iconPackManager: IconPackManager? = null
) {
    BackHandler(enabled = true) {
        if (state.isClockEditMode) {
            onClockEditModeToggled(false)
        }
        // Intercept back gesture / press on Home Screen: no navigation, no animation, no reload
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // On-Device ML Kit Scribble Recognizer Setup
    val scribbleRecognizer = remember(context) { ScribbleRecognizer(context) }
    DisposableEffect(scribbleRecognizer) {
        onDispose { scribbleRecognizer.close() }
    }

    // Live Scribble Search State
    var strokes by remember { mutableStateOf<List<ScribbleStroke>>(emptyList()) }
    var currentStrokePoints by remember { mutableStateOf<List<ScribblePoint>>(emptyList()) }
    var recognizedText by remember { mutableStateOf("") }
    var matchingScribbleApps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionJob by remember { mutableStateOf<Job?>(null) }
    var lastScribbleTime by remember { mutableLongStateOf(0L) }

    val currentStroke = if (currentStrokePoints.isNotEmpty()) ScribbleStroke(currentStrokePoints) else null

    val dateText by produceState(
        initialValue = DateTimeUtils.format(Calendar.getInstance().time, DateTimeUtils.Patterns.DATE_EEEE_MMMM_D),
        currentTimeFlow
    ) {
        currentTimeFlow.collect { time ->
            value = DateTimeUtils.format(time, DateTimeUtils.Patterns.DATE_EEEE_MMMM_D)
        }
    }
    
    val greetingText by produceState(
        initialValue = if (state.customGreeting == "auto") getContextualGreeting(Calendar.getInstance()) else state.customGreeting,
        currentTimeFlow, state.customGreeting
    ) {
        if (state.customGreeting == "auto") {
            currentTimeFlow.collect { timeVal ->
                value = getContextualGreeting(Calendar.getInstance().apply { time = timeVal })
            }
        } else {
            value = state.customGreeting
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Realtime draggable coordinates for the Time Card
    var localOffsetX by remember(state.timeCardOffsetX) { mutableFloatStateOf(state.timeCardOffsetX) }
    var localOffsetY by remember(state.timeCardOffsetY) { mutableFloatStateOf(state.timeCardOffsetY) }
    var isDraggingCard by remember { mutableStateOf(value = false) }

    // Clock swipe tracking in Edit Mode (Style carousel)
    var clockSwipeAccumulator by remember { mutableFloatStateOf(0f) }
    val allClockStyles = remember { ClockStyle.entries.toList() }
    val currentStyleIndex = allClockStyles.indexOf(state.clockStyle).coerceAtLeast(0)
    val latestStyleIndex by rememberUpdatedState(currentStyleIndex)

    val isEditMode = state.isClockEditMode
    val rawClockAnimScale by animateFloatAsState(
        targetValue = state.timeCardScale * (if (isDraggingCard) 1.06f else if (isEditMode) 1.02f else 1f),
        animationSpec = spring(),
        label = "clock_edit_scale",
    )
    val clockAnimScale = if (isLandscape) rawClockAnimScale.coerceAtMost(0.85f) else rawClockAnimScale

    // Active edit tab state in edit mode (0: Style, 1: Resize)
    var editTab by remember { mutableIntStateOf(0) }

    // Request high refresh rate for smooth transitions when this screen is active
    val currentView = LocalView.current
    SideEffect {
        if (android.os.Build.VERSION.SDK_INT >= 35) { // Android 15+
            try {
                currentView.requestedFrameRate = 120f
            } catch (_: Exception) { /* Fallback */ }
        }
    }

    // Multi-tap tracking for double tap to sleep and triple tap UltraFocus
    var rootTapCount by remember { mutableIntStateOf(0) }
    var lastRootTapTime by remember { mutableLongStateOf(0L) }

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width.toFloat()

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isEditMode) {
                        onClockEditModeToggled(false)
                    } else {
                        val now = System.currentTimeMillis()
                        val isDrawingActive = (now - lastScribbleTime) < 700L
                        if (!isDrawingActive) {
                            if (now - lastRootTapTime < 450) {
                                rootTapCount++
                            } else {
                                rootTapCount = 1
                            }
                            lastRootTapTime = now

                            if (rootTapCount == 2) {
                                if (state.enableDoubleTapToSleep) {
                                    LauncherHaptics.playClick(context)
                                    onDoubleTapToSleep()
                                }
                            } else if (rootTapCount >= 3) {
                                rootTapCount = 0
                                LauncherHaptics.playClick(context)
                                onToggleUltraFocus()
                            }
                        }
                    }
                },
                onLongClick = {
                    val now = System.currentTimeMillis()
                    val isDrawingOrRecent = currentStrokePoints.isNotEmpty() || strokes.isNotEmpty() || (now - lastScribbleTime < 800L)
                    if (!isEditMode && !isDrawingOrRecent) {
                        onOpenSettings()
                    }
                }
            )
            .pointerInput(isEditMode, screenWidthPx, state.installedApps, state.enableScribbleSearch) {
                if (!isEditMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val strokePoints = mutableListOf<ScribblePoint>()
                            strokePoints.add(ScribblePoint(down.position.x, down.position.y))
                            if (state.enableScribbleSearch) {
                                currentStrokePoints = strokePoints.toList()
                            }

                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.find { it.id == pointerId } ?: break

                                if (dragChange.pressed) {
                                    val dx = dragChange.position.x - down.position.x
                                    val dy = dragChange.position.y - down.position.y
                                    if (kotlin.math.hypot(dx, dy) > 12f) {
                                        lastScribbleTime = System.currentTimeMillis()
                                        strokePoints.add(ScribblePoint(dragChange.position.x, dragChange.position.y))
                                        if (state.enableScribbleSearch) {
                                            currentStrokePoints = strokePoints.toList()
                                        }
                                    }
                                } else {
                                    break
                                }
                            }

                            val completedStrokePoints = strokePoints.toList()
                            currentStrokePoints = emptyList()

                            if (completedStrokePoints.size > 1) {
                                val gesturalAction = ScribbleGestureCoordinator.analyzeStroke(
                                    points = completedStrokePoints,
                                    screenWidthPx = screenWidthPx
                                )

                                when (gesturalAction) {
                                    GesturalAction.SWIPE_UP -> onNavigate(LauncherScreen.SIX_APPS)
                                    GesturalAction.SWIPE_DOWN -> if (state.enableSwipeDownSearch) onOpenSearch()
                                    GesturalAction.SWIPE_LEFT -> if (state.showNewsFeed) onNavigate(LauncherScreen.RSS_FEED)
                                    GesturalAction.SWIPE_RIGHT -> if (state.showTimeAway) onNavigate(LauncherScreen.TIME_AWAY)
                                    GesturalAction.HANDWRITING_STROKE -> {
                                        if (state.enableScribbleSearch) {
                                            lastScribbleTime = System.currentTimeMillis()
                                            val newStrokes = strokes + ScribbleStroke(completedStrokePoints)
                                            strokes = newStrokes

                                            recognitionJob?.cancel()
                                            recognitionJob = scope.launch {
                                                isRecognizing = true
                                                delay(180.milliseconds)
                                                val candidates = scribbleRecognizer.recognizeStrokes(newStrokes)
                                                isRecognizing = false
                                                if (candidates.isNotEmpty()) {
                                                    val rawCandidate = candidates.first().trim()
                                                    val resolvedCandidate = when {
                                                        rawCandidate.equals("i", ignoreCase = true) || rawCandidate == "1" || rawCandidate == "|" -> {
                                                            if (candidates.any { it.equals("l", ignoreCase = true) } && !candidates.any { it.equals("i", ignoreCase = true) }) "L" else "I"
                                                        }
                                                        rawCandidate.equals("l", ignoreCase = true) -> "L"
                                                        else -> rawCandidate
                                                    }
                                                    if (resolvedCandidate.isNotBlank()) {
                                                        strokes = emptyList()
                                                        currentStrokePoints = emptyList()
                                                        recognizedText = ""
                                                        matchingScribbleApps = emptyList()
                                                        onOpenSearchWithQuery(resolvedCandidate)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    GesturalAction.NONE -> { /* Quick touch or tap */ }
                                }
                            }
                        }
                    }
                }
            }
            .testTag("home_screen_root")
    ) {
        // Edge gesture hints when not editing (Only shown if gesture hints setting is enabled)
        if (!isEditMode && state.showGestureHints) {
            // Left hint: Time Away Screen
            if (state.showTimeAway) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                )
            }
            // Right hint: News Feed
            if (state.showNewsFeed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                )
            }
        }

        // Top Edit Mode Header Badge & Drag Indicator
        if (isEditMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .testTag("clock_edit_mode_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "DRAG TO MOVE • CUSTOMIZE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Main Time Card Container with Free Movement / Drag Offset & Scaling
        if (!state.hideEverythingOnHome || isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(localOffsetX.roundToInt(), localOffsetY.roundToInt())
                    }
                    .pointerInput(isEditMode) {
                        if (isEditMode) {
                            detectDragGestures(
                                onDragStart = {
                                    isDraggingCard = true
                                },
                                onDragEnd = {
                                    isDraggingCard = false
                                    onTimeCardOffsetChanged(localOffsetX, localOffsetY)
                                },
                                onDragCancel = {
                                    isDraggingCard = false
                                    onTimeCardOffsetChanged(localOffsetX, localOffsetY)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                localOffsetX += dragAmount.x
                                localOffsetY += dragAmount.y
                            }
                        }
                    }
                    .testTag("time_card_moveable_container")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 12.dp)
                ) {
                    // Interactive Clock Card
                    Box(
                        modifier = Modifier
                            .scale(clockAnimScale)
                            .clip(RoundedCornerShape(28.dp))
                            .then(
                                if (isEditMode) {
                                    Modifier
                                        .background(
                                            if (isDraggingCard)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                        )
                                        .border(
                                            if (isDraggingCard) 2.5.dp else 1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(28.dp)
                                        )
                                        .padding(vertical = 16.dp, horizontal = 18.dp)
                                } else {
                                    Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                                }
                            )
                            .pointerInput(isEditMode) {
                                if (isEditMode) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            clockSwipeAccumulator = 0f
                                        },
                                        onDragEnd = {
                                            val swipeThreshold = 50f
                                            if (clockSwipeAccumulator < -swipeThreshold) {
                                                val nextIndex = (latestStyleIndex + 1) % allClockStyles.size
                                                val targetStyle = allClockStyles[nextIndex]
                                                onClockStyleChanged(targetStyle)
                                            } else if (clockSwipeAccumulator > swipeThreshold) {
                                                val prevIndex = if ((latestStyleIndex - 1) < 0) allClockStyles.size - 1 else latestStyleIndex - 1
                                                val targetStyle = allClockStyles[prevIndex]
                                                onClockStyleChanged(targetStyle)
                                            }
                                            clockSwipeAccumulator = 0f
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        clockSwipeAccumulator += dragAmount
                                    }
                                }
                            }
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isEditMode) {
                                        onOpenClock()
                                    }
                                },
                                onLongClick = {
                                    onClockEditModeToggled(true)
                                }
                            )
                            .testTag("minimalist_clock_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MinimalistClock(
                                timeFlow = currentTimeFlow,
                                clockStyle = state.clockStyle,
                                modifier = Modifier.testTag("minimalist_clock")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(clockAnimScale.coerceAtMost(1.2f))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (!isEditMode) {
                                    onOpenCalendar()
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("home_date_text")
                    )

                    // Text-Only Weather & Battery Glance
                    if (state.showWeatherBatteryGlance) {
                        val batteryStr = if (state.isBatteryCharging) "⚡ ${state.batteryLevel}%" else "${state.batteryLevel}%"
                        val weatherPart = state.weatherText ?: if (state.temperatureUnit == "C") "22°C Clear" else "72°F Clear"
                        val glanceText = "$weatherPart • $batteryStr"

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = glanceText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .scale(clockAnimScale.coerceAtMost(1.15f))
                                .testTag("home_weather_battery_glance")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Action Icons Row: Search Lens & Dialer Icon
                    if (!isEditMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .scale(clockAnimScale.coerceAtMost(1.1f))
                                .padding(vertical = 4.dp)
                        ) {
                            // Search Lens Icon Button
                            Surface(
                                onClick = onOpenSearch,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.size(38.dp).testTag("home_search_lens_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Lens",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            // Dialer Icon Button
                            Surface(
                                onClick = onOpenDialer,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.size(38.dp).testTag("home_dialer_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Call,
                                        contentDescription = "Open Dialer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Contextual Greeting
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(clockAnimScale.coerceAtMost(1.15f))
                            .testTag("contextual_greeting_text")
                    )

                    // Phone-Free Today Time Indicator
                    if (state.showTimeAway && state.hasUsagePermission && state.timeAwayStats != null) {
                        val todayMins = state.timeAwayStats.todayPhoneFreeMinutes
                        val hrs = todayMins / 60
                        val mins = todayMins % 60
                        val timeFormatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$timeFormatted phone-free today",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .scale(clockAnimScale.coerceAtMost(1.1f))
                                .testTag("home_phone_free_today_text")
                        )
                    }
                }
            }
        }

        // Clock & Time Card Edit Overlay Panel at Bottom
        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("time_card_editor_panel"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = editTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = editTab == 0,
                            onClick = { editTab = 0 },
                            text = { Text("Style", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                        )
                        Tab(
                            selected = editTab == 1,
                            onClick = { editTab = 1 },
                            text = { Text("Resize", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                        )
                    }

                    when (editTab) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val prevIndex = if ((latestStyleIndex - 1) < 0) allClockStyles.size - 1 else latestStyleIndex - 1
                                            onClockStyleChanged(allClockStyles[prevIndex])
                                        }
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Style")
                                    }
                                    Text(
                                        text = "${state.clockStyle.displayName} (${latestStyleIndex + 1}/${allClockStyles.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = {
                                            val nextIndex = (latestStyleIndex + 1) % allClockStyles.size
                                            onClockStyleChanged(allClockStyles[nextIndex])
                                        }
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Style")
                                    }
                                }
                                Text(
                                    text = "Swipe horizontally on the clock or use arrows to change style",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val sizePresets = listOf(
                                        "75%" to 0.75f,
                                        "100%" to 1.0f,
                                        "125%" to 1.25f,
                                        "150%" to 1.5f
                                    )
                                    sizePresets.forEach { (label, scale) ->
                                        val isSelected = abs(state.timeCardScale - scale) < 0.05f
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onTimeCardScaleChanged(scale) },
                                            label = { Text(label, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Scale:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Slider(
                                        value = state.timeCardScale,
                                        onValueChange = { onTimeCardScaleChanged(it) },
                                        valueRange = 0.6f..1.6f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(state.timeCardScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(44.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Touch & drag clock to place anywhere",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp
                                    )
                                    TextButton(
                                        onClick = {
                                            localOffsetX = 0f
                                            localOffsetY = 0f
                                            onResetTimeCardOffset()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Center Card", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onClockEditModeToggled(false) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("exit_clock_edit_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply & Save Setup", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 5. Direct Home Screen Scribble Search Live Ink Canvas & Overlay
        if (!isEditMode && state.enableScribbleSearch) {
            ScribbleSearchOverlay(
                strokes = strokes,
                currentStroke = currentStroke,
                recognizedText = recognizedText,
                matchingApps = matchingScribbleApps,
                isRecognizing = isRecognizing,
                onLaunchApp = { app ->
                    strokes = emptyList()
                    currentStrokePoints = emptyList()
                    recognizedText = ""
                    matchingScribbleApps = emptyList()
                    onLaunchApp(app)
                },
                onLongPressApp = { app ->
                    strokes = emptyList()
                    currentStrokePoints = emptyList()
                    recognizedText = ""
                    matchingScribbleApps = emptyList()
                    onLongPressApp(app)
                },
                onClearScribble = {
                    strokes = emptyList()
                    currentStrokePoints = emptyList()
                    recognizedText = ""
                    matchingScribbleApps = emptyList()
                },
                iconPackManager = iconPackManager,
                appDrawerStyle = state.appDrawerStyle,
                showMonograms = state.showMonograms,
                iconDisplayMode = state.iconDisplayMode,
                selectedIconPackPackage = state.selectedIconPackPackage,
                minimalConfig = state.minimalIconPackConfig,
                allAppsAlignment = state.allAppsAlignment,
                sixAppsScale = state.sixAppsScale
            )
        }

        // Bottom gesture hint when not in edit mode (Only shown if gesture hints setting is enabled)
        if (!isEditMode && state.showGestureHints) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Tap for apps • Draw on screen to search • Long press settings",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
