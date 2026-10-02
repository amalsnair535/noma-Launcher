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
}
