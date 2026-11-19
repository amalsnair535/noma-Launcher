package com.freelauncher.app

import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.InstalledAppMatcher
import com.freelauncher.app.ui.util.ScribblePoint
import com.freelauncher.app.ui.util.ScribbleSearchState
import com.freelauncher.app.ui.util.ScribbleStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScribbleSearchTest {

    private val sampleApps = listOf(
        AppItem(id = "1", label = "Airbnb", packageName = "com.airbnb.android"),
        AppItem(id = "2", label = "Amazon", packageName = "com.amazon.mShop.android.shopping"),
        AppItem(id = "3", label = "BookMyShow", packageName = "com.bt.bms"),
        AppItem(id = "4", label = "Chrome", packageName = "com.android.chrome"),
        AppItem(id = "5", label = "Calculator", packageName = "com.google.android.calculator")
    )

    @Test
    fun testDrawSingleLetter_FiltersAppsImmediately() {
        val results = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = sampleApps,
            query = "A",
            isScribbleSearch = true
        )

        assertTrue(results.size >= 2)
        assertEquals("Airbnb", results[0].app.label)
        assertEquals(0, results[0].highlightStart)
        assertEquals(1, results[0].highlightEnd)

        assertEquals("Amazon", results[1].app.label)
        assertEquals(0, results[1].highlightStart)
        assertEquals(1, results[1].highlightEnd)
    }

    @Test
    fun testDrawSequence_AB_NarrowsResults() {
        val resultsSingle = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = sampleApps,
            query = "A",
            isScribbleSearch = true
        )
        assertTrue(resultsSingle.size >= 2)

        val resultsDouble = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = sampleApps,
            query = "Ai",
            isScribbleSearch = true
        )
        assertEquals(1, resultsDouble.size)
        assertEquals("Airbnb", resultsDouble[0].app.label)
        assertEquals(0, resultsDouble[0].highlightStart)
        assertEquals(2, resultsDouble[0].highlightEnd)
    }

    @Test
    fun testMultiStrokePreservationState() {
        var state = ScribbleSearchState()
        assertFalse(state.isActive)

        val stroke1 = ScribbleStroke(
            points = listOf(
                ScribblePoint(100f, 100f),
                ScribblePoint(150f, 200f)
            )
        )
        state = state.copy(strokes = listOf(stroke1), recognizedQuery = "A")
        assertTrue(state.isActive)
        assertEquals(1, state.strokes.size)

        val stroke2 = ScribbleStroke(
            points = listOf(
                ScribblePoint(200f, 100f),
                ScribblePoint(250f, 200f)
            )
        )
        state = state.copy(strokes = state.strokes + stroke2, recognizedQuery = "AB")
        assertEquals(2, state.strokes.size)
        assertEquals("AB", state.recognizedQuery)

        // Clear action resets state
        state = ScribbleSearchState()
        assertFalse(state.isActive)
        assertEquals(0, state.strokes.size)
    }

    @Test
    fun testBackspaceGesture_ShortensQuery() {
        var query = "ABC"
        query = query.dropLast(1)
        assertEquals("AB", query)

        val resultsAB = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = sampleApps,
            query = query,
            isScribbleSearch = true
        )
        assertTrue(resultsAB.isEmpty()) // No app starting with AB in sampleApps

        val queryA = query.dropLast(1)
        assertEquals("A", queryA)
        val resultsA = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = sampleApps,
            query = queryA,
            isScribbleSearch = true
        )
        assertTrue(resultsA.size >= 2)
    }

    @Test
    fun testRanking_ExactAndPrefixMatchesAbovePartial() {
        val apps = listOf(
            AppItem(id = "1", label = "My Chrome", packageName = "com.my.chrome"),
            AppItem(id = "2", label = "Chrome", packageName = "com.android.chrome")
        )

        val matches = InstalledAppMatcher.matchAppsWithHighlight(
            installedApps = apps,
            query = "Chrome",
            isScribbleSearch = false
        )

        assertEquals(2, matches.size)
        assertEquals("Chrome", matches[0].app.label)
        assertEquals("My Chrome", matches[1].app.label)
    }
}
