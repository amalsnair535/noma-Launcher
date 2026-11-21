package com.freelauncher.app.ui.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freelauncher.app.data.db.*
import com.freelauncher.app.data.models.*
import com.freelauncher.app.data.repository.LauncherRepository
import com.freelauncher.app.data.service.*
import com.freelauncher.app.ui.components.ClockStyle
import com.freelauncher.app.ui.components.TimeCardHorizontalAlign
import com.freelauncher.app.ui.components.TimeCardVerticalAlign
import com.freelauncher.app.ui.theme.LauncherFont
import com.freelauncher.app.ui.theme.LauncherThemeMode
import com.freelauncher.app.ui.theme.LauncherWallpaper
import com.freelauncher.app.ui.util.SecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

enum class LauncherScreen {
    HOME,
    SIX_APPS,
    ALL_APPS,
    RSS_FEED,
    TIME_AWAY
}

data class LauncherUiState(
    val currentScreen: LauncherScreen = LauncherScreen.HOME,
    val clockStyle: ClockStyle = ClockStyle.LARGE_DIGITAL,
    val timeCardVAlign: TimeCardVerticalAlign = TimeCardVerticalAlign.CENTER,
    val timeCardHAlign: TimeCardHorizontalAlign = TimeCardHorizontalAlign.CENTER,
    val timeCardOffsetX: Float = 0f,
    val timeCardOffsetY: Float = 0f,
    val timeCardScale: Float = 1.0f,
    val fontFamily: LauncherFont = LauncherFont.MONOSPACE,
    val themeMode: LauncherThemeMode = LauncherThemeMode.OLED_BLACK,
    val wallpaperId: String = "paper_craft_sunset",
    val customWallpaperUri: String? = null,
    val wallpaperDim: Float = 0.25f,
    val showMonograms: Boolean = false,
    val customGreeting: String = "auto",
    val installedApps: List<AppItem> = emptyList(),
    val pinnedApps: List<AppItem> = emptyList(),
    val categories: List<AppCategoryInfo> = emptyList(),
    val categorizedApps: Map<AppCategory, List<AppItem>> = emptyMap(),
    val notes: List<NoteEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val feeds: List<RssFeedEntity> = emptyList(),
    val articles: List<RssArticleEntity> = emptyList(),
    val isSyncingRss: Boolean = false,
    val rssSyncMessage: String? = null,
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val focusTimerSecondsLeft: Int = 0,
    val isFocusTimerRunning: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val weeklyFocusHistory: List<FocusDayUsageData> = emptyList(),
    val timeAwayStats: TimeAwayStats? = null,
    val searchQuery: String = "",
    val selectedAppForPinning: AppItem? = null,
    val showMultiPinDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showCategoryManagerSheet: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val showRssManagerDialog: Boolean = false,
    val showWallpaperPicker: Boolean = false,
    val showAtmosphericCreator: Boolean = false,
    val customWallpapers: List<LauncherWallpaper> = emptyList(),
    val isClockEditMode: Boolean = false,
    val showGestureHints: Boolean = false,
    val showNewsFeed: Boolean = true,
    val showTimeAway: Boolean = true,
    val isPinnedOnlyLocked: Boolean = false,
    val pinnedLockFeedbackMessage: String? = null,
    val isBiometricLockEnabled: Boolean = false,
    val pendingBiometricUnlock: Boolean = false,
    val biometricAuthTrigger: Long = 0L,
    val lockMethod: String = "pin",
    val customPin: String = "",
    val pinSalt: String = "",
    val pinHash: String = "",
    val pinLockoutSeconds: Int = 0,
    val showSetPinDialog: Boolean = false,
    val showPinUnlockSheet: Boolean = false,
    val isSearchOnlyMode: Boolean = false,
    val showOnboardingGuide: Boolean = false,
    val showRearrangePinnedDialog: Boolean = false,
    val showAboutSheet: Boolean = false,
    val showBackupSheet: Boolean = false,
    val showWeatherBatteryGlance: Boolean = false,
    val temperatureUnit: String = "F",
    val enableSwipeDownSearch: Boolean = false,
    val enableScribbleSearch: Boolean = true,
    val scribbleSpeedMs: Int = 300,
    val enableMindfulPause: Boolean = false,
    val appPendingMindfulPause: AppItem? = null,
    val sixAppsScale: Float = 0.9f,
    val batteryLevel: Int = 100,
    val isBatteryCharging: Boolean = false,
    val weatherText: String? = null,
    val enableLandscapeMode: Boolean = false,
    val iconDisplayMode: IconDisplayMode = IconDisplayMode.TEXT_ONLY,
    val selectedIconPackPackage: String = "",
    val minimalIconPackConfig: MinimalIconPackConfig = MinimalIconPackConfig(),
    val installedIconPacks: List<IconPackInfo> = emptyList(),
    val showMinimalIconPackCreator: Boolean = false,
    val appDrawerStyle: AppDrawerStyle = AppDrawerStyle.NEO_MINIMAL,
    val sixAppsAlignment: SixAppsAlignment = SixAppsAlignment.LEFT,
    val allAppsAlignment: AllAppsAlignment = AllAppsAlignment.LEFT,
    val hideEverythingOnHome: Boolean = false,
    val isScribbleSearchMode: Boolean = false,
    val disableSixAppsView: Boolean = false,
    val homeCustomShortcut: String = "music",
    val showMusicPlayerOverlay: Boolean = false,
    val mediaPlaybackInfo: MediaPlaybackInfo = MediaPlaybackInfo()
) {
    val hasPinSet: Boolean
        get() = pinHash.isNotBlank()
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _scribbleSearchState = MutableStateFlow(com.freelauncher.app.ui.util.ScribbleSearchState())
    val scribbleSearchState: StateFlow<com.freelauncher.app.ui.util.ScribbleSearchState> = _scribbleSearchState.asStateFlow()
    private var scribbleRecognitionJob: Job? = null
    private var weatherJob: Job? = null
    private var lockoutJob: Job? = null
    private var failedPinAttempts = 0

    private val _currentTime = MutableStateFlow(Date())
    val currentTime: StateFlow<Date> = _currentTime.asStateFlow()

    private val weatherService = WeatherService(application)
    val iconPackManager = IconPackManager(application)
    val mediaSessionManager = NomaMediaSessionManager(application)

    init {
        startTimeTicker()
        observeMediaSession()
        loadSettings()
        observeDatabase()
        refreshApps()
        syncFeeds()
        refreshDigitalWellbeingStats()
        startBatteryAndWeatherMonitoring()
    }

    private fun observeMediaSession() {
        viewModelScope.launch {
            mediaSessionManager.playbackInfo.collect { info ->
                _uiState.update { it.copy(mediaPlaybackInfo = info) }
            }
        }
    }

    private fun startTimeTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                _currentTime.value = Date()
                delay(1000.milliseconds)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.allSettings.collect { settingsMap ->
                val clockStyleId = settingsMap["clock_style"] ?: "large_digital"
                val vAlignId = settingsMap["time_card_v_align"] ?: "center"
                val hAlignId = settingsMap["time_card_h_align"] ?: "center"
                val offsetX = settingsMap["time_card_offset_x"]?.toFloatOrNull() ?: 0f
                val offsetY = settingsMap["time_card_offset_y"]?.toFloatOrNull() ?: 0f
                val cardScale = settingsMap["time_card_scale"]?.toFloatOrNull() ?: 1.0f
                val fontId = settingsMap["font_family"] ?: "monospace"
                val themeId = settingsMap["theme_style"] ?: "oled_black"
                val rawWallpaperId = settingsMap["wallpaper_id"]
                val wallpaperId = if (rawWallpaperId == null || rawWallpaperId == "cyber_noir") "paper_craft_sunset" else rawWallpaperId
                val customWallpaperUri = settingsMap["custom_wallpaper_uri"]?.takeIf { it.isNotBlank() }
                val wallpaperDim = settingsMap["wallpaper_dim"]?.toFloatOrNull() ?: 0.25f
                val showMonograms = settingsMap["show_monograms"] == "true"
                val greeting = settingsMap["custom_greeting"] ?: "auto"
                val biometricLock = settingsMap["biometric_lock_enabled"] == "true"
                val gestureHints = settingsMap["show_gesture_hints"] == "true"
                val showNewsFeed = settingsMap["show_news_feed"] != "false"
                val showTimeAway = settingsMap["show_time_away"] != "false"
                val onboardingCompleted = settingsMap["onboarding_completed"] == "true"
                val showWeatherGlance = settingsMap["show_weather_battery_glance"] == "true"
                val tempUnit = settingsMap["temperature_unit"] ?: "F"
                val enableSwipeDownSearch = settingsMap["enable_swipe_down_search"] == "true"
                val enableScribbleSearch = settingsMap["enable_scribble_search"] != "false"
                val scribbleSpeedMs = settingsMap["scribble_speed_ms"]?.toIntOrNull()?.coerceIn(180, 550) ?: 300
                val enableMindfulPause = settingsMap["enable_mindful_pause"] == "true"
                val sixAppsScale = settingsMap["six_apps_scale"]?.toFloatOrNull() ?: 0.9f
                val lockMethod = settingsMap["lock_method"] ?: "pin"
                
                var pinSalt = settingsMap["pin_salt"] ?: ""
                var pinHash = settingsMap["pin_hash"] ?: ""
                val legacyPin = settingsMap["custom_launcher_pin"] ?: ""

                if (pinHash.isBlank() && legacyPin.isNotBlank()) {
                    val (mSalt, mHash) = SecurityHelper.createPinHashPair(legacyPin)
                    pinSalt = mSalt
                    pinHash = mHash
                    viewModelScope.launch {
                        repository.updateSetting("pin_salt", mSalt)
                        repository.updateSetting("pin_hash", mHash)
                        repository.updateSetting("custom_launcher_pin", "")
                    }
                }

                val enableLandscapeMode = settingsMap["enable_landscape_mode"] == "true"

                val iconDisplayMode = repository.getIconDisplayMode()
                val selectedIconPackPackage = repository.getSelectedIconPack()
                val minimalIconPackConfig = repository.getMinimalIconPackConfig()
                val installedIconPacks = withContext(Dispatchers.IO) { iconPackManager.getInstalledIconPacks() }
                val appDrawerStyle = repository.getAppDrawerStyle()
                val sixAppsAlignment = repository.getSixAppsAlignment()
                val allAppsAlignment = repository.getAllAppsAlignment()
                val hideEverythingOnHome = repository.getHideEverythingOnHome()

                val disableSixAppsView = repository.getDisableSixAppsView() || (settingsMap["disable_six_apps_view"] == "true")
                val homeCustomShortcut = repository.getHomeCustomShortcut()

                _uiState.update { state ->
                    state.copy(
                        clockStyle = ClockStyle.entries.find { it.id == clockStyleId } ?: ClockStyle.LARGE_DIGITAL,
                        timeCardVAlign = TimeCardVerticalAlign.entries.find { it.id == vAlignId } ?: TimeCardVerticalAlign.CENTER,
                        timeCardHAlign = TimeCardHorizontalAlign.entries.find { it.id == hAlignId } ?: TimeCardHorizontalAlign.CENTER,
                        timeCardOffsetX = offsetX,
                        timeCardOffsetY = offsetY,
                        timeCardScale = cardScale.coerceIn(0.5f, 2.0f),
                        fontFamily = LauncherFont.entries.find { it.id == fontId } ?: LauncherFont.MONOSPACE,
                        themeMode = LauncherThemeMode.entries.find { it.id == themeId } ?: LauncherThemeMode.OLED_BLACK,
                        wallpaperId = wallpaperId,
                        customWallpaperUri = customWallpaperUri,
                        wallpaperDim = wallpaperDim,
                        showMonograms = showMonograms,
                        customGreeting = greeting,
                        isBiometricLockEnabled = biometricLock,
                        lockMethod = lockMethod,
                        customPin = legacyPin,
                        pinSalt = pinSalt,
                        pinHash = pinHash,
                        showGestureHints = gestureHints,
                        showNewsFeed = showNewsFeed,
                        showTimeAway = showTimeAway,
                        showOnboardingGuide = !onboardingCompleted,
                        showWeatherBatteryGlance = showWeatherGlance,
                        temperatureUnit = tempUnit,
                        enableSwipeDownSearch = enableSwipeDownSearch,
                        enableScribbleSearch = enableScribbleSearch,
                        scribbleSpeedMs = scribbleSpeedMs,
                        enableMindfulPause = enableMindfulPause,
                        sixAppsScale = sixAppsScale.coerceIn(0.6f, 1.8f),
                        enableLandscapeMode = enableLandscapeMode,
                        iconDisplayMode = iconDisplayMode,
                        selectedIconPackPackage = selectedIconPackPackage,
                        minimalIconPackConfig = minimalIconPackConfig,
                        installedIconPacks = installedIconPacks,
                        appDrawerStyle = appDrawerStyle,
                        sixAppsAlignment = sixAppsAlignment,
                        allAppsAlignment = allAppsAlignment,
                        disableSixAppsView = disableSixAppsView,
                        hideEverythingOnHome = hideEverythingOnHome,
                        homeCustomShortcut = homeCustomShortcut,
                    )
                }
            }
        }
    }

    fun setLandscapeModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableLandscapeMode = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_landscape_mode", enabled.toString())
        }
    }

    fun setHomeCustomShortcut(shortcut: String) {
        _uiState.update { it.copy(homeCustomShortcut = shortcut) }
        viewModelScope.launch {
            repository.saveHomeCustomShortcut(shortcut)
        }
    }

    fun setShowMusicPlayerOverlay(show: Boolean) {
        if (show) {
            mediaSessionManager.updateMediaSession()
        }
        _uiState.update { it.copy(showMusicPlayerOverlay = show) }
    }

    fun toggleMediaPlayPause() {
        mediaSessionManager.togglePlayPause()
    }

    fun skipMediaNext() {
        mediaSessionManager.skipToNext()
    }

    fun skipMediaPrevious() {
        mediaSessionManager.skipToPrevious()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            repository.allNotes.collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }

        viewModelScope.launch {
            repository.allEvents.collect { events ->
                _uiState.update { it.copy(events = events) }
            }
        }

        viewModelScope.launch {
            repository.allFeeds.collect { feeds ->
                _uiState.update { it.copy(feeds = feeds) }
            }
        }

        viewModelScope.launch {
            repository.allArticles.collect { articles ->
                val seenKeys = mutableSetOf<String>()
                val distinctArticles = mutableListOf<RssArticleEntity>()
                for (article in articles) {
                    val cleanLink = article.link.trim().lowercase().removeSuffix("/")
                    val cleanTitle = article.title.lowercase().replace("[^a-z0-9]".toRegex(), "").take(35)
                    val key = cleanLink.ifBlank { cleanTitle }
                    if (key.isNotBlank() && !seenKeys.contains(key) && !seenKeys.contains(cleanTitle)) {
                        seenKeys.add(key)
                        seenKeys.add(cleanTitle)
                        distinctArticles.add(article)
                    }
                }
                _uiState.update { it.copy(articles = distinctArticles) }
            }
        }

        viewModelScope.launch {
            repository.allFocusSessions.collect { sessions ->
                _uiState.update { it.copy(focusSessions = sessions) }
                refreshDigitalWellbeingStats()
            }
        }

        viewModelScope.launch {
            repository.allCustomWallpapers.collect { entities ->
                val wallpapers = entities.map { LauncherWallpaper.fromEntity(it) }
                _uiState.update { it.copy(customWallpapers = wallpapers) }
            }
        }
    }

    private data class ProcessedAppsData(
        val updatedApps: List<AppItem>,
        val pinnedList: List<AppItem>,
        val userCategories: List<AppCategoryInfo>,
        val categorizedApps: Map<AppCategory, List<AppItem>>
    )

    fun refreshApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val apps = repository.getInstalledApps(forceRefresh)
            val pinnedIds = repository.getPinnedIds()

            val processed = withContext(Dispatchers.Default) {
                val installedIds = apps.asSequence().map { it.id }.toSet()

                val pinnedList = mutableListOf<AppItem>()
                val validPinnedIds = mutableListOf<String>()

                for (id in pinnedIds) {
                    if (pinnedList.size >= 6) break
                    val found = apps.find { it.id == id }
                    if ((found != null) && (!pinnedList.any { it.id == id })) {
                        pinnedList.add(found.copy(isPinned = true, pinIndex = pinnedList.size))
                        validPinnedIds.add(id)
                    }
                }

                if (pinnedIds.isNotEmpty() && (pinnedIds.size != validPinnedIds.size) && installedIds.isNotEmpty()) {
                    repository.setPinnedIds(validPinnedIds)
                }

                val updatedApps = apps.map { app ->
                    val pinIdx = validPinnedIds.indexOf(app.id)
                    if (pinIdx != -1) app.copy(isPinned = true, pinIndex = pinIdx)
                    else app.copy(isPinned = false, pinIndex = -1)
                }

                val userCategories = repository.getUserCategories()
                val categorized = updatedApps.groupBy { it.category }

                ProcessedAppsData(updatedApps, pinnedList, userCategories, categorized)
            }

            _uiState.update {
                it.copy(
                    installedApps = processed.updatedApps,
                    pinnedApps = processed.pinnedList,
                    categories = processed.userCategories,
                    categorizedApps = processed.categorizedApps,
                )
            }
        }
    }

    fun setCategoryManagerSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showCategoryManagerSheet = visible) }
    }

    fun setAddCategoryDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddCategoryDialog = visible) }
    }

    fun addCategory(title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                repository.addCustomCategory(title)
                refreshApps()
            }
        }
    }

    fun renameCategory(categoryId: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.renameCategory(categoryId, newTitle)
                refreshApps()
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            refreshApps()
        }
    }

    fun toggleCategoryVisibility(categoryId: String) {
        viewModelScope.launch {
            repository.toggleCategoryVisibility(categoryId)
            refreshApps()
        }
    }

    fun reorderCategories(reorderedIds: List<String>) {
        viewModelScope.launch {
            repository.reorderCategories(reorderedIds)
            refreshApps()
        }
    }

    fun moveCategoryUp(categoryId: String) {
        val current = _uiState.value.categories
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx > 0) {
            val list = current.toMutableList()
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            reorderCategories(list.map { it.id })
        }
    }

    fun moveCategoryDown(categoryId: String) {
        val current = _uiState.value.categories
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx in (0 until (current.size - 1))) {
            val list = current.toMutableList()
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            reorderCategories(list.map { it.id })
        }
    }

    fun setAppCategory(packageName: String, categoryId: String) {
        viewModelScope.launch {
            repository.setAppCategory(packageName, categoryId)
            refreshApps()
        }
    }

    fun resetCategoriesToDefault() {
        viewModelScope.launch {
            repository.resetCategoriesToDefault()
            refreshApps()
        }
    }

    fun setDisableSixAppsView(disabled: Boolean) {
        _uiState.update { it.copy(disableSixAppsView = disabled) }
        viewModelScope.launch {
            repository.saveDisableSixAppsView(disabled)
        }
    }

    fun navigateTo(screen: LauncherScreen) {
        val targetScreen = if (_uiState.value.disableSixAppsView && screen == LauncherScreen.SIX_APPS) {
            LauncherScreen.ALL_APPS
        } else {
            screen
        }

        if (_uiState.value.isPinnedOnlyLocked && (targetScreen != LauncherScreen.SIX_APPS)) {
            _uiState.update { it.copy(pinnedLockFeedbackMessage = "UltraFocus Mode Locked • Triple-tap to exit") }
            return
        }
        val leavingAllApps = _uiState.value.currentScreen == LauncherScreen.ALL_APPS && targetScreen != LauncherScreen.ALL_APPS
        if (leavingAllApps) {
            _uiState.update { it.copy(currentScreen = targetScreen) }
            viewModelScope.launch {
                delay(220.milliseconds)
                if (_uiState.value.currentScreen != LauncherScreen.ALL_APPS) {
                    _uiState.update {
                        it.copy(
                            isSearchOnlyMode = false,
                            isScribbleSearchMode = false,
                            searchQuery = ""
                        )
                    }
                }
            }
        } else {
            _uiState.update { 
                it.copy(
                    currentScreen = targetScreen,
                    isSearchOnlyMode = if (targetScreen == LauncherScreen.ALL_APPS) it.isSearchOnlyMode else false,
                    isScribbleSearchMode = if (targetScreen == LauncherScreen.ALL_APPS) it.isScribbleSearchMode else false,
                    searchQuery = if (targetScreen == LauncherScreen.ALL_APPS) it.searchQuery else ""
                ) 
            }
        }
    }

    private var pendingSearchUnlock: Boolean = false
    private var pendingDisableBiometric: Boolean = false
    private var pendingSwitchLockMethod: String? = null

    fun openSearchFromHome() {
        if (_uiState.value.isPinnedOnlyLocked) {
            _uiState.update { it.copy(pinnedLockFeedbackMessage = "UltraFocus Mode Locked • Triple-tap to exit") }
            return
        }
        if (_uiState.value.isBiometricLockEnabled) {
            pendingSearchUnlock = true
            pendingDisableBiometric = false
            if (_uiState.value.lockMethod == "pin") {
                if (!_uiState.value.hasPinSet) {
                    _uiState.update { it.copy(showSetPinDialog = true) }
                } else {
                    _uiState.update { it.copy(showPinUnlockSheet = true) }
                }
            } else {
                _uiState.update { it.copy(pendingBiometricUnlock = true, biometricAuthTrigger = it.biometricAuthTrigger + 1) }
            }
        } else {
            _uiState.update {
                it.copy(
                    isSearchOnlyMode = true,
                    isScribbleSearchMode = false,
                    searchQuery = "",
                    currentScreen = LauncherScreen.ALL_APPS,
                )
            }
        }
    }

    fun openSearchWithQuery(query: String) {
        if (_uiState.value.isPinnedOnlyLocked) {
            _uiState.update { it.copy(pinnedLockFeedbackMessage = "UltraFocus Mode Locked • Triple-tap to exit") }
            return
        }
        if (_uiState.value.isBiometricLockEnabled) {
            pendingSearchUnlock = true
            pendingDisableBiometric = false
            _uiState.update { it.copy(searchQuery = query, isScribbleSearchMode = true) }
            if (_uiState.value.lockMethod == "pin") {
                if (!_uiState.value.hasPinSet) {
                    _uiState.update { it.copy(showSetPinDialog = true) }
                } else {
                    _uiState.update { it.copy(showPinUnlockSheet = true) }
                }
            } else {
                _uiState.update { it.copy(pendingBiometricUnlock = true, biometricAuthTrigger = it.biometricAuthTrigger + 1) }
            }
        } else {
            _uiState.update {
                it.copy(
                    isSearchOnlyMode = true,
                    isScribbleSearchMode = true,
                    searchQuery = query,
                    currentScreen = LauncherScreen.ALL_APPS,
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { 
            it.copy(
                searchQuery = query,
                isScribbleSearchMode = if (query.isBlank()) false else it.isScribbleSearchMode
            ) 
        }
    }

    fun openPinDialog(app: AppItem) {
        _uiState.update { it.copy(selectedAppForPinning = app) }
    }

    fun closePinDialog() {
        _uiState.update { it.copy(selectedAppForPinning = null) }
    }

    fun setMultiPinDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showMultiPinDialog = visible) }
    }

    fun setRearrangePinnedDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showRearrangePinnedDialog = visible) }
    }

    fun movePinnedAppUp(appId: String) {
        val currentPinned = _uiState.value.pinnedApps.map { it.id }.toMutableList()
        val idx = currentPinned.indexOf(appId)
        if (idx > 0) {
            val item = currentPinned.removeAt(idx)
            currentPinned.add(idx - 1, item)
            updatePinnedApps(currentPinned)
        }
    }

    fun movePinnedAppDown(appId: String) {
        val currentPinned = _uiState.value.pinnedApps.map { it.id }.toMutableList()
        val idx = currentPinned.indexOf(appId)
        if (idx in 0 until (currentPinned.size - 1)) {
            val item = currentPinned.removeAt(idx)
            currentPinned.add(idx + 1, item)
            updatePinnedApps(currentPinned)
        }
    }

    fun updatePinnedApps(appIds: List<String>) {
        viewModelScope.launch {
            repository.setPinnedIds(appIds)
            refreshApps()
        }
    }

    fun pinApp(app: AppItem) {
        viewModelScope.launch {
            repository.pinApp(app.id)
            refreshApps()
        }
    }

    fun unpinApp(app: AppItem) {
        viewModelScope.launch {
            repository.unpinApp(app.id)
            refreshApps()
        }
    }

    fun launchApp(context: Context, app: AppItem) {
        if (_uiState.value.enableMindfulPause && isDistractingApp(app)) {
            _uiState.update { it.copy(appPendingMindfulPause = app) }
            return
        }
        executeAppLaunch(context, app)
    }

    fun confirmMindfulPause(context: Context) {
        val app = _uiState.value.appPendingMindfulPause
        _uiState.update { it.copy(appPendingMindfulPause = null) }
        if (app != null) {
            executeAppLaunch(context, app)
        }
    }

    fun cancelMindfulPause() {
        _uiState.update { it.copy(appPendingMindfulPause = null) }
    }

    private fun isDistractingApp(app: AppItem): Boolean {
        val catId = app.categoryId.uppercase()
        val catName = app.category.name.uppercase()
        val isDistractingCategory = listOf("SOCIAL", "GAMES", "MEDIA", "ENTERTAINMENT", "SHOPPING")
        return isDistractingCategory.contains(catId) || isDistractingCategory.contains(catName)
    }

    private fun executeAppLaunch(context: Context, app: AppItem) {
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager

            if (app.isDualApp && launcherApps != null && userManager != null) {
                val targetUser = userManager.getUserForSerialNumber(app.userSerialNumber)
                if (targetUser != null) {
                    val componentName = if (app.activityName.isNotBlank()) {
                        android.content.ComponentName(app.packageName, app.activityName)
                    } else {
                        val activities = launcherApps.getActivityList(app.packageName, targetUser)
                        activities.firstOrNull()?.componentName
                    }
                    if (componentName != null) {
                        launcherApps.startMainActivity(componentName, targetUser, null, null)
                        return
                    }
                }
            }

            val pm = context.packageManager
            var intent: Intent? = null

            if (app.activityName.isNotBlank()) {
                val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(app.packageName, app.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (explicitIntent.resolveActivity(pm) != null) {
                    intent = explicitIntent
                }
            }

            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(app.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
            } else {
                android.widget.Toast.makeText(context, "${app.label} is not installed", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            com.freelauncher.app.ui.util.SafeCrashlytics.logException(e)
            android.widget.Toast.makeText(context, "Could not open ${app.label}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun setBackupSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showBackupSheet = visible) }
    }

    fun exportBackupJson(): String {
        val state = _uiState.value
        val json = org.json.JSONObject().apply {
            put("wallpaper_id", state.wallpaperId)
            put("theme_mode", state.themeMode.name)
            put("font_family", state.fontFamily.name)
            put("clock_style", state.clockStyle.name)
            put("show_monograms", state.showMonograms)
            put("six_apps_scale", state.sixAppsScale)
            put("pinned_app_ids", org.json.JSONArray(state.pinnedApps.map { it.id }))
        }
        return json.toString(2)
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            if (json.has("wallpaper_id")) setWallpaper(json.getString("wallpaper_id"))
            if (json.has("font_family")) setFontFamily(LauncherFont.valueOf(json.getString("font_family")))
            if (json.has("clock_style")) setClockStyle(ClockStyle.valueOf(json.getString("clock_style")))
            if (json.has("show_monograms")) setShowMonograms(json.getBoolean("show_monograms"))
            if (json.has("six_apps_scale")) setSixAppsScale(json.getDouble("six_apps_scale").toFloat())
            if (json.has("pinned_app_ids")) {
                val arr = json.getJSONArray("pinned_app_ids")
                val ids = (0 until arr.length()).map { arr.getString(it) }
                updatePinnedApps(ids)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openWebUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val uri = Uri.parse(url.trim())
            val scheme = uri.scheme?.lowercase(java.util.Locale.ROOT) ?: ""

            val allowedSchemes = setOf("https", "http", "market", "mailto")
            if (scheme !in allowedSchemes) {
                android.util.Log.w("LauncherVM", "Rejected launching URI with untrusted scheme: $scheme")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            android.util.Log.w("LauncherVM", "No activity found to handle URI: $url")
            android.widget.Toast.makeText(context, "No app available to handle link", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("LauncherVM", "Error opening web URL: %s", e)
            com.freelauncher.app.ui.util.SafeCrashlytics.logException(e)
        }
    }

    fun openDialerApp(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Could not open dialer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun openClockApp(context: Context) {
        val clockIntents = listOf(
            Intent(AlarmClock.ACTION_SHOW_ALARMS),
            Intent(AlarmClock.ACTION_SET_ALARM),
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = android.content.ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock")
            },
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = android.content.ComponentName("com.android.deskclock", "com.android.deskclock.DeskClock")
            },
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = android.content.ComponentName("com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage")
            }
        )

        for (intent in clockIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next fallback
            }
        }

        val pm = context.packageManager
        val packages = listOf("com.google.android.deskclock", "com.android.deskclock", "com.sec.android.app.clockpackage", "com.coloros.alarmclock", "com.asus.clock")
        for (pkg in packages) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: Exception) { }
            }
        }

        android.widget.Toast.makeText(context, "Could not open Clock app", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun openCalendarApp(context: Context) {
        val builder = "content://com.android.calendar/time".toUri().buildUpon()
        ContentUris.appendId(builder, System.currentTimeMillis())
        val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
            data = builder.build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(calendarIntent)
            return
        } catch (_: Exception) {
            val pm = context.packageManager
            val packages = listOf("com.google.android.calendar", "com.android.calendar", "com.samsung.android.calendar")
            for (pkg in packages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(launchIntent)
                        return
                    } catch (_: Exception) { }
                }
            }
        }

        android.widget.Toast.makeText(context, "Could not open Calendar app", android.widget.Toast.LENGTH_SHORT).show()
    }

    // RSS / News
    fun syncFeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingRss = true, rssSyncMessage = "Fetching latest articles...") }
            val count = repository.syncRssFeeds()
            _uiState.update {
                it.copy(
                    isSyncingRss = false,
                    rssSyncMessage = if (count > 0) "Synced $count articles" else "Up to date",
                )
            }
            delay(3000.milliseconds)
            _uiState.update { it.copy(rssSyncMessage = null) }
        }
    }

    fun addFeed(title: String, url: String) {
        viewModelScope.launch {
            repository.addFeed(title, url)
        }
    }

    fun toggleFeed(feed: RssFeedEntity) {
        viewModelScope.launch {
            repository.toggleFeed(feed)
            syncFeeds()
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            repository.deleteFeed(feedId)
        }
    }

    // Digital Wellbeing & Focus Stats
    fun refreshDigitalWellbeingStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val hasPerm = DigitalWellbeingService.hasUsagePermission(app)
            val stats = DigitalWellbeingService.getWeeklyStats(app, _uiState.value.focusSessions)
            val timeAway = DigitalWellbeingService.getTimeAwayStats(app, _uiState.value.focusSessions)
            _uiState.update {
                it.copy(
                    hasUsagePermission = hasPerm,
                    weeklyFocusHistory = stats,
                    timeAwayStats = timeAway,
                )
            }
        }
    }

    fun openUsageAccessSettings(context: Context) {
        DigitalWellbeingService.openUsageSettings(context)
    }

    // Settings & Time Card Controls
    fun setClockStyle(style: ClockStyle) {
        viewModelScope.launch {
            _uiState.update { it.copy(clockStyle = style) }
            repository.updateSetting("clock_style", style.id)
        }
    }

    fun setTimeCardScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 2.0f)
        viewModelScope.launch {
            _uiState.update { it.copy(timeCardScale = clamped) }
            repository.updateSetting("time_card_scale", clamped.toString())
        }
    }

    fun setClockEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isClockEditMode = enabled) }
    }

    fun setFontFamily(font: LauncherFont) {
        viewModelScope.launch {
            _uiState.update { it.copy(fontFamily = font) }
            repository.updateSetting("font_family", font.id)
        }
    }

    fun setThemeMode(mode: LauncherThemeMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(themeMode = mode) }
            repository.updateSetting("theme_style", mode.id)
        }
    }

    fun setShowMonograms(show: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(showMonograms = show) }
            repository.updateSetting("show_monograms", show.toString())
        }
    }

    fun setCustomGreeting(greeting: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(customGreeting = greeting) }
            repository.updateSetting("custom_greeting", greeting)
        }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun setRssManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(showRssManagerDialog = visible) }
    }

    fun setWallpaper(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(wallpaperId = id) }
            repository.updateSetting("wallpaper_id", id)
        }
    }

    fun setWallpaperDim(dim: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(wallpaperDim = dim) }
            repository.updateSetting("wallpaper_dim", dim.toString())
        }
    }

    fun setWallpaperPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showWallpaperPicker = visible) }
    }

    fun setAtmosphericCreatorVisible(visible: Boolean) {
        _uiState.update { it.copy(showAtmosphericCreator = visible) }
    }

    fun saveAtmosphericTheme(name: String, colors: List<Long>, isDark: Boolean) {
        viewModelScope.launch {
            val newId = repository.saveCustomWallpaper(name, colors, isDark)
            setAtmosphericCreatorVisible(false)
            setWallpaperPickerVisible(false)
            if (newId > 0) {
                setWallpaper("custom_theme_$newId")
            }
        }
    }

    fun deleteAtmosphericTheme(id: String) {
        val longId = id.removePrefix("custom_theme_").toLongOrNull()
        if (longId != null) {
            viewModelScope.launch {
                repository.deleteCustomWallpaper(longId)
                if (_uiState.value.wallpaperId == id) {
                    setWallpaper("paper_craft_sunset")
                }
            }
        }
    }

    fun saveCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.filesDir.listFiles { file -> file.name.startsWith("custom_wallpaper_") }?.forEach { it.delete() }

                val fileName = "custom_wallpaper_${System.currentTimeMillis()}.jpg"
                val destinationFile = java.io.File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val savedPath = destinationFile.absolutePath
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            wallpaperId = "custom_gallery",
                            customWallpaperUri = savedPath,
                        )
                    }
                }
                repository.updateSetting("wallpaper_id", "custom_gallery")
                repository.updateSetting("custom_wallpaper_uri", savedPath)
            } catch (e: Exception) {
                com.freelauncher.app.ui.util.SafeCrashlytics.logException(e)
            }
        }
    }

    fun removeCustomWallpaper() {
        val currentUri = _uiState.value.customWallpaperUri
        viewModelScope.launch(Dispatchers.IO) {
            if (!currentUri.isNullOrBlank()) {
                try {
                    val file = java.io.File(currentUri)
                    if (file.exists() && file.name.startsWith("custom_wallpaper_")) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        wallpaperId = "paper_craft_sunset",
                        customWallpaperUri = null,
                    )
                }
            }
            repository.updateSetting("wallpaper_id", "paper_craft_sunset")
            repository.updateSetting("custom_wallpaper_uri", "")
        }
    }

    // Biometric / Fingerprint & PIN Lock
    fun setBiometricLockEnabled(enabled: Boolean) {
        if (!enabled && _uiState.value.isBiometricLockEnabled) {
            pendingDisableBiometric = true
            pendingSearchUnlock = false
            if (_uiState.value.lockMethod == "pin") {
                if (!_uiState.value.hasPinSet) {
                    _uiState.update { it.copy(showSetPinDialog = true) }
                } else {
                    _uiState.update { it.copy(showPinUnlockSheet = true) }
                }
            } else {
                _uiState.update { it.copy(pendingBiometricUnlock = true, biometricAuthTrigger = it.biometricAuthTrigger + 1) }
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isBiometricLockEnabled = enabled) }
                repository.updateSetting("biometric_lock_enabled", enabled.toString())
            }
        }
    }

    fun setLockMethod(method: String) {
        if (method == "biometric" && _uiState.value.lockMethod == "pin" && _uiState.value.isBiometricLockEnabled && _uiState.value.hasPinSet) {
            pendingSwitchLockMethod = "biometric"
            pendingDisableBiometric = false
            pendingSearchUnlock = false
            _uiState.update { it.copy(showPinUnlockSheet = true) }
        } else {
            _uiState.update { it.copy(lockMethod = method) }
            viewModelScope.launch {
                repository.updateSetting("lock_method", method)
            }
        }
    }

    fun setCustomPin(pin: String) {
        viewModelScope.launch {
            val (salt, hash) = SecurityHelper.createPinHashPair(pin)
            _uiState.update {
                it.copy(
                    customPin = "",
                    pinSalt = salt,
                    pinHash = hash,
                    showSetPinDialog = false
                )
            }
            repository.updateSetting("pin_salt", salt)
            repository.updateSetting("pin_hash", hash)
            repository.updateSetting("custom_launcher_pin", "")
            if (!_uiState.value.isBiometricLockEnabled) {
                setBiometricLockEnabled(true)
            }
        }
    }

    fun verifyEnteredPin(enteredPin: String): Boolean {
        if (_uiState.value.pinLockoutSeconds > 0) return false
        val isCorrect = SecurityHelper.verifyPin(
            enteredPin = enteredPin,
            saltHex = _uiState.value.pinSalt,
            targetHashHex = _uiState.value.pinHash
        )
        if (isCorrect) {
            failedPinAttempts = 0
            return true
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= 5) {
                startPinLockout(30)
            }
            return false
        }
    }

    private fun startPinLockout(seconds: Int) {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _uiState.update { it.copy(pinLockoutSeconds = remaining) }
                delay(1000.milliseconds)
                remaining--
            }
            _uiState.update { it.copy(pinLockoutSeconds = 0) }
            failedPinAttempts = 0
        }
    }

    fun onBiometricUnavailableFallback() {
        if (_uiState.value.hasPinSet) {
            _uiState.update { it.copy(pendingBiometricUnlock = false, showPinUnlockSheet = true) }
        } else {
            _uiState.update { it.copy(pendingBiometricUnlock = false) }
        }
    }

    fun setSetPinDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSetPinDialog = visible) }
    }

    fun setPinUnlockSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showPinUnlockSheet = visible) }
    }

    fun onBiometricPromptHandled() {
        pendingSearchUnlock = false
        pendingDisableBiometric = false
        pendingSwitchLockMethod = null
        _uiState.update { it.copy(pendingBiometricUnlock = false, showPinUnlockSheet = false) }
    }

    fun setOnboardingGuideVisible(visible: Boolean) {
        _uiState.update { it.copy(showOnboardingGuide = visible) }
    }

    fun completeOnboarding() {
        _uiState.update { it.copy(showOnboardingGuide = false) }
        viewModelScope.launch {
            repository.updateSetting("onboarding_completed", "true")
        }
    }

    fun openDefaultLauncherChooser(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? android.app.role.RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                    if (context is android.app.Activity) {
                        context.startActivityForResult(intent, 1001)
                        return
                    }
                }
            }
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun requestAccessToAllApps() {
        if (_uiState.value.isBiometricLockEnabled) {
            pendingSearchUnlock = false
            pendingDisableBiometric = false
            if (_uiState.value.lockMethod == "pin") {
                if (!_uiState.value.hasPinSet) {
                    _uiState.update { it.copy(showSetPinDialog = true) }
                } else {
                    _uiState.update { it.copy(showPinUnlockSheet = true) }
                }
            } else {
                _uiState.update { it.copy(pendingBiometricUnlock = true, biometricAuthTrigger = it.biometricAuthTrigger + 1) }
            }
        } else {
            _uiState.update { it.copy(currentScreen = LauncherScreen.ALL_APPS) }
        }
    }

    fun unlockAllApps() {
        if (pendingSwitchLockMethod != null) {
            val targetMethod = pendingSwitchLockMethod!!
            pendingSwitchLockMethod = null
            pendingDisableBiometric = false
            pendingSearchUnlock = false
            _uiState.update {
                it.copy(
                    pendingBiometricUnlock = false,
                    showPinUnlockSheet = false,
                    lockMethod = targetMethod,
                )
            }
            viewModelScope.launch {
                repository.updateSetting("lock_method", targetMethod)
            }
        } else if (pendingDisableBiometric) {
            pendingDisableBiometric = false
            pendingSearchUnlock = false
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        pendingBiometricUnlock = false,
                        showPinUnlockSheet = false,
                        isBiometricLockEnabled = false,
                    )
                }
                repository.updateSetting("biometric_lock_enabled", "false")
            }
        } else {
            val wasSearchUnlock = pendingSearchUnlock
            pendingSearchUnlock = false
            _uiState.update {
                it.copy(
                    pendingBiometricUnlock = false,
                    showPinUnlockSheet = false,
                    isSearchOnlyMode = wasSearchUnlock,
                    searchQuery = if (wasSearchUnlock) "" else it.searchQuery,
                    currentScreen = LauncherScreen.ALL_APPS,
                )
            }
        }
    }

    fun setTimeCardOffset(x: Float, y: Float) {
        _uiState.update { it.copy(timeCardOffsetX = x, timeCardOffsetY = y) }
        viewModelScope.launch {
            repository.updateSetting("time_card_offset_x", x.toString())
            repository.updateSetting("time_card_offset_y", y.toString())
        }
    }

    fun resetTimeCardOffset() {
        setTimeCardOffset(0f, 0f)
    }

    fun setShowGestureHints(show: Boolean) {
        _uiState.update { it.copy(showGestureHints = show) }
        viewModelScope.launch {
            repository.updateSetting("show_gesture_hints", show.toString())
        }
    }

    fun setShowNewsFeed(show: Boolean) {
        _uiState.update { it.copy(showNewsFeed = show) }
        viewModelScope.launch {
            repository.updateSetting("show_news_feed", show.toString())
        }
    }

    fun setShowTimeAway(show: Boolean) {
        _uiState.update { it.copy(showTimeAway = show) }
        viewModelScope.launch {
            repository.updateSetting("show_time_away", show.toString())
        }
    }

    fun setHideEverythingOnHome(hide: Boolean) {
        _uiState.update { it.copy(hideEverythingOnHome = hide) }
        viewModelScope.launch {
            repository.saveHideEverythingOnHome(hide)
        }
    }

    fun setSwipeDownSearchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableSwipeDownSearch = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_swipe_down_search", enabled.toString())
        }
    }

    fun setScribbleSearchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableScribbleSearch = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_scribble_search", enabled.toString())
        }
        if (!enabled) {
            clearScribbleSearch()
        }
    }

    fun setScribbleSpeedMs(speedMs: Int) {
        val clamped = speedMs.coerceIn(180, 550)
        _uiState.update { it.copy(scribbleSpeedMs = clamped) }
        viewModelScope.launch {
            repository.updateSetting("scribble_speed_ms", clamped.toString())
        }
    }

    fun onScribbleStrokeStarted(point: com.freelauncher.app.ui.util.ScribblePoint) {
        _scribbleSearchState.update { state ->
            state.copy(currentStroke = com.freelauncher.app.ui.util.ScribbleStroke(listOf(point)))
        }
    }

    fun onScribbleStrokeUpdated(point: com.freelauncher.app.ui.util.ScribblePoint) {
        _scribbleSearchState.update { state ->
            val curr = state.currentStroke
            if (curr != null) {
                state.copy(currentStroke = com.freelauncher.app.ui.util.ScribbleStroke(curr.points + point))
            } else {
                state.copy(currentStroke = com.freelauncher.app.ui.util.ScribbleStroke(listOf(point)))
            }
        }
    }

    fun onScribbleStrokeCompleted(
        points: List<com.freelauncher.app.ui.util.ScribblePoint>,
        action: com.freelauncher.app.ui.util.GesturalAction,
        recognizer: com.freelauncher.app.data.service.ScribbleRecognizer
    ) {
        _scribbleSearchState.update { it.copy(currentStroke = null) }

        when (action) {
            com.freelauncher.app.ui.util.GesturalAction.SWIPE_LEFT -> {
                onBackspaceScribble()
            }
            com.freelauncher.app.ui.util.GesturalAction.HANDWRITING_STROKE -> {
                val newStroke = com.freelauncher.app.ui.util.ScribbleStroke(points)
                val currentStrokes = _scribbleSearchState.value.strokes
                val updatedStrokes = currentStrokes + newStroke
                val nextRequestId = _scribbleSearchState.value.requestId + 1
                _scribbleSearchState.update {
                    it.copy(
                        strokes = updatedStrokes,
                        requestId = nextRequestId,
                        isRecognizing = true
                    )
                }

                scribbleRecognitionJob?.cancel()
                scribbleRecognitionJob = viewModelScope.launch {
                    delay(_uiState.value.scribbleSpeedMs.milliseconds)
                    val candidates = recognizer.recognizeStrokes(updatedStrokes)
                    if (_scribbleSearchState.value.requestId == nextRequestId) {
                        _scribbleSearchState.update { it.copy(isRecognizing = false) }
                        if (candidates.isNotEmpty()) {
                            val resolvedCandidate = com.freelauncher.app.data.service.ScribbleRecognizer.resolveCandidate(
                                candidates = candidates,
                                isMultiStroke = updatedStrokes.size > 1
                            )
                            if (resolvedCandidate.isNotBlank()) {
                                val currQuery = _scribbleSearchState.value.recognizedQuery
                                val nextQuery = if (currQuery.isNotEmpty()) {
                                    val combined = currQuery + resolvedCandidate
                                    val combinedMatches = com.freelauncher.app.data.service.InstalledAppMatcher.matchAppsWithHighlight(
                                        installedApps = _uiState.value.installedApps,
                                        query = combined,
                                        isScribbleSearch = true
                                    )
                                    if (combinedMatches.isNotEmpty()) combined else resolvedCandidate
                                } else {
                                    resolvedCandidate
                                }
                                updateScribbleQuery(nextQuery)
                            } else {
                                _scribbleSearchState.update { it.copy(strokes = emptyList(), currentStroke = null) }
                            }
                        } else {
                            _scribbleSearchState.update { it.copy(strokes = emptyList(), currentStroke = null) }
                        }
                    }
                }
            }
            else -> { /* Tap or linear navigation swipe */ }
        }
    }

    fun onBackspaceScribble() {
        val currQuery = _scribbleSearchState.value.recognizedQuery
        if (currQuery.isNotEmpty()) {
            val updatedQuery = currQuery.dropLast(1)
            updateScribbleQuery(updatedQuery)
        } else {
            clearScribbleSearch()
        }
    }

    fun updateScribbleQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            clearScribbleSearch()
            return
        }
        val matchingApps = com.freelauncher.app.data.service.InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = _uiState.value.installedApps,
            query = cleanQuery,
            isScribbleSearch = true
        )
        _scribbleSearchState.update {
            it.copy(
                recognizedQuery = cleanQuery,
                matchingApps = matchingApps,
                strokes = emptyList(),
                currentStroke = null
            )
        }
    }

    fun clearScribbleSearch() {
        scribbleRecognitionJob?.cancel()
        _scribbleSearchState.update {
            com.freelauncher.app.ui.util.ScribbleSearchState()
        }
    }

    fun setMindfulPauseEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableMindfulPause = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_mindful_pause", enabled.toString())
        }
    }

    fun checkForUpdates(context: Context) {
        val updateManager = UpdateManager(context)
        updateManager.checkForUpdate {
            android.widget.Toast.makeText(context, "A new update is available on the Play Store!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun setSixAppsScale(scale: Float) {
        val clampedScale = scale.coerceIn(0.6f, 1.8f)
        _uiState.update { it.copy(sixAppsScale = clampedScale) }
        viewModelScope.launch {
            repository.updateSetting("six_apps_scale", clampedScale.toString())
        }
    }

    fun setAboutSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showAboutSheet = visible) }
    }

    fun setIconDisplayMode(mode: IconDisplayMode) {
        _uiState.update { it.copy(iconDisplayMode = mode) }
        viewModelScope.launch {
            repository.saveIconDisplayMode(mode)
        }
    }

    fun setAppDrawerStyle(style: AppDrawerStyle) {
        _uiState.update { it.copy(appDrawerStyle = style) }
        viewModelScope.launch {
            repository.saveAppDrawerStyle(style)
        }
    }

    fun setSixAppsAlignment(alignment: SixAppsAlignment) {
        _uiState.update { it.copy(sixAppsAlignment = alignment) }
        viewModelScope.launch {
            repository.saveSixAppsAlignment(alignment)
        }
    }

    fun setAllAppsAlignment(alignment: AllAppsAlignment) {
        _uiState.update { it.copy(allAppsAlignment = alignment) }
        viewModelScope.launch {
            repository.saveAllAppsAlignment(alignment)
        }
    }

    fun setSelectedIconPack(packageName: String) {
        _uiState.update { it.copy(selectedIconPackPackage = packageName) }
        viewModelScope.launch {
            repository.saveSelectedIconPack(packageName)
        }
    }

    fun saveMinimalIconPackConfig(config: MinimalIconPackConfig) {
        _uiState.update { it.copy(minimalIconPackConfig = config, showMinimalIconPackCreator = false) }
        viewModelScope.launch {
            repository.saveMinimalIconPackConfig(config)
        }
    }

    fun setShowMinimalIconPackCreator(show: Boolean) {
        _uiState.update { it.copy(showMinimalIconPackCreator = show) }
    }

    private fun startBatteryAndWeatherMonitoring() {
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val app = getApplication<Application>()
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    intent?.let { updateBatteryState(it) }
                }
            }
            val batteryStatus: Intent? = androidx.core.content.ContextCompat.registerReceiver(
                app,
                receiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
            batteryStatus?.let { updateBatteryState(it) }
        } catch (e: Exception) {
            com.freelauncher.app.ui.util.SafeCrashlytics.logException(e)
        }

        updateWeatherMonitoringState()
    }

    private fun updateWeatherMonitoringState() {
        weatherJob?.cancel()
        if (_uiState.value.showWeatherBatteryGlance) {
            weatherJob = viewModelScope.launch {
                while (isActive) {
                    refreshWeather()
                    delay(1800000.milliseconds)
                }
            }
        }
    }

    private fun updateBatteryState(intent: Intent) {
        val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                status == android.os.BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        _uiState.update { it.copy(batteryLevel = pct, isBatteryCharging = isCharging) }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val weather = weatherService.fetchCurrentWeather()
            if (weather != null) {
                val tempStr = if (_uiState.value.temperatureUnit == "C") {
                    "${weather.tempC.roundToInt()}°C"
                } else {
                    "${weather.tempF.roundToInt()}°F"
                }
                _uiState.update { it.copy(weatherText = "$tempStr ${weather.condition}") }
            }
        }
    }

    fun toggleWeatherBatteryGlance(enabled: Boolean) {
        _uiState.update { it.copy(showWeatherBatteryGlance = enabled) }
        viewModelScope.launch {
            repository.updateSetting("show_weather_battery_glance", enabled.toString())
        }
        updateWeatherMonitoringState()
    }

    fun setTemperatureUnit(unit: String) {
        _uiState.update { it.copy(temperatureUnit = unit) }
        viewModelScope.launch {
            repository.updateSetting("temperature_unit", unit)
        }
        refreshWeather()
    }

    fun togglePinnedOnlyLocked(): Boolean {
        val newStatus = !_uiState.value.isPinnedOnlyLocked
        _uiState.update {
            it.copy(
                isPinnedOnlyLocked = newStatus,
                currentScreen = if (newStatus) LauncherScreen.SIX_APPS else it.currentScreen,
                pinnedLockFeedbackMessage = if (newStatus) "UltraFocus Mode Locked • Triple-tap to exit" else "UltraFocus Mode Unlocked",
            )
        }
        return newStatus
    }

    fun clearPinnedLockFeedback() {
        _uiState.update { it.copy(pinnedLockFeedbackMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        mediaSessionManager.release()
        weatherJob?.cancel()
        lockoutJob?.cancel()
        scribbleRecognitionJob?.cancel()
    }
}
