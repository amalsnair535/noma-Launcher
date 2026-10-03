package com.freelauncher.app.ui.viewmodel

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freelauncher.app.data.db.*
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppCategoryInfo
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.repository.LauncherRepository
import com.freelauncher.app.data.service.DigitalWellbeingService
import com.freelauncher.app.data.service.FocusDayUsageData
import com.freelauncher.app.ui.components.ClockStyle
import com.freelauncher.app.ui.components.TimeCardVerticalAlign
import com.freelauncher.app.ui.components.TimeCardHorizontalAlign
import com.freelauncher.app.ui.theme.LauncherFont
import com.freelauncher.app.ui.theme.LauncherThemeMode
import com.freelauncher.app.ui.theme.LauncherWallpaper
import com.google.firebase.crashlytics.FirebaseCrashlytics
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
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
    val fontFamily: LauncherFont = LauncherFont.MINIMAL_SANS,
    val themeMode: LauncherThemeMode = LauncherThemeMode.OLED_BLACK,
    val wallpaperId: String = "cyber_noir",
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
    val timeAwayStats: com.freelauncher.app.data.service.TimeAwayStats? = null,
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
    val showSetPinDialog: Boolean = false,
    val showPinUnlockSheet: Boolean = false,
    val isSearchOnlyMode: Boolean = false,
    val showOnboardingGuide: Boolean = false,
    val showRearrangePinnedDialog: Boolean = false,
    val showAboutSheet: Boolean = false,
    val showProSheet: Boolean = false,
    val showBackupSheet: Boolean = false,
    val isProUnlocked: Boolean = false,
    val showWeatherBatteryGlance: Boolean = false,
    val temperatureUnit: String = "F",
    val enableDoubleTapToSleep: Boolean = false,
    val enableSwipeDownSearch: Boolean = false,
    val enableMindfulPause: Boolean = false,
    val appPendingMindfulPause: AppItem? = null,
    val sixAppsScale: Float = 0.9f,
    val batteryLevel: Int = 100,
    val isBatteryCharging: Boolean = false,
    val weatherText: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _currentTime = MutableStateFlow(Date())
    val currentTime: StateFlow<Date> = _currentTime.asStateFlow()

    private val weatherService = com.freelauncher.app.data.service.WeatherService(application)

    init {
        startTimeTicker()
        loadSettings()
        observeDatabase()
        refreshApps()
        syncFeeds()
        refreshDigitalWellbeingStats()
        startBatteryAndWeatherMonitoring()
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
                val fontId = settingsMap["font_family"] ?: "minimal_sans"
                val themeId = settingsMap["theme_style"] ?: "oled_black"
                val wallpaperId = settingsMap["wallpaper_id"] ?: "cyber_noir"
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
                val doubleTapToSleep = settingsMap["enable_double_tap_to_sleep"] == "true"
                val enableSwipeDownSearch = settingsMap["enable_swipe_down_search"] == "true"
                val enableMindfulPause = settingsMap["enable_mindful_pause"] == "true"
                val sixAppsScale = settingsMap["six_apps_scale"]?.toFloatOrNull() ?: 0.9f
                val lockMethod = settingsMap["lock_method"] ?: "pin"
                val customPin = settingsMap["custom_launcher_pin"] ?: ""
                val isProUnlocked = settingsMap["is_pro_unlocked"] == "true"

                _uiState.update { state ->
                    state.copy(
                        clockStyle = ClockStyle.entries.find { it.id == clockStyleId } ?: ClockStyle.LARGE_DIGITAL,
                        timeCardVAlign = TimeCardVerticalAlign.entries.find { it.id == vAlignId } ?: TimeCardVerticalAlign.CENTER,
                        timeCardHAlign = TimeCardHorizontalAlign.entries.find { it.id == hAlignId } ?: TimeCardHorizontalAlign.CENTER,
                        timeCardOffsetX = offsetX,
                        timeCardOffsetY = offsetY,
                        timeCardScale = cardScale.coerceIn(0.5f, 2.0f),
                        fontFamily = LauncherFont.entries.find { it.id == fontId } ?: LauncherFont.MINIMAL_SANS,
                        themeMode = LauncherThemeMode.entries.find { it.id == themeId } ?: LauncherThemeMode.OLED_BLACK,
                        wallpaperId = wallpaperId,
                        customWallpaperUri = customWallpaperUri,
                        wallpaperDim = wallpaperDim,
                        showMonograms = showMonograms,
                        customGreeting = greeting,
                        isBiometricLockEnabled = biometricLock,
                        lockMethod = lockMethod,
                        customPin = customPin,
                        showGestureHints = gestureHints,
                        showNewsFeed = showNewsFeed,
                        showTimeAway = showTimeAway,
                        showOnboardingGuide = !onboardingCompleted,
                        showWeatherBatteryGlance = showWeatherGlance,
                        temperatureUnit = tempUnit,
                        enableDoubleTapToSleep = doubleTapToSleep,
                        enableSwipeDownSearch = enableSwipeDownSearch,
                        enableMindfulPause = enableMindfulPause,
                        sixAppsScale = sixAppsScale.coerceIn(0.6f, 1.8f),
                        isProUnlocked = isProUnlocked,
                    )
                }
            }
        }
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

    fun refreshApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            val pinnedIds = repository.getPinnedIds()
            val installedIds = apps.asSequence().map { it.id }.toSet()

            // Build the pinned apps matching the stored list (can be 0 to 6 apps freely)
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

            // Sync sanitized valid ID list back to repository if there was any ghost app
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
            _uiState.update {
                it.copy(
                    installedApps = updatedApps,
                    pinnedApps = pinnedList,
                    categories = userCategories,
                    categorizedApps = categorized,
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

    fun navigateTo(screen: LauncherScreen) {
        if (_uiState.value.isPinnedOnlyLocked && (screen != LauncherScreen.SIX_APPS)) {
            _uiState.update { it.copy(pinnedLockFeedbackMessage = "UltraFocus Mode Locked • Triple-tap to exit") }
            return
        }
        _uiState.update { 
            it.copy(
                currentScreen = screen,
                isSearchOnlyMode = if (screen == LauncherScreen.ALL_APPS) it.isSearchOnlyMode else false,
                searchQuery = if (screen == LauncherScreen.ALL_APPS) it.searchQuery else ""
            ) 
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
                if (_uiState.value.customPin.isBlank()) {
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
                    searchQuery = "",
                    currentScreen = LauncherScreen.ALL_APPS,
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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

            // Try specific launcher activity for the target app
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

            // Fallback to default package launch intent
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
            FirebaseCrashlytics.getInstance().recordException(e)
            android.widget.Toast.makeText(context, "Could not open ${app.label}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun setBackupSheetVisible(visible: Boolean) {
        if (visible && !_uiState.value.isProUnlocked) {
            setProSheetVisible(true)
        } else {
            _uiState.update { it.copy(showBackupSheet = visible) }
        }
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
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("LauncherVM", "Error opening web URL: %s", e)
            FirebaseCrashlytics.getInstance().recordException(e)
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
            // Try package fallback
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
        val isCustomOrTheme = id == "custom_gallery" || id.startsWith("custom_theme_")
        if (isCustomOrTheme && !_uiState.value.isProUnlocked) {
            _uiState.update { it.copy(showProSheet = true) }
            return
        }
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
            repository.saveCustomWallpaper(name, colors, isDark)
            setAtmosphericCreatorVisible(false)
        }
    }

    fun deleteAtmosphericTheme(id: String) {
        val longId = id.removePrefix("custom_theme_").toLongOrNull()
        if (longId != null) {
            viewModelScope.launch {
                repository.deleteCustomWallpaper(longId)
                if (_uiState.value.wallpaperId == id) {
                    setWallpaper("cyber_noir")
                }
            }
        }
    }

    fun saveCustomWallpaperFromUri(uri: Uri) {
        if (!_uiState.value.isProUnlocked) {
            _uiState.update { it.copy(showProSheet = true) }
            return
        }
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
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    fun removeCustomWallpaper() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    wallpaperId = "cyber_noir",
                    customWallpaperUri = null,
                )
            }
            repository.updateSetting("wallpaper_id", "cyber_noir")
            repository.updateSetting("custom_wallpaper_uri", "")
        }
    }

    // Biometric / Fingerprint & PIN Lock
    fun setBiometricLockEnabled(enabled: Boolean) {
        if (!enabled && _uiState.value.isBiometricLockEnabled) {
            // Require authentication before disabling lock
            pendingDisableBiometric = true
            pendingSearchUnlock = false
            if (_uiState.value.lockMethod == "pin") {
                if (_uiState.value.customPin.isBlank()) {
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
        if (method == "biometric" && _uiState.value.lockMethod == "pin" && _uiState.value.isBiometricLockEnabled && _uiState.value.customPin.isNotBlank()) {
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
            _uiState.update { it.copy(customPin = pin, showSetPinDialog = false) }
            repository.updateSetting("custom_launcher_pin", pin)
            if (!_uiState.value.isBiometricLockEnabled) {
                setBiometricLockEnabled(true)
            }
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

    fun setProSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showProSheet = visible) }
    }

    fun unlockProFeatures() {
        _uiState.update { it.copy(isProUnlocked = true, showProSheet = false) }
        viewModelScope.launch {
            repository.updateSetting("is_pro_unlocked", "true")
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
                if (_uiState.value.customPin.isBlank()) {
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

    fun setDoubleTapToSleepEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableDoubleTapToSleep = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_double_tap_to_sleep", enabled.toString())
        }
    }

    fun setSwipeDownSearchEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableSwipeDownSearch = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_swipe_down_search", enabled.toString())
        }
    }

    fun setMindfulPauseEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableMindfulPause = enabled) }
        viewModelScope.launch {
            repository.updateSetting("enable_mindful_pause", enabled.toString())
        }
    }

    fun checkForUpdates(context: android.content.Context) {
        val updateManager = com.freelauncher.app.data.service.UpdateManager(context)
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

    fun performDoubleTapToSleep(context: Context) {
        if (com.freelauncher.app.data.service.NomaAccessibilityService.isEnabled() &&
            com.freelauncher.app.data.service.NomaAccessibilityService.lockScreen(context)) {
            return
        }

        android.widget.Toast.makeText(
            context,
            "Enable '.noma' in Accessibility Settings to lock screen on double-tap",
            android.widget.Toast.LENGTH_LONG
        ).show()

        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun startBatteryAndWeatherMonitoring() {
        try {
            val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = getApplication<Application>().registerReceiver(
                object : android.content.BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        intent?.let { updateBatteryState(it) }
                    }
                }, filter
            )
            batteryStatus?.let { updateBatteryState(it) }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        viewModelScope.launch {
            while (isActive) {
                refreshWeather()
                delay(1800000.milliseconds)
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
}
