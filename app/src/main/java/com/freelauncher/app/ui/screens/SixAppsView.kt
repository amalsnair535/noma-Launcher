package com.freelauncher.app.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.ui.components.MinimalistClock
import com.freelauncher.app.ui.util.DateTimeUtils
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.abs
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SixAppsView(
    state: LauncherUiState,
    currentTime: Date,
    onLaunchApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPressApp: (AppItem) -> Unit = {},
    onOpenMultiPin: () -> Unit = {},
    onOpenRearrangePinned: () -> Unit = {},
    onClearLockFeedback: () -> Unit = {},
    onToggleUltraFocus: () -> Unit = {},
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
) {
    BackHandler {
        onNavigate(LauncherScreen.HOME)
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useLandscapeLayout = isLandscape
    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width.toFloat()

    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    // Triple tap tracking
    var bgTapCount by remember { mutableIntStateOf(0) }
    var lastBgTapTime by remember { mutableLongStateOf(0L) }

    val isLocked = state.isPinnedOnlyLocked

    val dateText = remember(currentTime) { DateTimeUtils.format(currentTime, DateTimeUtils.Patterns.DATE_EEEE_MMMM_D) }

    // Request high refresh rate for smooth transitions when this screen is active
    val currentView = LocalView.current
    SideEffect {
        if (android.os.Build.VERSION.SDK_INT >= 35) { // Android 15+
            try {
                currentView.requestedFrameRate = 120f
            } catch (_: Exception) { /* Fallback */ }
        }
    }

    // Auto-clear feedback message after 2.5s
    LaunchedEffect(state.pinnedLockFeedbackMessage) {
        if (state.pinnedLockFeedbackMessage != null) {
            delay(2500)
            onClearLockFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
            .pointerInput(isLocked, screenWidthPx) {
                if (!isLocked) {
                    var isStartedInSafeZone = true
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            totalDragY = 0f
                            totalDragX = 0f
                            val gestureEdgeMargin = 32.dp.toPx()
                            isStartedInSafeZone = startOffset.x in gestureEdgeMargin..(screenWidthPx - gestureEdgeMargin)
                        },
                        onDragEnd = {
                            if (!isStartedInSafeZone) return@detectDragGestures
                            val threshold = 60f
                            if (abs(totalDragY) > abs(totalDragX)) {
                                if (totalDragY > threshold) {
                                    // Dragged down -> back to Home
                                    onNavigate(LauncherScreen.HOME)
                                } else if (totalDragY < -threshold) {
                                    // Dragged up -> All Apps & Search
                                    onNavigate(LauncherScreen.ALL_APPS)
                                }
                            } else {
                                if (totalDragX < -threshold && state.showNewsFeed) {
                                    // Dragged left -> RSS Feed
                                    onNavigate(LauncherScreen.RSS_FEED)
                                } else if (totalDragX > threshold && state.showTimeAway) {
                                    // Dragged right -> Time Away
                                    onNavigate(LauncherScreen.TIME_AWAY)
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (isStartedInSafeZone) {
                                change.consume()
                                totalDragY += dragAmount.y
                                totalDragX += dragAmount.x
                            }
                        }
                    )
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastBgTapTime < 450) {
                        bgTapCount++
                    } else {
                        bgTapCount = 1
                    }
                    lastBgTapTime = now

                    if (bgTapCount >= 3) {
                        bgTapCount = 0
                        LauncherHaptics.playClick(context)
                        onToggleUltraFocus()
                    }
                }
            )
            .testTag("six_apps_view_root")
    ) {
        if (useLandscapeLayout) {
            // -------------------------------------------------------------
            // LANDSCAPE SPLIT LAYOUT (Matching Reference Image)
            // -------------------------------------------------------------
            
            // Top Center Return Button (when unlocked and gesture hints enabled)
            if (!isLocked && state.showGestureHints) {
                IconButton(
                    onClick = { onNavigate(LauncherScreen.HOME) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back to Home",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Split Row: Left Clock/Date, Right 2x3 App Grid
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Half: Large Clock & Date
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MinimalistClock(
                            currentTime = currentTime,
                            clockStyle = state.clockStyle
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Right Half: 2-Column x 3-Row Pinned Apps Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    val pinned = state.pinnedApps.take(6)
                    if (pinned.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Your quick access slots are empty.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (!isLocked) {
                                Button(
                                    onClick = onOpenMultiPin,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Apps (0/6)")
                                }
                            }
                        }
                    } else {
                        val rows = pinned.chunked(2)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            rows.forEach { rowApps ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowApps.forEach { app ->
                                        LandscapeAppGridItem(
                                            app = app,
                                            showMonogram = state.showMonograms,
                                            iconDisplayMode = state.iconDisplayMode,
                                            iconPackPackage = state.selectedIconPackPackage,
                                            iconPackManager = iconPackManager,
                                            minimalConfig = state.minimalIconPackConfig,
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = {
                                                if (!isLocked) onOpenRearrangePinned()
                                            },
                                            onTripleTap = {
                                                LauncherHaptics.playClick(context)
                                                onToggleLock()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Center Navigation Chevron (All Apps)
            if (!isLocked && state.showGestureHints) {
                IconButton(
                    onClick = { onNavigate(LauncherScreen.ALL_APPS) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "All Apps",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (isLocked) {
                // UltraFocus lock indicator at bottom center
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        val isTimerActive = state.isFocusTimerRunning && state.focusTimerSecondsLeft > 0
                        val timerText = if (isTimerActive) {
                            val mins = state.focusTimerSecondsLeft / 60
                            val secs = state.focusTimerSecondsLeft % 60
                            String.format(LocalLocale.current.platformLocale, "%02d:%02d", mins, secs)
                        } else ""

                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isTimerActive) Icons.Outlined.SelfImprovement else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (isTimerActive) "ULTRAFOCUS $timerText • TRIPLE-TAP TO EXIT" else "ULTRAFOCUS LOCKED • TRIPLE-TAP TO EXIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp,
                                    fontWeight = if (isTimerActive) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

        } else {
            // -------------------------------------------------------------
            // PORTRAIT LAYOUT
            // -------------------------------------------------------------

            // Top Section: Back Button (when unlocked and gesture hints enabled)
            if (!isLocked && state.showGestureHints) {
                IconButton(
                    onClick = { onNavigate(LauncherScreen.HOME) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .testTag("back_to_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back to Home",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Center Content: Pinned Apps List (1 to 6 apps)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .scale(state.sixAppsScale)
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Clock header fixed at 75% scale (independent of Home Screen clock scale)
                Column(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .wrapContentSize()
                        .scale(0.75f)
                        .testTag("pinned_full_clock"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MinimalistClock(
                        currentTime = currentTime,
                        clockStyle = state.clockStyle
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }

                // App list aligned dynamically within the centered column
                val listHorizontalAlignment = when (state.sixAppsAlignment) {
                    com.freelauncher.app.data.models.SixAppsAlignment.LEFT -> Alignment.Start
                    com.freelauncher.app.data.models.SixAppsAlignment.CENTER -> Alignment.CenterHorizontally
                    com.freelauncher.app.data.models.SixAppsAlignment.RIGHT -> Alignment.End
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = listHorizontalAlignment,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (state.pinnedApps.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Your quick access slots are empty.",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = if (isLocked) "Triple-tap to unlock and select apps." else "Add up to 6 apps for a distraction-free home.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Start
                            )
                            if (!isLocked) {
                                Surface(
                                    onClick = onOpenMultiPin,
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("add_first_app_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Add Apps (0/6)",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val pinned = state.pinnedApps.take(6)
                        pinned.forEachIndexed { index, app ->
                            SixAppRowItem(
                                app = app,
                                showMonogram = state.showMonograms,
                                iconDisplayMode = state.iconDisplayMode,
                                iconPackPackage = state.selectedIconPackPackage,
                                iconPackManager = iconPackManager,
                                minimalConfig = state.minimalIconPackConfig,
                                alignment = state.sixAppsAlignment,
                                isLocked = isLocked,
                                onClick = { onLaunchApp(app) },
                                onLongClick = {
                                    if (!isLocked) {
                                        onOpenRearrangePinned()
                                    }
                                },
                                onTripleTap = {
                                    LauncherHaptics.playClick(context)
                                    onToggleLock()
                                },
                                testTag = "six_app_item_$index"
                            )
                        }
                    }
                }
            }

            // Bottom Section: Swipe affordance when unlocked OR Discreet Unlock Hint when locked
            if (!isLocked && state.showGestureHints) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onNavigate(LauncherScreen.ALL_APPS) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "All Apps",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else if (isLocked) {
                // Subtle unlock hint at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                    ) {
                        val isTimerActive = state.isFocusTimerRunning && state.focusTimerSecondsLeft > 0
                        val timerText = if (isTimerActive) {
                            val mins = state.focusTimerSecondsLeft / 60
                            val secs = state.focusTimerSecondsLeft % 60
                            String.format(LocalLocale.current.platformLocale, "%02d:%02d", mins, secs)
                        } else ""

                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isTimerActive) Icons.Outlined.SelfImprovement else Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = if (isTimerActive) "ULTRAFOCUS $timerText • TRIPLE-TAP TO EXIT" else "ULTRAFOCUS LOCKED • TRIPLE-TAP TO EXIT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp,
                                    fontWeight = if (isTimerActive) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Animated Toast / Feedback Banner when mode changes
        AnimatedVisibility(
            visible = state.pinnedLockFeedbackMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLocked) 90.dp else 40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.pinnedLockFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

/**
 * App Item in Landscape 2x3 Grid (matching reference design).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandscapeAppGridItem(
    app: AppItem,
    showMonogram: Boolean,
    iconDisplayMode: com.freelauncher.app.data.models.IconDisplayMode = com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY,
    iconPackPackage: String = "",
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
    minimalConfig: com.freelauncher.app.data.models.MinimalIconPackConfig = com.freelauncher.app.data.models.MinimalIconPackConfig(),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTripleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemTapCount by remember { mutableIntStateOf(0) }
    var itemLastTapTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .widthIn(min = 180.dp, max = 320.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - itemLastTapTime < 450) {
                        itemTapCount++
                    } else {
                        itemTapCount = 1
                    }
                    itemLastTapTime = now

                    if (itemTapCount >= 3) {
                        itemTapCount = 0
                        onTripleTap()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val shouldShowIcon = iconDisplayMode != com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY
        if (shouldShowIcon) {
            com.freelauncher.app.ui.components.AppIcon(
                app = app,
                mode = iconDisplayMode,
                iconPackPackage = iconPackPackage,
                iconPackManager = iconPackManager,
                minimalConfig = minimalConfig,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SixAppRowItem(
    app: AppItem,
    showMonogram: Boolean,
    iconDisplayMode: com.freelauncher.app.data.models.IconDisplayMode = com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY,
    iconPackPackage: String = "",
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
    minimalConfig: com.freelauncher.app.data.models.MinimalIconPackConfig = com.freelauncher.app.data.models.MinimalIconPackConfig(),
    alignment: com.freelauncher.app.data.models.SixAppsAlignment = com.freelauncher.app.data.models.SixAppsAlignment.LEFT,
    isLocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTripleTap: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var itemTapCount by remember { mutableIntStateOf(0) }
    var itemLastTapTime by remember { mutableLongStateOf(0L) }

    val rowArrangement = when (alignment) {
        com.freelauncher.app.data.models.SixAppsAlignment.LEFT -> Arrangement.Start
        com.freelauncher.app.data.models.SixAppsAlignment.CENTER -> Arrangement.Center
        com.freelauncher.app.data.models.SixAppsAlignment.RIGHT -> Arrangement.End
    }
    val textAlignment = when (alignment) {
        com.freelauncher.app.data.models.SixAppsAlignment.LEFT -> TextAlign.Start
        com.freelauncher.app.data.models.SixAppsAlignment.CENTER -> TextAlign.Center
        com.freelauncher.app.data.models.SixAppsAlignment.RIGHT -> TextAlign.End
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - itemLastTapTime < 450) {
                        itemTapCount++
                    } else {
                        itemTapCount = 1
                    }
                    itemLastTapTime = now

                    if (itemTapCount >= 3) {
                        itemTapCount = 0
                        onTripleTap()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = rowArrangement
    ) {
        val shouldShowIcon = iconDisplayMode != com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY
        if (shouldShowIcon) {
            com.freelauncher.app.ui.components.AppIcon(
                app = app,
                mode = iconDisplayMode,
                iconPackPackage = iconPackPackage,
                iconPackManager = iconPackManager,
                minimalConfig = minimalConfig,
                size = 38.dp
            )
            Spacer(modifier = Modifier.width(18.dp))
        }

        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.3.sp,
                textAlign = textAlignment
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
