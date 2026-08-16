package com.applock.guard.util

import java.security.MessageDigest

object CryptoHelper {

    /**
     * Hashes a string using SHA-256.
     * Used for storing PINs and patterns securely — never store plain text.
     */
    fun hashSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
