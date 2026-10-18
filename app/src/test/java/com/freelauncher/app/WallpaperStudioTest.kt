package com.freelauncher.app

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.freelauncher.app.ui.util.ImageThemeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WallpaperStudioTest {

    @Test
    fun testPaletteExtractionFromBitmap() {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until 30) {
                bitmap.setPixel(x, y, AndroidColor.RED)
            }
            for (y in 30 until 70) {
                bitmap.setPixel(x, y, AndroidColor.GREEN)
            }
            for (y in 70 until 100) {
                bitmap.setPixel(x, y, AndroidColor.BLUE)
            }
        }

        val palette = ImageThemeExtractor.extractPaletteFromBitmap(bitmap)

        assertNotNull(palette)
        assertEquals(0xFF, (palette.topColor.red * 255).toInt())
        assertEquals(0xFF, (palette.middleColor.green * 255).toInt())
        assertEquals(0xFF, (palette.bottomColor.blue * 255).toInt())
        assertTrue(palette.swatches.isNotEmpty())
    }
}
