package com.freelauncher.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.roundToInt

data class ExtractedImagePalette(
    val topColor: Color,
    val middleColor: Color,
    val bottomColor: Color,
    val isDark: Boolean,
    val swatches: List<Color>
)

object ImageThemeExtractor {

    /**
     * Asynchronously decodes and downsamples an image Uri, extracting a 3-stop vertical gradient palette
     * and dominant color swatches.
     */
    suspend fun extractPaletteFromUri(context: Context, uri: Uri): ExtractedImagePalette? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val reqWidth = 120
            val reqHeight = 120
            var sampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / sampleSize) >= reqHeight && (halfWidth / sampleSize) >= reqWidth) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap != null) {
                val result = extractPaletteFromBitmap(bitmap)
                bitmap.recycle()
                result
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageThemeExtractor", "Error extracting palette from URI", e)
            null
        }
    }

    /**
     * Extracts 3-stop vertical gradient colors (Top, Middle, Bottom) and luminance information from a Bitmap.
     */
    fun extractPaletteFromBitmap(bitmap: Bitmap): ExtractedImagePalette {
        val width = bitmap.width
        val height = bitmap.height

        val topRegionEnd = (height * 0.30f).roundToInt().coerceAtLeast(1)
        val midRegionStart = (height * 0.35f).roundToInt()
        val midRegionEnd = (height * 0.65f).roundToInt()
        val bottomRegionStart = (height * 0.70f).roundToInt()

        val topColor = calculateAverageColor(bitmap, 0, width, 0, topRegionEnd)
        val middleColor = calculateAverageColor(bitmap, 0, width, midRegionStart, midRegionEnd)
        val bottomColor = calculateAverageColor(bitmap, 0, width, bottomRegionStart, height)

        val totalAvgColor = calculateAverageColor(bitmap, 0, width, 0, height)
        val isDark = calculateLuminance(totalAvgColor) < 0.5f

        val swatches = extractSwatches(bitmap, width, height)

        return ExtractedImagePalette(
            topColor = topColor,
            middleColor = middleColor,
            bottomColor = bottomColor,
            isDark = isDark,
            swatches = swatches
        )
    }

    private fun calculateAverageColor(bitmap: Bitmap, startX: Int, endX: Int, startY: Int, endY: Int): Color {
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var count = 0

        val step = 2 // Sample every 2nd pixel for speed
        val safeStartX = startX.coerceIn(0, bitmap.width - 1)
        val safeEndX = endX.coerceIn(safeStartX + 1, bitmap.width)
        val safeStartY = startY.coerceIn(0, bitmap.height - 1)
        val safeEndY = endY.coerceIn(safeStartY + 1, bitmap.height)

        for (y in safeStartY until safeEndY step step) {
            for (x in safeStartX until safeEndX step step) {
                val pixel = bitmap.getPixel(x, y)
                redSum += android.graphics.Color.red(pixel)
                greenSum += android.graphics.Color.green(pixel)
                blueSum += android.graphics.Color.blue(pixel)
                count++
            }
        }

        if (count == 0) return Color(0xFF0F172A)

        val avgR = (redSum / count).toInt()
        val avgG = (greenSum / count).toInt()
        val avgB = (blueSum / count).toInt()

        return Color(android.graphics.Color.rgb(avgR, avgG, avgB))
    }

    private fun extractSwatches(bitmap: Bitmap, width: Int, height: Int): List<Color> {
        val colors = mutableListOf<Color>()
        val gridRows = 3
        val gridCols = 3
        val rowStep = height / gridRows
        val colStep = width / gridCols

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val startX = c * colStep
                val startY = r * rowStep
                val color = calculateAverageColor(
                    bitmap,
                    startX,
                    (startX + colStep).coerceAtMost(width),
                    startY,
                    (startY + rowStep).coerceAtMost(height)
                )
                if (!colors.contains(color)) {
                    colors.add(color)
                }
            }
        }

        return colors.take(8)
    }

    private fun calculateLuminance(color: Color): Float {
        val argb = color.toArgb()
        val r = android.graphics.Color.red(argb) / 255f
        val g = android.graphics.Color.green(argb) / 255f
        val b = android.graphics.Color.blue(argb) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }
}
