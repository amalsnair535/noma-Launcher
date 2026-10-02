package com.freelauncher.app.data.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.freelauncher.app.data.db.FocusSessionEntity
import com.freelauncher.app.ui.util.DateTimeUtils
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class FocusDayUsageData(
    val dayLetter: String,
    val dateKey: String,
    val focusScore: Int,
    val unlocks: Int,
    val screenTimeMinutes: Int,
    val isToday: Boolean,
    val focusSessionMinutes: Int = 0,
    val longestBreakMinutes: Int = 0
)

data class LongestBreakData(
    val dayLabel: String,
    val minutes: Int
)

data class TimeAwayStats(
    val todayPhoneFreeMinutes: Int,
    val currentBreakMinutes: Int,
    val todayLongestBreakMinutes: Int,
    val weeklyPhoneFreePercentage: Int,
    val totalWeeklyReclaimedMinutes: Int,
    val weeklyHistory: List<FocusDayUsageData>,
    val longestBreaksHistory: List<LongestBreakData>
)

/**
 * Raw per-day numbers pulled from UsageStatsManager, before scoring is applied.
 */
private data class DayRawUsage(
    val screenTimeMinutes: Int,
    val unlockCount: Int,
    val longestBreakMinutes: Int,
    /** Gap still open at the end of the query window (only meaningful for "today"). */
    val trailingBreakMinutes: Int
) {
    companion object {
        val EMPTY = DayRawUsage(0, 0, 0, 0)
    }
}

object DigitalWellbeingService {

    /**
     * Fine-grained event types (KEYGUARD_HIDDEN/SHOWN, SCREEN_INTERACTIVE/NON_INTERACTIVE) were
     * added in API 28 (P). Below that we can still get coarse screen time from
     * queryUsageStats, but unlocks and break/gap detection genuinely require these events,
     * so we degrade gracefully instead of guessing.
     */
    private val supportsFineGrainedEvents = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Checks if the app has been granted PACKAGE_USAGE_STATS permission to query Digital Wellbeing stats.
     */
    fun hasUsagePermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Launches the Android system Settings screen for Usage Access so the user can grant permission.
     */
    fun openUsageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * System packages to ignore when calculating user active screen time.
     * NOTE: this is an inherently incomplete denylist (OEM keyboards/system UI vary a lot
     * across manufacturers). It's fine as a heuristic, but if false positives show up on
     * specific devices, consider flipping this to an allowlist-based "distracting apps" model.
     */
    private val IGNORED_SYSTEM_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.android.keyguard"
    )

    /**
     * Builds the ignore-list for a given context, always excluding the launcher's own package —
     * home-screen/app-drawer time from *this* app shouldn't count as "distracting" screen time.
     */
    private fun ignoredPackagesFor(context: Context): Set<String> =
        IGNORED_SYSTEM_PACKAGES + context.packageName

    /**
     * Obtains real digital wellbeing usage statistics from Android's UsageStatsManager for the past 7 days.
     * Computes, in a single pass over the event stream per day:
     * 1. Screen Time (minutes spent in foreground apps, excluding system/keyboard/self packages)
     * 2. Device Unlocks (KEYGUARD_HIDDEN count when available)
     * 3. Longest phone-free break of the day
     * 4. Focus Score (0-100) combining the above with logged focus sessions
     */
    fun getWeeklyStats(
        context: Context,
        focusSessions: List<FocusSessionEntity>
    ): List<FocusDayUsageData> {
        val hasPermission = hasUsagePermission(context)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val ignoredPackages = ignoredPackagesFor(context)

        val results = mutableListOf<FocusDayUsageData>()
        val now = System.currentTimeMillis()

        // Query the last 7 days ending today (6 days ago -> today)
        for (i in 6 downTo 0) {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val startTime = startCal.timeInMillis
            val isToday = (i == 0)
            val endTime = if (isToday) now else endCal.timeInMillis
            val dateKey = DateTimeUtils.format(startCal.time, DateTimeUtils.Patterns.DATE_ISO)
            val dayLetter = DateTimeUtils.format(startCal.time, DateTimeUtils.Patterns.DAY_LETTER).take(1).uppercase(Locale.ROOT)

            val sessionsOnDay = focusSessions.filter { it.dateString == dateKey }
            val focusSessionMins = sessionsOnDay.sumOf { it.sessionMinutes }

            val raw = if (hasPermission && usageStatsManager != null) {
                val phoneActiveAtStart = isForegroundActiveBefore(usageStatsManager, startTime)
                computeDayRawUsage(usageStatsManager, startTime, endTime, phoneActiveAtStart, ignoredPackages)
            } else {
                DayRawUsage.EMPTY
            }

            val calculatedScore = calculateDigitalWellbeingFocusScore(
                screenTimeMinutes = raw.screenTimeMinutes,
                unlocks = raw.unlockCount,
                focusSessionMinutes = focusSessionMins,
                hasPermission = hasPermission
            )

            results.add(
                FocusDayUsageData(
                    dayLetter = dayLetter,
                    dateKey = dateKey,
                    focusScore = calculatedScore,
                    unlocks = raw.unlockCount,
                    screenTimeMinutes = raw.screenTimeMinutes,
                    isToday = isToday,
                    focusSessionMinutes = focusSessionMins,
                    longestBreakMinutes = raw.longestBreakMinutes
                )
            )
        }

        return results
    }

    /**
     * Single-pass event processing for one day. Tracks two independent things off the same
     * event stream so we only call queryEvents once per day:
     *  - "distracting" foreground time (filtered by ignoredPackages) -> screenTimeMinutes
     *  - "any app in foreground" state (unfiltered) -> break/gap tracking + unlock fallback
     *
     * A break only ends on a real engagement signal (KEYGUARD_HIDDEN or an app actually
     * resuming) — not on SCREEN_INTERACTIVE alone, since that also fires for ambient/raise
     * -to-wake without the user actually using the phone.
     */
    private fun computeDayRawUsage(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        phoneActiveAtStart: Boolean,
        ignoredPackages: Set<String>
    ): DayRawUsage {
        if (endTime <= startTime) return DayRawUsage.EMPTY
        val dayMaxMillis = endTime - startTime

        var distractingPackage: String? = null
        var distractingStart = 0L
        var totalForegroundMillis = 0L

        var anyAppActive = phoneActiveAtStart
        var lastIdleStart = if (phoneActiveAtStart) -1L else startTime
        var longestBreakMillis = 0L

        var keyguardUnlockCount = 0
        var sessionStartCount = 0
        var sawKeyguardEvents = false

        fun closeDistractingSession(eventTime: Long) {
            if (distractingPackage != null && eventTime > distractingStart) {
                totalForegroundMillis += (eventTime - distractingStart)
            }
            distractingPackage = null
        }

        fun closeBreak(eventTime: Long) {
            if (lastIdleStart >= 0L) {
                val gap = eventTime - lastIdleStart
                if (gap > longestBreakMillis) longestBreakMillis = gap
                lastIdleStart = -1L
            }
            anyAppActive = true
        }

        try {
            if (supportsFineGrainedEvents) {
                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val eventTime = event.timeStamp.coerceIn(startTime, endTime)
                    val pkg = event.packageName

                    when (event.eventType) {
                        UsageEvents.Event.KEYGUARD_HIDDEN -> {
                            keyguardUnlockCount++
                            sawKeyguardEvents = true
                            if (!anyAppActive) closeBreak(eventTime)
                        }
                        UsageEvents.Event.ACTIVITY_RESUMED -> {
                            if (!anyAppActive) {
                                closeBreak(eventTime)
                                sessionStartCount++
                            }
                            if (pkg != null && !ignoredPackages.contains(pkg)) {
                                closeDistractingSession(eventTime)
                                distractingPackage = pkg
                                distractingStart = eventTime
                            } else {
                                closeDistractingSession(eventTime)
                            }
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED,
                        UsageEvents.Event.ACTIVITY_STOPPED -> {
                            if (distractingPackage == pkg) {
                                closeDistractingSession(eventTime)
                            }
                        }
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                        UsageEvents.Event.KEYGUARD_SHOWN -> {
                            // Screen genuinely off: close any foreground session and start a break.
                            closeDistractingSession(eventTime)
                            if (anyAppActive) {
                                anyAppActive = false
                                lastIdleStart = eventTime
                            }
                        }
                        // SCREEN_INTERACTIVE deliberately not treated as engagement — see doc comment.
                    }
                }
            }

            // Account for a foreground app still open at the end of the window.
            if (distractingPackage != null && endTime > distractingStart) {
                totalForegroundMillis += (endTime - distractingStart)
            }

            // Fallback to queryUsageStats if the event stream gave us nothing (some OEMs drop events).
            if (totalForegroundMillis == 0L) {
                val statsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                if (!statsList.isNullOrEmpty()) {
                    val filteredSum = statsList
                        .filter { !ignoredPackages.contains(it.packageName) }
                        .sumOf { it.totalTimeInForeground }
                    totalForegroundMillis = filteredSum.coerceIn(0L, dayMaxMillis)
                }
            }

            totalForegroundMillis = totalForegroundMillis.coerceIn(0L, dayMaxMillis)
        } catch (_: Exception) {
            return DayRawUsage.EMPTY
        }

        // A gap still open at the end of the window (relevant for "today").
        var trailingBreakMillis = 0L
        if (!anyAppActive && lastIdleStart >= 0L) {
            trailingBreakMillis = endTime - lastIdleStart
            if (trailingBreakMillis > longestBreakMillis) longestBreakMillis = trailingBreakMillis
        }

        val unlockCount = if (sawKeyguardEvents) keyguardUnlockCount else sessionStartCount

        return DayRawUsage(
            screenTimeMinutes = (totalForegroundMillis / (1000 * 60)).toInt(),
            unlockCount = unlockCount,
            longestBreakMinutes = (longestBreakMillis / (1000 * 60)).toInt(),
            trailingBreakMinutes = (trailingBreakMillis / (1000 * 60)).toInt()
        )
    }

    /**
     * Digital Wellbeing Focus Score calculation (0-100), continuous end to end (no cliffs):
     * - Screen Time Health (0-50 pts): <=2h -> 50, tapering linearly to 0 at 8h+
     * - Unlock Hygiene (0-30 pts): <=30 unlocks -> 30, tapering linearly to 0 at 120+
     * - Intentional Focus Sessions (0-20 pts): +5 pts per 15 min logged, capped at 20 (60 min)
     * - Mindful Balance bonus (0-10 pts): tapers smoothly rather than cutting off sharply,
     *   so a day at 241 min screen time scores ~the same as one at 239 min.
     *
     * With no usage permission we have no real screen-time/unlock signal, so the score is
     * based purely on logged focus sessions instead of fabricating a baseline.
     */
    fun calculateDigitalWellbeingFocusScore(
        screenTimeMinutes: Int,
        unlocks: Int,
        focusSessionMinutes: Int,
        hasPermission: Boolean
    ): Int {
        val sessionBonus = ((focusSessionMinutes / 15.0) * 5.0).coerceIn(0.0, 20.0)

        if (!hasPermission) {
            return ((sessionBonus / 20.0) * 100.0).roundToInt().coerceIn(0, 100)
        }

        // 1. Screen Time Health (0 to 50 pts)
        val screenTimeScore = when {
            screenTimeMinutes <= 120 -> 50.0
            screenTimeMinutes >= 480 -> 0.0
            else -> 50.0 * (1.0 - (screenTimeMinutes - 120).toDouble() / 360.0)
        }

        // 2. Unlock Hygiene (0 to 30 pts)
        val unlockScore = when {
            unlocks <= 30 -> 30.0
            unlocks >= 120 -> 0.0
            else -> 30.0 * (1.0 - (unlocks - 30).toDouble() / 90.0)
        }

        // 3. Mindful Balance bonus (0 to 10 pts) — smooth taper instead of a hard cutoff,
        // fully in effect at 0 usage and fading out by 300 min / 90 unlocks.
        val screenTimeTaper = when {
            screenTimeMinutes <= 240 -> 1.0
            screenTimeMinutes >= 300 -> 0.0
            else -> 1.0 - (screenTimeMinutes - 240).toDouble() / 60.0
        }
        val unlockTaper = when {
            unlocks <= 70 -> 1.0
            unlocks >= 90 -> 0.0
            else -> 1.0 - (unlocks - 70).toDouble() / 20.0
        }
        val mindfulBaseline = 10.0 * minOf(screenTimeTaper, unlockTaper)

        val total = (screenTimeScore + unlockScore + sessionBonus + mindfulBaseline).roundToInt()
        return total.coerceIn(0, 100)
    }

    const val DAILY_MINUTES = 24 * 60 // 1440 minutes (24-hour full day model)

    /**
     * Calculates total minutes elapsed since midnight today (00:00).
     */
    fun getElapsedMinutesToday(): Int {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((System.currentTimeMillis() - startOfDay) / (1000 * 60)).toInt().coerceIn(0, DAILY_MINUTES)
    }

    /**
     * Looks back a couple of hours before [time] to guess whether the phone was mid-session
     * (some app in the foreground) right at that boundary. Used for every day now (not just
     * "today"), so multi-day usage sessions that cross midnight don't get miscounted as a
     * break starting exactly at 00:00.
     */
    private fun isForegroundActiveBefore(usageStatsManager: UsageStatsManager, time: Long): Boolean {
        if (!supportsFineGrainedEvents) return false
        return try {
            val events = usageStatsManager.queryEvents(time - (2 * 60 * 60 * 1000), time)
            val event = UsageEvents.Event()
            var isOn = false
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.KEYGUARD_HIDDEN,
                    UsageEvents.Event.ACTIVITY_RESUMED -> isOn = true
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> isOn = false
                }
            }
            isOn
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Aggregates real-time statistics for the "Time Away" dashboard. Reuses the single pass
     * already done in getWeeklyStats instead of re-querying the event stream a second time.
     */
    fun getTimeAwayStats(
        context: Context,
        focusSessions: List<FocusSessionEntity>
    ): TimeAwayStats {
        val hasPermission = hasUsagePermission(context)
        if (!hasPermission) {
            return TimeAwayStats(
                todayPhoneFreeMinutes = 0,
                currentBreakMinutes = 0,
                todayLongestBreakMinutes = 0,
                weeklyPhoneFreePercentage = 0,
                totalWeeklyReclaimedMinutes = 0,
                weeklyHistory = emptyList(),
                longestBreaksHistory = emptyList()
            )
        }

        val weeklyHistory = getWeeklyStats(context, focusSessions)
        val today = weeklyHistory.lastOrNull { it.isToday }

        val elapsedToday = getElapsedMinutesToday()
        val todayPhoneFreeMinutes = today?.let { (elapsedToday - it.screenTimeMinutes).coerceAtLeast(0) } ?: 0

        val totalWeeklyReclaimedMinutes = weeklyHistory.sumOf { day ->
            if (day.isToday) todayPhoneFreeMinutes
            else (DAILY_MINUTES - day.screenTimeMinutes).coerceAtLeast(0)
        }

        val phoneFreePct = if (elapsedToday > 0) {
            ((todayPhoneFreeMinutes.toDouble() / elapsedToday.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            100
        }

        val longestBreaksHistory = weeklyHistory.map { day ->
            LongestBreakData(dayLabel = dayLabelFor(day), minutes = day.longestBreakMinutes)
        }

        // "Current break" is the trailing gap for today specifically — recompute it directly
        // rather than threading a second field through FocusDayUsageData for every day.
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val currentBreakMinutes = if (usageStatsManager != null) {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val now = System.currentTimeMillis()
            val phoneActiveAtStart = isForegroundActiveBefore(usageStatsManager, startOfToday)
            computeDayRawUsage(usageStatsManager, startOfToday, now, phoneActiveAtStart, ignoredPackagesFor(context))
                .trailingBreakMinutes
        } else 0

        return TimeAwayStats(
            todayPhoneFreeMinutes = todayPhoneFreeMinutes,
            currentBreakMinutes = currentBreakMinutes,
            todayLongestBreakMinutes = today?.longestBreakMinutes ?: 0,
            weeklyPhoneFreePercentage = phoneFreePct,
            totalWeeklyReclaimedMinutes = totalWeeklyReclaimedMinutes,
            weeklyHistory = weeklyHistory,
            longestBreaksHistory = longestBreaksHistory
        )
    }

    private fun dayLabelFor(day: FocusDayUsageData): String {
        if (day.isToday) return "Today"
        val dayDate = java.text.SimpleDateFormat(DateTimeUtils.Patterns.DATE_ISO, Locale.getDefault()).parse(day.dateKey) ?: return day.dayLetter
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val yesterdayKey = DateTimeUtils.format(yesterday, DateTimeUtils.Patterns.DATE_ISO)
        return if (day.dateKey == yesterdayKey) "Yesterday"
        else DateTimeUtils.format(dayDate, DateTimeUtils.Patterns.DAY_NAME)
    }
}
