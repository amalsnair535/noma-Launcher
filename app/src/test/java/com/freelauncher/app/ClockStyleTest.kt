package com.freelauncher.app

import com.freelauncher.app.ui.components.ClockStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ClockStyleTest {

    @Test
    fun `bubbly 3d clock style is defined with correct id`() {
        val bubblyStyle = ClockStyle.entries.find { it.id == "bubbly_3d" }
        assertNotNull("BUBBLY_3D clock style should exist", bubblyStyle)
        assertEquals("bubbly_3d", bubblyStyle?.id)
        assertEquals("Bubbly 3D", bubblyStyle?.displayName)
    }

    @Test
    fun `all clock styles have unique non-empty ids`() {
        val ids = ClockStyle.entries.map { it.id }
        assertEquals("Clock style IDs should be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `advanced clock styles flip timeline outline are defined correctly`() {
        val flip = ClockStyle.entries.find { it.id == "flip" }
        assertNotNull("FLIP clock style should exist", flip)
        assertEquals("flip", flip?.id)

        val timeline = ClockStyle.entries.find { it.id == "timeline" }
        assertNotNull("TIMELINE clock style should exist", timeline)
        assertEquals("timeline", timeline?.id)

        val outline = ClockStyle.entries.find { it.id == "outline" }
        assertNotNull("OUTLINE clock style should exist", outline)
        assertEquals("outline", outline?.id)

        val rotatingWheels = ClockStyle.entries.find { it.id == "rotating_wheels" }
        assertNotNull("ROTATING_WHEELS clock style should exist", rotatingWheels)
        assertEquals("rotating_wheels", rotatingWheels?.id)
    }
}
