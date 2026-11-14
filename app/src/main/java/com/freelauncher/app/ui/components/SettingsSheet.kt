package com.freelauncher.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.freelauncher.app.BuildConfig
import com.freelauncher.app.ui.theme.LauncherFont
import com.freelauncher.app.ui.theme.LauncherThemeMode
import com.freelauncher.app.ui.util.LauncherHaptics
import java.io.File
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    currentClockStyle: ClockStyle,
    currentFont: LauncherFont,
    currentTheme: LauncherThemeMode,
    currentWallpaperId: String = "oled_void",
    customWallpaperUri: String? = null,
    userWallpapers: List<com.freelauncher.app.ui.theme.LauncherWallpaper> = emptyList(),
    showMonograms: Boolean = false,
    showGestureHints: Boolean = false,
    showNewsFeed: Boolean = true,
    showTimeAway: Boolean = true,
    showWeatherBatteryGlance: Boolean = false,
    enableDoubleTapToSleep: Boolean = false,
    sixAppsScale: Float = 0.9f,
    temperatureUnit: String = "F",
    currentGreeting: String,
    isBiometricLockEnabled: Boolean = false,
    lockMethod: String = "pin",
    customPin: String = "",
    onClockStyleChanged: (ClockStyle) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onThemeChanged: (LauncherThemeMode) -> Unit,
    onOpenWallpaperPicker: () -> Unit = {},
    onMonogramsToggled: (Boolean) -> Unit = {},
    onGestureHintsToggled: (Boolean) -> Unit = {},
    onDoubleTapToSleepToggled: (Boolean) -> Unit = {},
    onSixAppsScaleChanged: (Float) -> Unit = {},
    onNewsFeedToggled: (Boolean) -> Unit = {},
    onTimeAwayToggled: (Boolean) -> Unit = {},
    enableSwipeDownSearch: Boolean = false,
    onSwipeDownSearchToggled: (Boolean) -> Unit = {},
    enableScribbleSearch: Boolean = false,
    onScribbleSearchToggled: (Boolean) -> Unit = {},
    enableMindfulPause: Boolean = false,
    onMindfulPauseToggled: (Boolean) -> Unit = {},
    onWeatherBatteryGlanceToggled: (Boolean) -> Unit = {},
    onTemperatureUnitChanged: (String) -> Unit = {},
    onGreetingChanged: (String) -> Unit,
    onBiometricLockToggled: (Boolean) -> Unit = {},
    onLockMethodChanged: (String) -> Unit = {},
    onOpenSetPinDialog: () -> Unit = {},
    onOpenCategoryManager: () -> Unit = {},
    onOpenRssManager: () -> Unit,
    onOpenOnboardingGuide: () -> Unit = {},
    onOpenAboutSheet: () -> Unit = {},
    onOpenBackupSheet: () -> Unit = {},
    enableLandscapeMode: Boolean = false,
    onLandscapeModeToggled: (Boolean) -> Unit = {},
    iconDisplayMode: com.freelauncher.app.data.models.IconDisplayMode = com.freelauncher.app.data.models.IconDisplayMode.TEXT_ONLY,
    selectedIconPackPackage: String = "",
    installedIconPacks: List<com.freelauncher.app.data.service.IconPackInfo> = emptyList(),
    minimalIconPackConfig: com.freelauncher.app.data.models.MinimalIconPackConfig = com.freelauncher.app.data.models.MinimalIconPackConfig(),
    onIconDisplayModeChanged: (com.freelauncher.app.data.models.IconDisplayMode) -> Unit = {},
    onSelectedIconPackChanged: (String) -> Unit = {},
    onOpenMinimalIconPackCreator: () -> Unit = {},
    hideEverythingOnHome: Boolean = false,
    onHideEverythingOnHomeToggled: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val previewTime = remember { Date() }
    val lazyListState = rememberLazyListState()
    var showIconPackSheet by remember { mutableStateOf(false) }
    var showScribbleConfirmationDialog by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("index") }
    var searchQuery by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    LaunchedEffect(selectedCategory) { lazyListState.scrollToItem(0) }
    BackHandler(enabled = selectedCategory != "index") { selectedCategory = "index" }

    val categoryTitle = when (selectedCategory) {
        "personalization" -> "Personalization"
        "layout" -> "Home Screen"
        "apps" -> "Apps & Icons"
        "gestures" -> "Gestures"
        "focus" -> "Focus & Privacy"
        "feeds" -> "Feeds & Content"
        "backup" -> "Backup & Restore"
        "about" -> "About NOMA"
        else -> "Settings"
    }

    Dialog(
        onDismissRequest = {
            if (selectedCategory != "index") {
                selectedCategory = "index"
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 50.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Windows Phone Panoramic Header
                item(key = "metro_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory == "index") "settings" else categoryTitle.lowercase(Locale.ROOT),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp,
                                fontSize = 34.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { if (selectedCategory == "index") onDismiss() else selectedCategory = "index" },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_settings_button")
                        ) {
                            Icon(
                                imageVector = if (selectedCategory == "index") Icons.Default.Close else Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = if (selectedCategory == "index") "Close Settings" else "Back to settings",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // HOMEPAGE INDEX PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "index") {
                    // Search Bar
                    item(key = "search_settings_bar") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Settings",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search settings...",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                            )
                                        }
                                        inner()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "settings_metro_categories") {
                        val categories = listOf(
                            Triple("personalization", "personalization", "Themes, wallpaper, & typography"),
                            Triple("layout", "home screen", "Clock styles, greeting, layout, & glance options"),
                            Triple("apps", "apps & icons", "Icon packs, icon customization, & app drawer"),
                            Triple("gestures", "gestures", "Swipe actions, double-tap to sleep, & scribble search"),
                            Triple("focus", "focus & privacy", "App lock, secret PIN, biometrics, & mindful pause"),
                            Triple("feeds", "feeds & content", "RSS news feeds, feeds manager, & content settings"),
                            Triple("backup", "backup & restore", "Export or import launcher setup & layout JSON"),
                            Triple("onboarding", "onboarding & gesture guide", "Spatial navigation map & gesture control tips"),
                            Triple("about", "about noma", "App version, credits, donation, & feedback")
                        )

                        val filteredCategories = if (searchQuery.isNotBlank()) {
                            categories.filter {
                                it.second.contains(searchQuery, ignoreCase = true) ||
                                it.third.contains(searchQuery, ignoreCase = true)
                            }
                        } else categories

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredCategories.forEach { (key, title, description) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            LauncherHaptics.playClick(context)
                                            if (key == "onboarding") {
                                                onOpenOnboardingGuide()
                                            } else {
                                                selectedCategory = key
                                            }
                                        }
                                        .testTag("settings_category_$key"),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontWeight = FontWeight.Light,
                                                    fontSize = 22.sp,
                                                    letterSpacing = (-0.2).sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = description,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 12.sp,
                                                    letterSpacing = 0.3.sp
                                                ),
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.ChevronRight,
                                            contentDescription = "Open $title",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 1. PERSONALIZATION SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "personalization") {
                    item(key = "page_personalization") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Section: Themes
                            MetroSectionHeader(title = "COLOR SCHEME & THEME")
                            MaterialYouCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LauncherThemeMode.entries.forEach { mode ->
                                        val isSelected = mode == currentTheme
                                        val cardBg by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            animationSpec = tween(200),
                                            label = "cardBg"
                                        )
                                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    LauncherHaptics.playClick(context)
                                                    onThemeChanged(mode)
                                                }
                                                .testTag("theme_mode_${mode.name.lowercase(Locale.ROOT)}"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = cardBg
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (mode) {
                                                                    LauncherThemeMode.OLED_BLACK -> Color(0xFF000000)
                                                                    LauncherThemeMode.MATERIAL_YOU -> MaterialTheme.colorScheme.primary
                                                                    LauncherThemeMode.CHARCOAL -> Color(0xFF1E1E24)
                                                                    LauncherThemeMode.PAPER_WARM -> Color(0xFFF7F4EC)
                                                                    LauncherThemeMode.MINIMAL_LIGHT -> Color(0xFFFFFFFF)
                                                                }
                                                            )
                                                            .border(
                                                                1.5.dp,
                                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                                CircleShape
                                                            )
                                                    )
                                                    Column {
                                                        Text(
                                                            text = mode.displayName,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                            ),
                                                            color = textColor
                                                        )
                                                        Text(
                                                            text = if (mode == LauncherThemeMode.MATERIAL_YOU) "Dynamic palette extracted from wallpaper" else if (mode.isDark) "Dark Surface • High Contrast" else "Light Surface • Soft Tone",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = textColor.copy(alpha = 0.75f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: Wallpaper Selector
                            MetroSectionHeader(title = "WALLPAPER & BACKGROUND")
                            MaterialYouCard {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val currentWp = remember(currentWallpaperId, userWallpapers) {
                                        userWallpapers.find { it.id == currentWallpaperId }
                                            ?: com.freelauncher.app.ui.theme.LauncherWallpaper.getById(currentWallpaperId)
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onOpenWallpaperPicker()
                                            }
                                            .testTag("settings_open_wallpaper_picker"),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 46.dp, height = 46.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .border(
                                                            1.dp,
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                            RoundedCornerShape(10.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (currentWallpaperId == "custom_gallery" && !customWallpaperUri.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = File(customWallpaperUri),
                                                            contentDescription = "Custom Wallpaper",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(currentWp.brush)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (currentWallpaperId == "custom_gallery" && !customWallpaperUri.isNullOrBlank()) "Custom Phone Wallpaper" else currentWp.name,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.SemiBold
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (currentWallpaperId == "custom_gallery" && !customWallpaperUri.isNullOrBlank()) "Personal photo from device gallery" else currentWp.subtitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                                contentDescription = "Open Wallpaper Picker",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Typography
                            MetroSectionHeader(title = "SYSTEM TYPOGRAPHY & FONT")
                            MaterialYouCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LauncherFont.entries.forEach { font ->
                                            val isSelected = font == currentFont
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onFontChanged(font) },
                                                label = {
                                                    Text(
                                                        text = font.displayName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 2. HOME SCREEN SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "layout") {
                    item(key = "page_home_screen") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "HOME CLOCK STYLE")
                            MaterialYouCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        items(ClockStyle.entries, key = { it.id }) { style ->
                                            val isSelected = style == currentClockStyle
                                            ClockStyleCard(
                                                style = style,
                                                isSelected = isSelected,
                                                previewTime = previewTime,
                                                onClick = { onClockStyleChanged(style) }
                                            )
                                        }
                                    }
                                }
                            }

                            MetroSectionHeader(title = "CONTEXTUAL HOME GREETING")
                            MaterialYouCard {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val greetings = listOf(
                                        "auto" to "Dynamic (Time-based)",
                                        "Less phone. More freedom." to "✨ Less phone. More freedom.",
                                        "Stay present" to "🌿 Stay present",
                                        "Hi there" to "👋 Hi there",
                                        "What's new?" to "📰 What's new?"
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        greetings.forEach { (value, label) ->
                                            val isSelected = currentGreeting == value
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onGreetingChanged(value) },
                                                label = { Text(label) },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            MetroSectionHeader(title = "DISPLAY & GLANCE OPTIONS")
                            MaterialYouCard {
                                Column {
                                    // Hide Everything
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onHideEverythingOnHomeToggled(!hideEverythingOnHome)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Hide Everything on Home", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Hide clock, date, and widgets for a pure clean canvas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = hideEverythingOnHome,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onHideEverythingOnHomeToggled(it)
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Weather & Battery Glance
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onWeatherBatteryGlanceToggled(!showWeatherBatteryGlance)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Weather & Battery Glance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Text-only status badge on Home Screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = showWeatherBatteryGlance,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onWeatherBatteryGlanceToggled(it)
                                            }
                                        )
                                    }

                                    if (showWeatherBatteryGlance) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Temperature Unit", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                FilterChip(selected = temperatureUnit == "F", onClick = { onTemperatureUnitChanged("F") }, label = { Text("°F") })
                                                FilterChip(selected = temperatureUnit == "C", onClick = { onTemperatureUnitChanged("C") }, label = { Text("°C") })
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Force Landscape Mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onLandscapeModeToggled(!enableLandscapeMode)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Force Landscape Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Force sensor landscape or auto-adapt on system rotation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = enableLandscapeMode,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onLandscapeModeToggled(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 3. APPS & ICONS SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "apps") {
                    item(key = "page_apps_icons") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "APP ICONS & THIRD-PARTY PACKS")
                            MaterialYouCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LauncherHaptics.playClick(context)
                                            showIconPackSheet = true
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("App Icons & Icon Packs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text(iconDisplayMode.label + " • " + iconDisplayMode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open Icon Pack Settings", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }

                            MetroSectionHeader(title = "QUICK ACCESS SLOTS SCALE")
                            MaterialYouCard {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Quick Access (6 Apps View) Size", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("${(sixAppsScale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = sixAppsScale,
                                        onValueChange = { onSixAppsScaleChanged(it) },
                                        valueRange = 0.6f..1.8f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(0.8f, 0.9f, 1.0f, 1.2f, 1.4f).forEach { preset ->
                                            val isSelected = (sixAppsScale * 100).toInt() == (preset * 100).toInt()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSixAppsScaleChanged(preset) },
                                                label = { Text("${(preset * 100).toInt()}%") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 4. GESTURES SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "gestures") {
                    item(key = "page_gestures") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "SYSTEM & SCREEN GESTURES")
                            MaterialYouCard {
                                Column {
                                    // Double-Tap to Sleep
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onDoubleTapToSleepToggled(!enableDoubleTapToSleep)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Double-Tap to Sleep", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Double-tap empty space on home screen to turn off / lock display", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = enableDoubleTapToSleep,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onDoubleTapToSleepToggled(it)
                                            }
                                        )
                                    }

                                    if (enableDoubleTapToSleep) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "GPay / Banking Note: If Google Pay or banking apps show an accessibility security alert, you can temporarily turn off Double-Tap to Sleep here.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Swipe Down for Search
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onSwipeDownSearchToggled(!enableSwipeDownSearch)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Swipe Down to Search", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Swipe down on home screen to open universal search immediately", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = enableSwipeDownSearch,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onSwipeDownSearchToggled(it)
                                            }
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Scribble to Search
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                if (!enableScribbleSearch) {
                                                    showScribbleConfirmationDialog = true
                                                } else {
                                                    onScribbleSearchToggled(false)
                                                }
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Scribble to Search", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Draw letters or app names on home screen to search", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = enableScribbleSearch,
                                            onCheckedChange = { isChecked ->
                                                LauncherHaptics.playClick(context)
                                                if (isChecked) {
                                                    showScribbleConfirmationDialog = true
                                                } else {
                                                    onScribbleSearchToggled(false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 5. FOCUS & PRIVACY SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "focus") {
                    item(key = "page_focus_privacy") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "APP LOCK & SECURITY")
                            MaterialYouCard {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onBiometricLockToggled(!isBiometricLockEnabled)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("App Lock Protection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Require PIN/Biometrics to unlock All Apps and Settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = isBiometricLockEnabled,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onBiometricLockToggled(it)
                                            }
                                        )
                                    }

                                    if (isBiometricLockEnabled) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Lock Method Preference", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(
                                                    selected = lockMethod == "pin",
                                                    onClick = {
                                                        LauncherHaptics.playClick(context)
                                                        onLockMethodChanged("pin")
                                                    },
                                                    label = { Text("Custom 4-Digit PIN", style = MaterialTheme.typography.labelSmall) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                FilterChip(
                                                    selected = lockMethod == "biometric",
                                                    onClick = {
                                                        LauncherHaptics.playClick(context)
                                                        onLockMethodChanged("biometric")
                                                    },
                                                    label = { Text("System Biometrics", style = MaterialTheme.typography.labelSmall) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (lockMethod == "pin") {
                                                Button(
                                                    onClick = {
                                                        LauncherHaptics.playClick(context)
                                                        onOpenSetPinDialog()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(if (customPin.isBlank()) "Set 4-Digit Secret PIN" else "Change 4-Digit PIN (PIN Set)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Mindful Pause
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onMindfulPauseToggled(!enableMindfulPause)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Mindful Pause for Distracting Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text("Forces a 5-second breathing pause before launching social or gaming apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = enableMindfulPause,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onMindfulPauseToggled(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 6. FEEDS & CONTENT SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "feeds") {
                    item(key = "page_feeds_content") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "NEWS FEEDS & RSS INTEGRATION")
                            MaterialYouCard {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onNewsFeedToggled(!showNewsFeed)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enable News Feed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text(if (showNewsFeed) "Swipe left on home to view RSS news" else "News feed disabled for maximum minimalism", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = showNewsFeed,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onNewsFeedToggled(it)
                                            }
                                        )
                                    }

                                    if (showNewsFeed) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenRssManager() }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("RSS News Feeds Manager", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                                Text("Add, remove, or customize tech and Malayalam RSS news feeds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Open Feeds Manager", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    // Time Away
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LauncherHaptics.playClick(context)
                                                onTimeAwayToggled(!showTimeAway)
                                            }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Time Away Screen", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                            Text(if (showTimeAway) "Swipe right on home to view phone-free statistics" else "Time Away screen disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Switch(
                                            checked = showTimeAway,
                                            onCheckedChange = {
                                                LauncherHaptics.playClick(context)
                                                onTimeAwayToggled(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 7. BACKUP & RESTORE SUB-PAGE
                // -------------------------------------------------------------
                if (selectedCategory == "backup") {
                    item(key = "page_backup_restore") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MetroSectionHeader(title = "EXPORT & IMPORT PREFERENCES")
                            MaterialYouCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenBackupSheet() }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Backup & Restore Setup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium))
                                        Text("Export or import launcher settings and layout as JSON", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Backup and Restore", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 8. ABOUT NOMA SUB-PAGE (DIRECT SHOW DETAILS, CREDITS, DONATIONS & FEEDBACK)
                // -------------------------------------------------------------
                if (selectedCategory == "about") {
                    item(key = "page_about_noma") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MaterialYouCard {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            android.widget.ImageView(ctx).apply {
                                                setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                                            }
                                        },
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                                    )
                                    Text(
                                        text = "NOMA Launcher",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Version ${BuildConfig.VERSION_NAME} • Minimalist, focused, intelligent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Minimal Android launcher focused on calm and intentional phone usage.",
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }

                            MetroSectionHeader(title = "SUPPORT & COMMUNITY")
                            MaterialYouCard {
                                Column {
                                    MetroActionRowItem(
                                        icon = Icons.Outlined.Star,
                                        title = "Rate NOMA Launcher",
                                        subtitle = "Support the project with a Play Store review",
                                        onClick = { openUrl("market://details?id=com.freelauncher.app") }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    MetroActionRowItem(
                                        icon = Icons.Outlined.Coffee,
                                        title = "Buy Me a Coffee / Donation",
                                        subtitle = "Support independent development via Buy Me a Coffee",
                                        onClick = { openUrl("https://buymeacoffee.com/amalsnair535") }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    MetroActionRowItem(
                                        icon = Icons.Outlined.Groups,
                                        title = "Reddit Community (r/noma_launcher)",
                                        subtitle = "Join discussion, share setups & request features",
                                        onClick = { openUrl("https://www.reddit.com/r/noma_launcher") }
                                    )
                                }
                            }

                            MetroSectionHeader(title = "LEGAL & OPEN SOURCE")
                            MaterialYouCard {
                                Column {
                                    MetroActionRowItem(
                                        icon = Icons.AutoMirrored.Outlined.Article,
                                        title = "Privacy Policy",
                                        subtitle = "Our commitment to your data",
                                        onClick = { openUrl("https://raw.githubusercontent.com/amalsnair535/free-launcher/main/privacy_policy.md") }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    MetroActionRowItem(
                                        icon = Icons.Outlined.Code,
                                        title = "Open Source Libraries",
                                        subtitle = "Check out software licenses on GitHub",
                                        onClick = { openUrl("https://github.com/amalsnair535/free-launcher") }
                                    )
                                }
                            }

                            MetroSectionHeader(title = "FEEDBACK & BUG REPORTS")
                            MaterialYouCard {
                                Column {
                                    MetroActionRowItem(
                                        icon = Icons.Outlined.BugReport,
                                        title = "Report a Bug",
                                        subtitle = "Tell us what's broken",
                                        onClick = { openUrl("mailto:amalsnair535@gmail.com?subject=.noma%20Bug%20Report") }
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

                                    MetroActionRowItem(
                                        icon = Icons.Outlined.Lightbulb,
                                        title = "Suggest a Feature",
                                        subtitle = "What should we build next?",
                                        onClick = { openUrl("mailto:amalsnair535@gmail.com?subject=.noma%20Feature%20Suggestion") }
                                    )
                                }
                            }

                            // Footer
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Built with ❤️ in India",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Footer
                item(key = "footer") {
                    if (selectedCategory == "index") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.ImageView(ctx).apply {
                                        setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.packageName))
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Version ${com.freelauncher.app.BuildConfig.VERSION_NAME} • Minimalist, focused, intelligent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showIconPackSheet) {
        IconPackSheet(
            iconDisplayMode = iconDisplayMode,
            selectedIconPackPackage = selectedIconPackPackage,
            installedIconPacks = installedIconPacks,
            minimalIconPackConfig = minimalIconPackConfig,
            onIconDisplayModeChanged = onIconDisplayModeChanged,
            onSelectedIconPackChanged = onSelectedIconPackChanged,
            onOpenMinimalIconPackCreator = onOpenMinimalIconPackCreator,
            onDismiss = { showIconPackSheet = false }
        )
    }

    if (showScribbleConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showScribbleConfirmationDialog = false },
            title = {
                Text(
                    text = "Enable Scribble to Search?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Turning on Scribble to Search will automatically turn off 'Swipe Down to Search' to prevent gesture conflicts on the home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        LauncherHaptics.playClick(context)
                        showScribbleConfirmationDialog = false
                        onScribbleSearchToggled(true)
                        onSwipeDownSearchToggled(false)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Turn On")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showScribbleConfirmationDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
private fun MetroActionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = "Open Link",
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MetroSectionHeader(title: String) {
    Text(
        text = title.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun ClockStyleCard(
    style: ClockStyle,
    isSelected: Boolean,
    previewTime: Date,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(170.dp)
            .height(125.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 0.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(280.dp, 200.dp)
                    .scale(0.40f),
                contentAlignment = Alignment.Center
            ) {
                val previewTimeFlow = remember(previewTime) { kotlinx.coroutines.flow.MutableStateFlow(previewTime) }
                MinimalistClock(
                    timeFlow = previewTimeFlow,
                    clockStyle = style
                )
            }
        }
    }
}

@Composable
fun MaterialYouCard(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        content = content
    )
}
