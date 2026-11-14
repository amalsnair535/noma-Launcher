package com.freelauncher.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AllAppsAlignment
import com.freelauncher.app.data.models.AppDrawerStyle
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.models.IconDisplayMode
import com.freelauncher.app.data.models.MinimalIconPackConfig
import com.freelauncher.app.data.service.IconPackManager
import com.freelauncher.app.ui.screens.AllAppsRowItem
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.ScribbleStroke

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScribbleSearchOverlay(
    strokes: List<ScribbleStroke>,
    currentStroke: ScribbleStroke?,
    recognizedText: String,
    matchingApps: List<AppItem>,
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
    allAppsAlignment: AllAppsAlignment = AllAppsAlignment.CENTER,
    sixAppsScale: Float = 0.9f
) {
    val context = LocalContext.current
    val strokeColor = MaterialTheme.colorScheme.primary
    val allStrokes = strokes + listOfNotNull(currentStroke)
    val showResultCard = recognizedText.isNotBlank()

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Live Smooth Ink Drawing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (stroke in allStrokes) {
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

        // 2. Floating Minimal NOMA Scribble Result Hub at Top Center
        AnimatedVisibility(
            visible = showResultCard,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Bar: Recognized Query Badge & Clear Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Text(
                                text = "\"$recognizedText\"",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                LauncherHaptics.playClick(context)
                                onClearScribble()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear drawing",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Matching App Results Rendered according to selected App Drawer Style
                    if (matchingApps.isNotEmpty()) {
                        if (appDrawerStyle == AppDrawerStyle.NEO_MINIMAL) {
                            // Neo Minimal Drawer Style
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(
                                    items = matchingApps,
                                    key = { "scribble_neo_${it.id.ifBlank { it.packageName }}" }
                                ) { app ->
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

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 40.dp)
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
                                            text = app.label,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontSize = 22.sp * sixAppsScale,
                                                fontWeight = FontWeight.Normal,
                                                letterSpacing = 0.3.sp,
                                                textAlign = appTextAlignment
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Classic Drawer Style
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(
                                    items = matchingApps,
                                    key = { "scribble_classic_${it.id.ifBlank { it.packageName }}" }
                                ) { app ->
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
                    } else if (recognizedText.isNotBlank() && !isRecognizing) {
                        Text(
                            text = "No matching apps for \"$recognizedText\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
