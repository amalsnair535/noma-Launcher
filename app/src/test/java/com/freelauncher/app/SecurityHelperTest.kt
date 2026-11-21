package com.freelauncher.app

import com.freelauncher.app.ui.util.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityHelperTest {

    @Test
    fun testSaltGeneration_UniqueAndNonEmpty() {
        val salt1 = SecurityHelper.generateSalt()
        val salt2 = SecurityHelper.generateSalt()

        assertTrue(salt1.isNotBlank())
        assertTrue(salt2.isNotBlank())
        assertNotEquals(salt1, salt2)
        assertEquals(32, salt1.length) // 16 bytes = 32 hex chars
    }

    @Test
    fun testPinHashing_ConsistentWithSameSalt() {
        val salt = SecurityHelper.generateSalt()
        val hash1 = SecurityHelper.hashPin("1234", salt)
        val hash2 = SecurityHelper.hashPin("1234", salt)
        val hashDifferentPin = SecurityHelper.hashPin("4321", salt)

        assertTrue(hash1.isNotBlank())
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hashDifferentPin)
    }

    @Test
    fun testPinVerification_ValidAndInvalidPins() {
        val (salt, hash) = SecurityHelper.createPinHashPair("5678")

        assertTrue(SecurityHelper.verifyPin("5678", salt, hash))
        assertFalse(SecurityHelper.verifyPin("1234", salt, hash))
        assertFalse(SecurityHelper.verifyPin("", salt, hash))
        assertFalse(SecurityHelper.verifyPin("5678", "", hash))
    }
}
