package com.freelauncher.app.ui.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityHelper {

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    /**
     * Generates a 16-byte cryptographically secure random salt in Hex format.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.toHexString()
    }

    /**
     * Computes a PBKDF2-HMAC-SHA256 hash for a PIN string using the provided salt.
     */
    fun hashPin(pin: String, saltHex: String): String {
        if (pin.isBlank() || saltHex.isBlank()) return ""
        try {
            val saltBytes = saltHex.decodeHex()
            val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            return hash.toHexString()
        } catch (e: Exception) {
            SafeCrashlytics.logException(e)
            return ""
        }
    }

    /**
     * Verifies an entered PIN against a stored salt and hash using constant-time comparison.
     */
    fun verifyPin(enteredPin: String, saltHex: String, targetHashHex: String): Boolean {
        if (enteredPin.isBlank() || saltHex.isBlank() || targetHashHex.isBlank()) return false
        val computedHash = hashPin(enteredPin, saltHex)
        if (computedHash.isBlank()) return false
        return MessageDigest.isEqual(computedHash.toByteArray(Charsets.UTF_8), targetHashHex.toByteArray(Charsets.UTF_8))
    }

    /**
     * Utility method to generate salt and hash for a new PIN.
     */
    fun createPinHashPair(pin: String): Pair<String, String> {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        return Pair(salt, hash)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
