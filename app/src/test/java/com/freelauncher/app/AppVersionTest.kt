package com.freelauncher.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppVersionTest {

    @Test
    fun `verify build config version name is defined`() {
        assertNotNull(BuildConfig.VERSION_NAME)
        assertFalse(BuildConfig.VERSION_NAME.isBlank())
    }
}
