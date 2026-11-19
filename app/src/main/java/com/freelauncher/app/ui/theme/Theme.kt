package com.freelauncher.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class LauncherThemeMode(val id: String, val displayName: String, val isDark: Boolean) {
    OLED_BLACK("oled_black", "OLED Pure Black", true),
    MATERIAL_YOU("material_you", "Material You (Dynamic Colors)", true),
    CHARCOAL("charcoal", "Charcoal Dark", true)
}

val OledColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = OledCard,
    onPrimaryContainer = Color.White,
    secondary = OledOnMuted,
    onSecondary = Color.Black,
    background = OledBackground,
    onBackground = OledOnBackground,
    surface = OledSurface,
    onSurface = OledOnBackground,
    surfaceVariant = OledCard,
    onSurfaceVariant = OledOnMuted,
    outline = OledBorder
)

val CharcoalColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = CharcoalBackground,
    primaryContainer = CharcoalCard,
    onPrimaryContainer = CharcoalOnBackground,
    secondary = CharcoalOnMuted,
    onSecondary = Color.White,
    background = CharcoalBackground,
    onBackground = CharcoalOnBackground,
    surface = CharcoalSurface,
    onSurface = CharcoalOnBackground,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = CharcoalOnMuted,
    outline = CharcoalBorder
)

@Composable
fun FreeLauncherTheme(
    themeMode: LauncherThemeMode = LauncherThemeMode.OLED_BLACK,
    launcherFont: LauncherFont = LauncherFont.MONOSPACE,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when (themeMode) {
        LauncherThemeMode.OLED_BLACK -> OledColorScheme
        LauncherThemeMode.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                CharcoalColorScheme
            }
        }
        LauncherThemeMode.CHARCOAL -> CharcoalColorScheme
    }
    val typography = getTypographyForFont(launcherFont)

    CompositionLocalProvider(
        LocalLauncherFont provides launcherFont
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
