package com.freelauncher.app.ui.screens

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.UniversalSearchManager
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.TrackScrollHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

import kotlin.time.Duration.Companion.milliseconds

private fun Modifier.verticalFadingEdge(
    topFadeHeight: Dp = 48.dp,
    bottomFadeHeight: Dp = 64.dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val topFadePx = topFadeHeight.toPx()
        val bottomFadePx = bottomFadeHeight.toPx()
        val height = size.height

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstIn
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = height - bottomFadePx,
                    endY = height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeoMinimalAppDrawer(
    state: LauncherUiState,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val effectiveIconPackManager = remember(context, iconPackManager) {
        iconPackManager ?: com.freelauncher.app.data.service.IconPackManager(context)
    }

    var isSearchFocused by remember(state.isSearchOnlyMode) { mutableStateOf(state.isSearchOnlyMode) }
    val isSearching = state.searchQuery.isNotBlank() || isSearchFocused || state.isSearchOnlyMode

    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }

    LaunchedEffect(state.isSearchOnlyMode, state.isScribbleSearchMode, state.currentScreen) {
        val shouldShowKeyboard = state.currentScreen == LauncherScreen.ALL_APPS && state.isSearchOnlyMode && !state.isScribbleSearchMode
        if (shouldShowKeyboard) {
            isSearchFocused = true
            focusRequester.requestFocus()
            delay(120.milliseconds)
            if (state.currentScreen == LauncherScreen.ALL_APPS && state.isSearchOnlyMode && !state.isScribbleSearchMode) {
                keyboardController?.show()
            }
        } else {
            isSearchFocused = false
            keyboardController?.hide()
        }
    }

    BackHandler {
        if (isSearchFocused || state.searchQuery.isNotBlank() || state.isSearchOnlyMode) {
            onSearchQueryChange("")
            isSearchFocused = false
            keyboardController?.hide()
            onNavigate(LauncherScreen.HOME)
        } else {
            onNavigate(LauncherScreen.HOME)
        }
    }

    // Visible Categories for Horizontal Swiping
    val visibleCategories = remember(state.categories) {
        state.categories.filter { !it.isHidden }
    }
    val categoryIds = remember(visibleCategories) {
        listOf<String?>(null) + visibleCategories.map { it.id }
    }

    val pagerState = rememberPagerState(pageCount = { categoryIds.size })

    // Universal Search Results
    val matchingApps = remember(state.installedApps, state.searchQuery, state.isScribbleSearchMode) {
        if (state.searchQuery.isNotBlank()) {
            com.freelauncher.app.data.service.InstalledAppMatcher.matchApps(
                installedApps = state.installedApps,
                query = state.searchQuery,
                limit = 100,
                isScribbleSearch = state.isScribbleSearchMode
            )
        } else {
            emptyList()
        }
    }

    val matchingSettings = remember(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            UniversalSearchManager.searchSettings(state.searchQuery)
        } else {
            emptyList()
        }
    }

    val scope = rememberCoroutineScope()
    val scribbleRecognizer = remember(context) { com.freelauncher.app.data.service.ScribbleRecognizer(context) }
    DisposableEffect(scribbleRecognizer) {
        onDispose { scribbleRecognizer.close() }
    }

    var scribbleStrokes by remember { mutableStateOf<List<com.freelauncher.app.ui.util.ScribbleStroke>>(emptyList()) }
    var currentStrokePoints by remember { mutableStateOf<List<com.freelauncher.app.ui.util.ScribblePoint>>(emptyList()) }
    var isRecognizing by remember { mutableStateOf(false) }
    var recognitionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val screenWidthPx = windowInfo.containerSize.width.toFloat()
    val currentStroke = remember(currentStrokePoints) {
        if (currentStrokePoints.size > 1) com.freelauncher.app.ui.util.ScribbleStroke(currentStrokePoints) else null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .navigationBarsPadding()
            .pointerInput(state.enableScribbleSearch, screenWidthPx, isSearching, state.searchQuery) {
                if (state.enableScribbleSearch && isSearching) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val strokePoints = mutableListOf<com.freelauncher.app.ui.util.ScribblePoint>()
                            strokePoints.add(com.freelauncher.app.ui.util.ScribblePoint(down.position.x, down.position.y))
                            currentStrokePoints = strokePoints.toList()
                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val dragChange = event.changes.find { it.id == pointerId } ?: break

                                if (dragChange.pressed) {
                                    val lastPt = strokePoints.last()
                                    val dx = dragChange.position.x - lastPt.x
                                    val dy = dragChange.position.y - lastPt.y
                                    if (kotlin.math.hypot(dx, dy) > 6f) {
                                        strokePoints.add(com.freelauncher.app.ui.util.ScribblePoint(dragChange.position.x, dragChange.position.y))
                                        currentStrokePoints = strokePoints.toList()
                                    }
                                } else {
                                    break
                                }
                            }

                            val completedStrokePoints = strokePoints.toList()
                            currentStrokePoints = emptyList()

                            if (completedStrokePoints.size > 1) {
                                val gesturalAction = com.freelauncher.app.ui.util.ScribbleGestureCoordinator.analyzeStroke(
                                    points = completedStrokePoints,
                                    screenWidthPx = screenWidthPx
                                )

                                when (gesturalAction) {
                                    com.freelauncher.app.ui.util.GesturalAction.SWIPE_LEFT,
                                    com.freelauncher.app.ui.util.GesturalAction.SWIPE_RIGHT -> {
                                        if (state.searchQuery.isNotEmpty()) {
                                            com.freelauncher.app.ui.util.LauncherHaptics.playClick(context)
                                            val updatedQuery = state.searchQuery.dropLast(1)
                                            onSearchQueryChange(updatedQuery)
                                        }
                                    }
                                    com.freelauncher.app.ui.util.GesturalAction.HANDWRITING_STROKE -> {
                                        val newStrokes = scribbleStrokes + com.freelauncher.app.ui.util.ScribbleStroke(completedStrokePoints)
                                        scribbleStrokes = newStrokes

                                        recognitionJob?.cancel()
                                        recognitionJob = scope.launch {
                                            isRecognizing = true
                                            delay(180.milliseconds)
                                            val candidates = scribbleRecognizer.recognizeStrokes(newStrokes)
                                            isRecognizing = false
                                            scribbleStrokes = emptyList()
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
                                                    com.freelauncher.app.ui.util.LauncherHaptics.playClick(context)
                                                    val accumulatedQuery = (state.searchQuery + resolvedCandidate).uppercase(Locale.ROOT)
                                                    onSearchQueryChange(accumulatedQuery)
                                                }
                                            }
                                        }
                                    }
                                    else -> { /* Tap or system swipe */ }
                                }
                            }
                        }
                    }
                }
            }
            .testTag("neo_minimal_drawer_root")
    ) {
        if (!isSearching) {
            // Main Category View with Horizontal Category Pager
            Column(modifier = Modifier.fillMaxSize()) {
                // Windows Phone Metro Style Large Category Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val currentCatId = categoryIds[pagerState.currentPage]
                    val currentCatTitle = remember(currentCatId, visibleCategories) {
                        if (currentCatId == null) "all apps"
                        else visibleCategories.find { it.id == currentCatId }?.title?.lowercase(Locale.ROOT) ?: currentCatId.lowercase(Locale.ROOT)
                    }

                    val catApps = remember(currentCatId, state.installedApps) {
                        if (currentCatId == null) state.installedApps
                        else state.installedApps.filter { it.categoryId == currentCatId || (it.category.name == currentCatId && it.categoryId.isBlank()) }
                    }

                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = currentCatTitle,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp,
                                fontSize = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${catApps.size} apps",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Horizontal Pager for Swipeable Categories
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    val catId = categoryIds[page]
                    val pageApps = remember(catId, state.installedApps) {
                        if (catId == null) {
                            state.installedApps.sortedBy { it.label.lowercase(Locale.ROOT) }
                        } else {
                            state.installedApps
                                .filter { it.categoryId == catId || (it.category.name == catId && it.categoryId.isBlank()) }
                                .sortedBy { it.label.lowercase(Locale.ROOT) }
                        }
                    }

                    val listState = rememberLazyListState()
                    TrackScrollHaptics(listState)

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (pageApps.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No apps in this category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalFadingEdge(topFadeHeight = 44.dp, bottomFadeHeight = 70.dp),
                                contentPadding = PaddingValues(top = 12.dp, bottom = 85.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                items(
                                    items = pageApps,
                                    key = { it.id.ifBlank { "${it.packageName}/${it.activityName}" } }
                                ) { app ->
                                    val appTextAlignment = when (state.allAppsAlignment) {
                                        com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> TextAlign.Start
                                        com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> TextAlign.Center
                                        com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> TextAlign.End
                                    }
                                    val boxContentAlignment = when (state.allAppsAlignment) {
                                        com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> Alignment.CenterStart
                                        com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> Alignment.Center
                                        com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> Alignment.CenterEnd
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    LauncherHaptics.playClick(context)
                                                    onLaunchApp(app)
                                                },
                                                onLongClick = {
                                                    LauncherHaptics.playClick(context)
                                                    onLongPressApp(app)
                                                }
                                            )
                                            .padding(vertical = 5.dp, horizontal = 24.dp),
                                        contentAlignment = boxContentAlignment
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = when (state.allAppsAlignment) {
                                                com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> Arrangement.Start
                                                com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> Arrangement.Center
                                                com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> Arrangement.End
                                            }
                                        ) {
                                            if (state.iconDisplayMode != com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY) {
                                                com.freelauncher.app.ui.components.AppIcon(
                                                    app = app,
                                                    mode = state.iconDisplayMode,
                                                    iconPackPackage = state.selectedIconPackPackage,
                                                    iconPackManager = effectiveIconPackManager,
                                                    minimalConfig = state.minimalIconPackConfig,
                                                    size = (22.sp.value * state.sixAppsScale).dp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }

                                            Text(
                                                text = app.label,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontSize = 22.sp * state.sixAppsScale,
                                                    fontWeight = FontWeight.Normal,
                                                    letterSpacing = 0.3.sp,
                                                    textAlign = appTextAlignment
                                                ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Search Results Mode
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val headerText = if (state.isScribbleSearchMode) {
                            if (state.searchQuery.isBlank()) "scribble search" else state.searchQuery.uppercase(Locale.ROOT)
                        } else {
                            if (state.searchQuery.isBlank()) "search" else state.searchQuery.lowercase(Locale.ROOT)
                        }

                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp,
                                fontSize = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${matchingApps.size} apps found",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isSearchOnlyMode && state.searchQuery.isBlank()) {
                        item(key = "search_mode_blank_space") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Type to search apps, settings, web...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (matchingApps.isEmpty() && matchingSettings.isEmpty() && state.searchQuery.isNotBlank()) {
                        item(key = "empty_state") {
                            Text(
                                text = "No matching apps found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 40.dp)
                            )
                        }
                    } else {
                        // Matching Apps
                        if (matchingApps.isNotEmpty()) {
                            items(
                                items = matchingApps,
                                key = { "search_${it.id}" }
                            ) { app ->
                                val appTextAlignment = when (state.allAppsAlignment) {
                                    com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> TextAlign.Start
                                    com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> TextAlign.Center
                                    com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> TextAlign.End
                                }
                                val boxContentAlignment = when (state.allAppsAlignment) {
                                    com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> Alignment.CenterStart
                                    com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> Alignment.Center
                                    com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> Alignment.CenterEnd
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .combinedClickable(
                                            onClick = {
                                                LauncherHaptics.playClick(context)
                                                onLaunchApp(app)
                                            },
                                            onLongClick = {
                                                LauncherHaptics.playClick(context)
                                                onLongPressApp(app)
                                            }
                                        )
                                        .padding(vertical = 5.dp),
                                    contentAlignment = boxContentAlignment
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = when (state.allAppsAlignment) {
                                            com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> Arrangement.Start
                                            com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> Arrangement.Center
                                            com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> Arrangement.End
                                        }
                                    ) {
                                        if (state.iconDisplayMode != com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY) {
                                            com.freelauncher.app.ui.components.AppIcon(
                                                app = app,
                                                mode = state.iconDisplayMode,
                                                iconPackPackage = state.selectedIconPackPackage,
                                                iconPackManager = effectiveIconPackManager,
                                                minimalConfig = state.minimalIconPackConfig,
                                                size = (22.sp.value * state.sixAppsScale).dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }

                                        Text(
                                            text = app.label,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = 22.sp * state.sixAppsScale,
                                                fontWeight = FontWeight.Normal,
                                                letterSpacing = 0.3.sp,
                                                textAlign = appTextAlignment
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Matching Settings
                        if (matchingSettings.isNotEmpty()) {
                            items(
                                items = matchingSettings,
                                key = { "setting_${it.intentAction}" }
                            ) { setting ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                context.startActivity(Intent(setting.intentAction).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                            } catch (_: Exception) {}
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = setting.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = setting.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Web Search & YouTube Search Shortcuts (when user typed a search query)
                    if (state.searchQuery.isNotBlank()) {
                        item(key = "web_search_item") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(SearchManager.QUERY, state.searchQuery)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(webIntent)
                                        } catch (_: Exception) {
                                            try {
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://www.google.com/search?q=${Uri.encode(state.searchQuery)}".toUri()
                                                ).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(browserIntent)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Search Google for \"${state.searchQuery}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Open web browser search",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        item(key = "youtube_search_item") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        try {
                                            context.startActivity(UniversalSearchManager.getYouTubeSearchIntent(state.searchQuery))
                                        } catch (_: Exception) {
                                            try {
                                                context.startActivity(UniversalSearchManager.getYouTubeWebSearchIntent(state.searchQuery))
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFF0000).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0000),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Search YouTube for \"${state.searchQuery}\"",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Open YouTube app or web",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Search Bar (Theme-Adaptive Dark Charcoal Pill)
        if (!state.isScribbleSearchMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable {
                        isSearchFocused = true
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.searchQuery.isEmpty() && !isSearchFocused) {
                            Text(
                                text = "Tap to search...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        }

                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = {
                                onSearchQueryChange(it)
                                isSearchFocused = true
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                isSearchFocused = false
                                keyboardController?.hide()
                                if (state.isSearchOnlyMode) {
                                    onNavigate(LauncherScreen.HOME)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Live Smooth Ink Drawing Canvas Overlay for Scribble Search
        if (state.enableScribbleSearch) {
            val strokeColor = MaterialTheme.colorScheme.primary
            val activeStrokes = scribbleStrokes + listOfNotNull(currentStroke)
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (stroke in activeStrokes) {
                    if (stroke.points.size > 1) {
                        val path = Path().apply {
                            moveTo(stroke.points.first().x, stroke.points.first().y)
                            for (i in 1 until stroke.points.size) {
                                val p = stroke.points[i]
                                lineTo(p.x, p.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}
}

