package com.freelauncher.app.ui.screens

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.R
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.SettingSearchResult
import com.freelauncher.app.data.service.UniversalSearchManager
import com.freelauncher.app.ui.util.DateTimeUtils
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.TrackScrollHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(
    state: LauncherUiState,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
    onOpenCategoryManager: () -> Unit = {},
    onOpenAddCategory: () -> Unit = {},
) {
    val isNeoMinimalUnlocked = state.isProUnlocked && state.appDrawerStyle == com.freelauncher.app.data.models.AppDrawerStyle.NEO_MINIMAL
    if (isNeoMinimalUnlocked) {
        NeoMinimalAppDrawer(
            state = state,
            onLaunchApp = onLaunchApp,
            onLongPressApp = onLongPressApp,
            onNavigate = onNavigate,
            onSearchQueryChange = onSearchQueryChange,
            modifier = modifier
        )
    } else {
        ClassicAppDrawer(
            state = state,
            onLaunchApp = onLaunchApp,
            onLongPressApp = onLongPressApp,
            onNavigate = onNavigate,
            onSearchQueryChange = onSearchQueryChange,
            modifier = modifier,
            iconPackManager = iconPackManager,
            onOpenCategoryManager = onOpenCategoryManager,
            onOpenAddCategory = onOpenAddCategory
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClassicAppDrawer(
    state: LauncherUiState,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
    onOpenCategoryManager: () -> Unit = {},
    onOpenAddCategory: () -> Unit = {},
) {
    BackHandler {
        onNavigate(LauncherScreen.HOME)
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var selectedCategoryFilterId by remember { mutableStateOf<String?>(null) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val lazyListState = rememberLazyListState()
    TrackScrollHaptics(lazyListState)

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        delay(120.milliseconds)
        keyboardController?.show()
    }

    val isSearching = state.searchQuery.isNotBlank()

    // 1. Filtered and Sorted Apps
    val matchingApps = remember(state.installedApps, state.searchQuery, selectedCategoryFilterId) {
        val list = if (selectedCategoryFilterId != null && state.searchQuery.isBlank()) {
            state.installedApps.filter { 
                it.categoryId == selectedCategoryFilterId || 
                (it.category.name == selectedCategoryFilterId && it.categoryId.isBlank()) 
            }
        } else {
            state.installedApps
        }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase(Locale.ROOT)
            list.filter {
                it.label.lowercase(Locale.ROOT).contains(q) ||
                        it.packageName.lowercase(Locale.ROOT).contains(q)
            }.sortedBy { it.label.lowercase(Locale.ROOT) }
        } else {
            list.sortedBy { it.label.lowercase(Locale.ROOT) }
        }
    }

    // 2. Universal Search: Settings
    val matchingSettings = remember(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            UniversalSearchManager.searchSettings(state.searchQuery)
        } else {
            emptyList()
        }
    }

    val visibleCategories = remember(state.categories) {
        state.categories.filter { !it.isHidden }
    }
    val categoryFilterIds = remember(visibleCategories) {
        listOf<String?>(null) + visibleCategories.map { it.id }
    }
    val categoryScrollState = rememberScrollState()

    LaunchedEffect(selectedCategoryFilterId) {
        val index = categoryFilterIds.indexOf(selectedCategoryFilterId).coerceAtLeast(0)
        val approxOffset = (index * 85 * context.resources.displayMetrics.density).toInt()
        categoryScrollState.animateScrollTo(approxOffset)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = if (isLandscape) 12.dp else 44.dp)
            .pointerInput(categoryFilterIds, selectedCategoryFilterId) {
                detectDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        totalDragX = 0f
                    },
                    onDragEnd = {
                        val threshold = 50f
                        val isHorizontal = abs(totalDragX) > abs(totalDragY) * 1.1f
                        val isVertical = abs(totalDragY) > abs(totalDragX) * 1.1f

                        if (isHorizontal) {
                            val currentIndex = categoryFilterIds.indexOf(selectedCategoryFilterId).coerceAtLeast(0)
                            if (totalDragX < -threshold) {
                                // Swipe Left -> Next Category
                                val nextIndex = (currentIndex + 1) % categoryFilterIds.size
                                LauncherHaptics.playClick(context)
                                selectedCategoryFilterId = categoryFilterIds[nextIndex]
                            } else if (totalDragX > threshold) {
                                // Swipe Right -> Previous Category
                                val prevIndex = if (currentIndex - 1 < 0) categoryFilterIds.size - 1 else currentIndex - 1
                                LauncherHaptics.playClick(context)
                                selectedCategoryFilterId = categoryFilterIds[prevIndex]
                            }
                        } else if (isVertical) {
                            if (totalDragY > threshold) {
                                // Swipe Down -> Return Home
                                onNavigate(LauncherScreen.HOME)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        totalDragY += dragAmount.y
                        totalDragX += dragAmount.x
                    }
                )
            }
            .testTag("all_apps_screen_root")
    ) {
        // Top Return Hint Button and Search Bar Container (lowered well below punch hole camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(LauncherScreen.HOME) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("all_apps_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Universal Search Box in the marked lower area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("apps_search_input"),
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search apps, settings, contacts...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Row below search bar (Hidden when searching or in Search-Only mode)
        if (!isSearching && !state.isSearchOnlyMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(categoryScrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = selectedCategoryFilterId == null
                CategoryChip(
                    label = "ALL",
                    isSelected = allSelected,
                    onClick = {
                        LauncherHaptics.playClick(context)
                        selectedCategoryFilterId = null
                    },
                    testTag = "category_chip_all"
                )

                // Visible Categories (ordered according to user structure)
                visibleCategories.forEach { category ->
                    val isSelected = selectedCategoryFilterId == category.id
                    CategoryChip(
                        label = category.title,
                        isSelected = isSelected,
                        onClick = {
                            LauncherHaptics.playClick(context)
                            selectedCategoryFilterId = if (isSelected) null else category.id
                        },
                        testTag = "category_chip_${category.id.lowercase(Locale.ROOT)}"
                    )
                }

                // Quick Add Category Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            0.75.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            LauncherHaptics.playClick(context)
                            onOpenAddCategory()
                        }
                        .testTag("category_chip_add_new")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ADD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Restructure & Manage Categories Icon Chip
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            LauncherHaptics.playClick(context)
                            onOpenCategoryManager()
                        }
                        .testTag("manage_categories_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manage and Restructure Categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Content List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .testTag("all_apps_lazy_column"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (state.isSearchOnlyMode && state.searchQuery.isBlank()) {
                // CLEAN BLANK SPACE IN SEARCH MODE BEFORE TYPING STARTS
                item(key = "search_mode_blank_space") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type to search apps, contacts, web...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isSearching) {
                // UNIVERSAL SEARCH RESULTS VIEW

                // Section 1: Matching Apps
                if (matchingApps.isNotEmpty()) {
                    item(key = "search_header_apps") {
                        SearchSectionHeader(title = "APPS (${matchingApps.size})")
                    }
                    items(
                        items = matchingApps,
                        key = { "app_${it.packageName}_${it.label}" }
                    ) { app ->
                        AllAppsRowItem(
                            app = app,
                            showMonogram = state.showMonograms,
                            iconDisplayMode = state.iconDisplayMode,
                            iconPackPackage = state.selectedIconPackPackage,
                            iconPackManager = iconPackManager,
                            minimalConfig = state.minimalIconPackConfig,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            testTag = "search_app_item_${app.packageName}"
                        )
                    }
                }

                // Section 2: Phone Settings
                if (matchingSettings.isNotEmpty()) {
                    item(key = "search_header_settings") {
                        SearchSectionHeader(title = "PHONE SETTINGS (${matchingSettings.size})")
                    }
                    items(
                        items = matchingSettings,
                        key = { "setting_${it.intentAction}" }
                    ) { setting ->
                        SettingResultRowItem(
                            setting = setting,
                            onClick = {
                                try {
                                    val intent = Intent(setting.intentAction).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to general settings
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (_: Exception) {
                                        android.util.Log.e("AllAppsScreen", "Failed to open settings")
                                    }
                                }
                            }
                        )
                    }
                }

                // Section 5: Web Search Fallback
                item(key = "search_header_web") {
                    SearchSectionHeader(title = "WEB SEARCH")
                }
                item(key = "web_search_item") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(SearchManager.QUERY, state.searchQuery)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(webIntent)
                                } catch (e: Exception) {
                                    try {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/search?q=${Uri.encode(state.searchQuery)}")
                                        ).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        android.util.Log.e("AllAppsScreen", "Failed to open settings")
                                    }
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
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    context.startActivity(UniversalSearchManager.getYouTubeSearchIntent(state.searchQuery))
                                } catch (e: Exception) {
                                    try {
                                        context.startActivity(UniversalSearchManager.getYouTubeWebSearchIntent(state.searchQuery))
                                    } catch (_: Exception) {
                                        android.util.Log.e("AllAppsScreen", "Failed to open settings")
                                    }
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
                                imageVector = Icons.Outlined.Language, // Will find a better icon or use generic
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

            } else {
                // NORMAL ALL APPS VIEW
                if (matchingApps.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, bottom = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else if (selectedCategoryFilterId == null) {
                    // WHEN NO CATEGORY IS SELECTED: Show all apps strictly alphabetically (A-Z)
                    if (isLandscape && !isSearching) {
                        val appRows = matchingApps.chunked(2)
                        items(
                            items = appRows,
                            key = { row -> row.first().id.ifBlank { "${row.first().packageName}/${row.first().activityName}" } }
                        ) { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowApps.forEach { app ->
                                    AllAppsRowItem(
                                        app = app,
                                        showMonogram = state.showMonograms,
                                        iconDisplayMode = state.iconDisplayMode,
                                        iconPackPackage = state.selectedIconPackPackage,
                                        iconPackManager = iconPackManager,
                                        minimalConfig = state.minimalIconPackConfig,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onLongPressApp(app) },
                                        testTag = "app_item_${app.packageName}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowApps.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = matchingApps,
                            key = { it.id.ifBlank { "${it.packageName}/${it.activityName}#${it.label}" } }
                        ) { app ->
                            AllAppsRowItem(
                                app = app,
                                showMonogram = state.showMonograms,
                                iconDisplayMode = state.iconDisplayMode,
                                iconPackPackage = state.selectedIconPackPackage,
                                iconPackManager = iconPackManager,
                                minimalConfig = state.minimalIconPackConfig,
                                alignment = state.allAppsAlignment,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                                testTag = "app_item_${app.packageName}"
                            )
                        }
                    }
                } else {
                    // WHEN A SPECIFIC CATEGORY IS SELECTED: Show header for that category followed by its apps
                    val currentCategoryTitle = state.categories.find { it.id == selectedCategoryFilterId }?.title ?: selectedCategoryFilterId!!
                    item(key = "header_${selectedCategoryFilterId}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = currentCategoryTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.5.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        }
                    }

                    if (isLandscape && !isSearching) {
                        val appRows = matchingApps.chunked(2)
                        items(
                            items = appRows,
                            key = { row -> row.first().id.ifBlank { "${row.first().packageName}/${row.first().activityName}" } }
                        ) { rowApps ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowApps.forEach { app ->
                                    AllAppsRowItem(
                                        app = app,
                                        showMonogram = state.showMonograms,
                                        iconDisplayMode = state.iconDisplayMode,
                                        iconPackPackage = state.selectedIconPackPackage,
                                        iconPackManager = iconPackManager,
                                        minimalConfig = state.minimalIconPackConfig,
                                        alignment = state.allAppsAlignment,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onLongPressApp(app) },
                                        testTag = "app_item_${app.packageName}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowApps.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(
                            items = matchingApps,
                            key = { it.id.ifBlank { "${it.packageName}/${it.activityName}#${it.label}" } }
                        ) { app ->
                            AllAppsRowItem(
                                app = app,
                                showMonogram = state.showMonograms,
                                iconDisplayMode = state.iconDisplayMode,
                                iconPackPackage = state.selectedIconPackPackage,
                                iconPackManager = iconPackManager,
                                minimalConfig = state.minimalIconPackConfig,
                                alignment = state.allAppsAlignment,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                                testTag = "app_item_${app.packageName}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}

@Composable
fun SettingResultRowItem(
    setting: SettingSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(6.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = setting.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = 0.75.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.secondary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsRowItem(
    app: AppItem,
    showMonogram: Boolean,
    iconDisplayMode: com.freelauncher.app.data.models.IconDisplayMode = com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY,
    iconPackPackage: String = "",
    iconPackManager: com.freelauncher.app.data.service.IconPackManager? = null,
    minimalConfig: com.freelauncher.app.data.models.MinimalIconPackConfig = com.freelauncher.app.data.models.MinimalIconPackConfig(),
    alignment: com.freelauncher.app.data.models.AllAppsAlignment = com.freelauncher.app.data.models.AllAppsAlignment.LEFT,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val rowArrangement = when (alignment) {
        com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> Arrangement.Start
        com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> Arrangement.Center
        com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> Arrangement.End
    }
    val textAlignment = when (alignment) {
        com.freelauncher.app.data.models.AllAppsAlignment.LEFT -> TextAlign.Start
        com.freelauncher.app.data.models.AllAppsAlignment.CENTER -> TextAlign.Center
        com.freelauncher.app.data.models.AllAppsAlignment.RIGHT -> TextAlign.End
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = rowArrangement,
            modifier = Modifier.weight(1f)
        ) {
            val shouldShowIcon = iconDisplayMode != com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY
            if (shouldShowIcon) {
                com.freelauncher.app.ui.components.AppIcon(
                    app = app,
                    mode = iconDisplayMode,
                    iconPackPackage = iconPackPackage,
                    iconPackManager = iconPackManager,
                    minimalConfig = minimalConfig,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
            }

            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp,
                    textAlign = textAlignment
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (app.isPinned) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
