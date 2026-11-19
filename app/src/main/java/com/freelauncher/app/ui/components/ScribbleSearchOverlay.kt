package com.freelauncher.app.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AllAppsAlignment
import com.freelauncher.app.data.models.AppDrawerStyle
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.models.IconDisplayMode
import com.freelauncher.app.data.models.MinimalIconPackConfig
import com.freelauncher.app.data.service.IconPackManager
import com.freelauncher.app.data.service.UniversalSearchManager
import com.freelauncher.app.ui.screens.AllAppsRowItem
import com.freelauncher.app.ui.util.AppMatchResult
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.ScribbleStroke
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScribbleSearchOverlay(
    strokes: List<ScribbleStroke>,
    currentStroke: ScribbleStroke?,
    recognizedText: String,
    matchingApps: List<AppMatchResult>,
    isRecognizing: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit = {},
    onClearScribble: () -> Unit,
    modifier: Modifier = Modifier,
    iconPackManager: IconPackManager? = null,
    appDrawerStyle: AppDrawerStyle = AppDrawerStyle.NEO_MINIMAL,
    showMonograms: Boolean = false,
    iconDisplayMode: IconDisplayMode = IconDisplayMode.TEXT_ONLY,
    selectedIconPackPackage: String = "",
    minimalConfig: MinimalIconPackConfig = MinimalIconPackConfig(),
    allAppsAlignment: AllAppsAlignment = AllAppsAlignment.LEFT,
    sixAppsScale: Float = 0.9f
) {
    val context = LocalContext.current
    val strokeColor = MaterialTheme.colorScheme.primary
    val showResultOverlay = recognizedText.isNotBlank()

    val matchingSettings = remember(recognizedText) {
        if (recognizedText.isNotBlank()) {
            UniversalSearchManager.searchSettings(recognizedText, isScribbleSearch = true)
        } else {
            emptyList()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Live Smooth Bezier Ink Canvas
        ScribbleCanvas(
            strokes = strokes,
            currentStroke = currentStroke,
            strokeColor = strokeColor,
            strokeWidth = 5.dp
        )

        // 2. Full-Screen Minimal NOMA Scribble Result Hub Layout (Matching App & Settings Cards)
        AnimatedVisibility(
            visible = showResultOverlay,
            enter = slideInVertically { -it / 3 } + fadeIn(),
            exit = slideOutVertically { -it / 3 } + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 10.dp)
            ) {
                // Header Bar: Single scribbled letter title and app count subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = recognizedText.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp,
                                fontSize = 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${matchingApps.size} apps found",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )

                            if (isRecognizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.8.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            LauncherHaptics.playClick(context)
                            onClearScribble()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Results: Matching Apps List followed by System Settings Cards
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Matching Apps List
                    if (matchingApps.isNotEmpty()) {
                        if (appDrawerStyle == AppDrawerStyle.NEO_MINIMAL) {
                            items(
                                items = matchingApps,
                                key = { "scribble_neo_${it.app.id.ifBlank { it.app.packageName }}" }
                            ) { matchResult ->
                                val app = matchResult.app
                                val appTextAlignment = when (allAppsAlignment) {
                                    AllAppsAlignment.LEFT -> TextAlign.Start
                                    AllAppsAlignment.CENTER -> TextAlign.Center
                                    AllAppsAlignment.RIGHT -> TextAlign.End
                                }
                                val boxContentAlignment = when (allAppsAlignment) {
                                    AllAppsAlignment.LEFT -> Alignment.CenterStart
                                    AllAppsAlignment.CENTER -> Alignment.Center
                                    AllAppsAlignment.RIGHT -> Alignment.CenterEnd
                                }

                                val highlightedText = buildHighlightedLabel(
                                    result = matchResult,
                                    baseColor = MaterialTheme.colorScheme.onBackground,
                                    highlightColor = MaterialTheme.colorScheme.primary
                                )

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
                                        .padding(vertical = 4.dp),
                                    contentAlignment = boxContentAlignment
                                ) {
                                    Text(
                                        text = highlightedText,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = 22.sp * sixAppsScale,
                                            fontWeight = FontWeight.Normal,
                                            letterSpacing = 0.3.sp,
                                            textAlign = appTextAlignment
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            items(
                                items = matchingApps,
                                key = { "scribble_classic_${it.app.id.ifBlank { it.app.packageName }}" }
                            ) { matchResult ->
                                val app = matchResult.app
                                AllAppsRowItem(
                                    app = app,
                                    showMonogram = showMonograms,
                                    iconDisplayMode = iconDisplayMode,
                                    iconPackPackage = selectedIconPackPackage,
                                    iconPackManager = iconPackManager,
                                    minimalConfig = minimalConfig,
                                    alignment = allAppsAlignment,
                                    onClick = {
                                        LauncherHaptics.playClick(context)
                                        onLaunchApp(app)
                                    },
                                    onLongClick = {
                                        LauncherHaptics.playClick(context)
                                        onLongPressApp(app)
                                    },
                                    testTag = "scribble_classic_app_${app.packageName}"
                                )
                            }
                        }
                    }

                    // Matching System Settings Cards (as seen in Noma Search Results)
                    if (matchingSettings.isNotEmpty()) {
                        items(
                            items = matchingSettings,
                            key = { "scribble_setting_${it.intentAction}" }
                        ) { setting ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        LauncherHaptics.playClick(context)
                                        try {
                                            context.startActivity(
                                                Intent(setting.intentAction).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                            )
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    Text(
                                        text = setting.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = setting.description,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    if (matchingApps.isEmpty() && matchingSettings.isEmpty() && !isRecognizing) {
                        item(key = "empty_scribble_state") {
                            Text(
                                text = "No matching apps or settings found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildHighlightedLabel(
    result: AppMatchResult,
    baseColor: Color,
    highlightColor: Color
): AnnotatedString {
    val label = result.app.label
    val start = result.highlightStart
    val end = result.highlightEnd

    if (start in 0 until end && end <= label.length) {
        return buildAnnotatedString {
            append(label.substring(0, start))
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(label.substring(start, end))
            }
            withStyle(SpanStyle(color = baseColor)) {
                append(label.substring(end))
            }
        }
    } else {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) {
                append(label)
            }
        }
    }
}
