package com.freelauncher.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.*
import com.freelauncher.app.data.service.IconPackManager
import kotlin.math.cos
import kotlin.math.sin

val HexagonShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = Math.min(size.width, size.height) / 2f
    for (i in 0 until 6) {
        val angle = Math.toRadians((60 * i - 30).toDouble())
        val x = cx + radius * cos(angle).toFloat()
        val y = cy + radius * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun AppIcon(
    app: AppItem,
    mode: IconDisplayMode,
    iconPackPackage: String,
    iconPackManager: IconPackManager?,
    minimalConfig: MinimalIconPackConfig,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    when (mode) {
        IconDisplayMode.TEXT_ONLY -> {
            // Text only mode: No icon or monogram badge is rendered
        }

        IconDisplayMode.MONOGRAM -> {
            MonogramIcon(
                monogram = app.monogram,
                size = size,
                modifier = modifier
            )
        }

        IconDisplayMode.ORIGINAL -> {
            val drawable by produceState<Drawable?>(initialValue = null, app.packageName, app.activityName, iconPackManager) {
                if (iconPackManager != null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        iconPackManager.getOriginalAppIcon(app.packageName, app.activityName)
                    }
                }
            }
            if (drawable != null) {
                DrawableImageIcon(
                    drawable = drawable!!,
                    size = size,
                    modifier = modifier
                )
            } else {
                MonogramIcon(
                    monogram = app.monogram,
                    size = size,
                    modifier = modifier
                )
            }
        }

        IconDisplayMode.ICON_PACK -> {
            val drawable by produceState<Drawable?>(initialValue = null, app.packageName, app.activityName, iconPackPackage, iconPackManager) {
                if (iconPackManager != null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        iconPackManager.getIconForApp(app.packageName, app.activityName, iconPackPackage)
                    }
                }
            }
            if (drawable != null) {
                DrawableImageIcon(
                    drawable = drawable!!,
                    size = size,
                    modifier = modifier
                )
            } else {
                MonogramIcon(
                    monogram = app.monogram,
                    size = size,
                    modifier = modifier
                )
            }
        }

        IconDisplayMode.MINIMAL_CREATOR -> {
            val drawable by produceState<Drawable?>(initialValue = null, app.packageName, app.activityName, minimalConfig.useMonogramGlyph, iconPackManager) {
                if (!minimalConfig.useMonogramGlyph && iconPackManager != null) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        iconPackManager.getOriginalAppIcon(app.packageName, app.activityName)
                    }
                }
            }
            MinimalCreatorIcon(
                monogram = app.monogram,
                config = minimalConfig,
                size = size,
                modifier = modifier,
                drawable = drawable
            )
        }
    }
}

@Composable
fun MonogramIcon(
    monogram: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .border(
                0.75.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun DrawableImageIcon(
    drawable: Drawable,
    size: Dp,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null
) {
    val imageBitmap: ImageBitmap? = remember(drawable, colorFilter) {
        try {
            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                drawable.bitmap.asImageBitmap()
            } else {
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 144
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 144
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    if (imageBitmap != null) {
        Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = null,
            colorFilter = colorFilter,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
fun MinimalCreatorIcon(
    monogram: String,
    config: MinimalIconPackConfig,
    size: Dp,
    modifier: Modifier = Modifier,
    drawable: Drawable? = null
) {
    val iconShape = remember(config.shape) {
        when (config.shape) {
            IconShape.CIRCLE -> CircleShape
            IconShape.SQUIRCLE -> RoundedCornerShape(14.dp)
            IconShape.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
            IconShape.HEXAGON -> HexagonShape
            IconShape.OUTLINE_ONLY -> RoundedCornerShape(10.dp)
        }
    }

    val iconColor = remember(config.colorTheme, config.customColorHex) {
        try {
            when (config.colorTheme) {
                IconColorTheme.MONOCHROME_WHITE -> Color.White
                IconColorTheme.ACCENT_PRIMARY -> Color(0xFFE2D3FA)
                IconColorTheme.PASTEL_NEON -> Color(0xFFB3E5FC)
                IconColorTheme.DARK_SLATE -> Color(0xFF2A2D34)
                IconColorTheme.CUSTOM -> Color(android.graphics.Color.parseColor(config.customColorHex))
            }
        } catch (_: Exception) {
            Color(0xFFE2D3FA)
        }
    }

    val bgModifier = when (config.containerStyle) {
        IconContainerStyle.SOLID -> Modifier.background(iconColor.copy(alpha = 0.2f))
        IconContainerStyle.GLASSMORPHIC -> Modifier.background(iconColor.copy(alpha = 0.12f))
        IconContainerStyle.OUTLINE -> Modifier.border(config.strokeWidthDp.dp, iconColor.copy(alpha = 0.7f), iconShape)
        IconContainerStyle.TRANSPARENT -> Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(iconShape)
            .then(bgModifier)
            .border(
                width = if (config.containerStyle != IconContainerStyle.OUTLINE) 0.75.dp else config.strokeWidthDp.dp,
                color = iconColor.copy(alpha = 0.4f),
                shape = iconShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!config.useMonogramGlyph && drawable != null) {
            val colorFilter = remember(config.colorTheme) {
                if (config.colorTheme == IconColorTheme.MONOCHROME_WHITE) {
                    val matrix = ColorMatrix().apply { setToSaturation(0f) }
                    ColorFilter.colorMatrix(matrix)
                } else null
            }

            val glyphDrawable = remember(drawable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                    drawable.foreground ?: drawable
                } else {
                    drawable
                }
            }

            DrawableImageIcon(
                drawable = glyphDrawable,
                size = (size * 0.68f),
                colorFilter = colorFilter
            )
        } else {
            Text(
                text = monogram,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                    letterSpacing = 0.5.sp
                ),
                color = if (config.colorTheme == IconColorTheme.MONOCHROME_WHITE) MaterialTheme.colorScheme.onBackground else iconColor
            )
        }
    }
}
