package com.freelauncher.app.ui.screens

import androidx.activity.compose.BackHandler
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
import com.freelauncher.app.data.service.InstalledAppMatcher
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
) {
    NeoMinimalAppDrawer(
        state = state,
        onLaunchApp = onLaunchApp,
        onLongPressApp = onLongPressApp,
        onNavigate = onNavigate,
        onSearchQueryChange = onSearchQueryChange,
        modifier = modifier,
        iconPackManager = iconPackManager
    )
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
