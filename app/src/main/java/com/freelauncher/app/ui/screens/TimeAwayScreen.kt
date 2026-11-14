package com.freelauncher.app.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.EnergySavingsLeaf
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.service.DigitalWellbeingService
import com.freelauncher.app.data.service.LongestBreakData
import com.freelauncher.app.data.service.TimeAwayStats
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeAwayScreen(
    state: LauncherUiState,
    onNavigate: (LauncherScreen) -> Unit,
    onOpenUsageSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onNavigate(LauncherScreen.HOME)
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val hasPerm = state.hasUsagePermission
    val stats = state.timeAwayStats
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = if (isLandscape) 8.dp else 20.dp)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        val threshold = 70f
                        if (abs(totalDragX) > abs(totalDragY)) {
                            if (totalDragX < -threshold) {
                                // Dragged left -> Return to Home
                                onNavigate(LauncherScreen.HOME)
                            }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y
                }
            }
            .testTag("time_away_screen_root")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // Header Row
            item {
                HeaderRow(
                    onBackClick = { onNavigate(LauncherScreen.HOME) },
                    isLandscape = isLandscape
                )
            }

            // Permission Warning Banner if Usage Stats access is missing
            if (!hasPerm) {
                item {
                    PermissionBanner(onOpenUsageSettings = onOpenUsageSettings)
                }
            }

            if (isLandscape) {
                // LANDSCAPE MODE: Optimized Dashboard Layout

                // Top Metrics Row: Today Hero Card + Last Break + Longest Break
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TodayHeroCard(
                            hasPerm = hasPerm,
                            stats = stats,
                            isLandscape = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        LastBreakCard(
                            hasPerm = hasPerm,
                            stats = stats,
                            modifier = Modifier.weight(1f)
                        )
                        LongestBreakCard(
                            hasPerm = hasPerm,
                            stats = stats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Middle Row: Reclaimed Time Card
                item {
                    ReclaimedTimeCard(hasPerm = hasPerm, stats = stats)
                }

                // Bottom Row: 2-Column Side-by-Side (Weekly Overview & Recent Longest Breaks)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Weekly Overview
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Weekly Overview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            WeeklyOverviewCard(hasPerm = hasPerm, stats = stats)
                        }

                        // Right Column: Recent Longest Breaks
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Recent Longest Breaks",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            val breaks = if (hasPerm) (stats?.longestBreaksHistory ?: emptyList()) else emptyList()
                            RecentLongestBreaksCard(hasPerm = hasPerm, breaks = breaks)
                        }
                    }
                }
            } else {
                // PORTRAIT MODE: Standard Single-Column Layout

                // Today Hero Card
                item {
                    TodayHeroCard(
                        hasPerm = hasPerm,
                        stats = stats,
                        isLandscape = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Stat Grid: Last Break vs Longest Break
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LastBreakCard(
                            hasPerm = hasPerm,
                            stats = stats,
                            modifier = Modifier.weight(1f)
                        )
                        LongestBreakCard(
                            hasPerm = hasPerm,
                            stats = stats,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Total Weekly Reclaimed Time Card
                item {
                    ReclaimedTimeCard(hasPerm = hasPerm, stats = stats)
                }

                // Weekly History Section Header & Card
                item {
                    Text(
                        text = "Weekly Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    WeeklyOverviewCard(hasPerm = hasPerm, stats = stats)
                }

                // Recent Longest Breaks Section Header
                item {
                    Text(
                        text = "Recent Longest Breaks",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                val breaks = if (hasPerm) (stats?.longestBreaksHistory ?: emptyList()) else emptyList()
                if (breaks.isNotEmpty()) {
                    items(breaks) { breakData ->
                        LongestBreakItem(breakData = breakData)
                    }
                } else {
                    item {
                        Text(
                            text = if (hasPerm) "No break history recorded yet." else "Grant usage access permission to track screen-off breaks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    onBackClick: () -> Unit,
    isLandscape: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLandscape) 4.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("time_away_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Home",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "time away",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                fontSize = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun PermissionBanner(onOpenUsageSettings: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Usage Access Required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "To accurately track phone-free time and break durations, grant Usage Access permission in system settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
            )
            Button(
                onClick = onOpenUsageSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permission", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun TodayHeroCard(
    hasPerm: Boolean,
    stats: TimeAwayStats?,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedTime = if (hasPerm && stats != null) {
        val todayMins = stats.todayPhoneFreeMinutes
        val hours = todayMins / 60
        val mins = todayMins % 60
        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    } else {
        "--"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.testTag("today_hero_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLandscape) 14.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 6.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PHONE-FREE TODAY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isLandscape) 32.sp else 42.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Phone-free time today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LastBreakCard(
    hasPerm: Boolean,
    stats: TimeAwayStats?,
    modifier: Modifier = Modifier
) {
    val currentFormatted = if (hasPerm && stats != null) {
        val currentMins = stats.currentBreakMinutes
        val currentHrs = currentMins / 60
        val currentRemMins = currentMins % 60
        if (currentHrs > 0) "${currentHrs}h ${currentRemMins}m" else "${currentMins}m"
    } else {
        "--"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier.testTag("current_break_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Last Break",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = currentFormatted,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Most recent off-phone time",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LongestBreakCard(
    hasPerm: Boolean,
    stats: TimeAwayStats?,
    modifier: Modifier = Modifier
) {
    val longestFormatted = if (hasPerm && stats != null) {
        val longestMins = stats.todayLongestBreakMinutes
        val longestHrs = longestMins / 60
        val longestRemMins = longestMins % 60
        if (longestHrs > 0) "${longestHrs}h ${longestRemMins}m" else "${longestMins}m"
    } else {
        "--"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier.testTag("longest_break_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Longest Break",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = longestFormatted,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Continuous today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ReclaimedTimeCard(
    hasPerm: Boolean,
    stats: TimeAwayStats?,
    modifier: Modifier = Modifier
) {
    val weeklyFormatted = if (hasPerm && stats != null) {
        val totalWeeklyMins = stats.totalWeeklyReclaimedMinutes
        val weeklyHrs = totalWeeklyMins / 60
        val weeklyRemMins = totalWeeklyMins % 60
        if (weeklyHrs > 0) "${weeklyHrs}h ${weeklyRemMins}m" else "${weeklyRemMins}m"
    } else {
        "--"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("reclaimed_time_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.EnergySavingsLeaf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Reclaimed This Week",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = weeklyFormatted,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total offline focus time (past 7 days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun WeeklyOverviewCard(
    hasPerm: Boolean,
    stats: TimeAwayStats?,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_overview_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val history = if (hasPerm) (stats?.weeklyHistory ?: emptyList()) else emptyList()
            if (history.isEmpty()) {
                Text(
                    text = if (hasPerm) "No historical usage data recorded yet." else "Grant usage access permission above to view weekly statistics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                history.forEach { dayData ->
                    val screenMins = dayData.screenTimeMinutes
                    val totalDayMins = DigitalWellbeingService.DAILY_MINUTES
                    val elapsedMinutesToday = DigitalWellbeingService.getElapsedMinutesToday()
                    val freeMins = if (dayData.isToday) {
                        (elapsedMinutesToday - screenMins).coerceAtLeast(0)
                    } else {
                        (totalDayMins - screenMins).coerceAtLeast(0)
                    }
                    val maxWindowMins = if (dayData.isToday) elapsedMinutesToday.coerceAtLeast(1) else totalDayMins
                    val freeRatio = (freeMins.toFloat() / maxWindowMins.toFloat()).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = dayData.dayLetter,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (dayData.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(24.dp)
                        )

                        LinearProgressIndicator(
                            progress = { freeRatio },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (dayData.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        val hrs = freeMins / 60
                        val remMins = freeMins % 60
                        val formatted = if (hrs > 0 && remMins > 0) {
                            "${hrs}h ${remMins}m"
                        } else if (hrs > 0) {
                            "${hrs}h free"
                        } else {
                            "${remMins}m free"
                        }
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (dayData.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.width(68.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentLongestBreaksCard(
    hasPerm: Boolean,
    breaks: List<LongestBreakData>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (breaks.isEmpty()) {
                Text(
                    text = if (hasPerm) "No break history recorded yet." else "Grant usage access permission to track screen-off breaks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                breaks.forEach { breakData ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = breakData.dayLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val hrs = breakData.minutes / 60
                        val mins = breakData.minutes % 60
                        val formatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LongestBreakItem(breakData: LongestBreakData) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = breakData.dayLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            val hrs = breakData.minutes / 60
            val mins = breakData.minutes % 60
            val formatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

            Text(
                text = formatted,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
