package com.petcare.app.util

import java.security.MessageDigest

/**
 * Security utility handling password hashing for local user authentication.
 */
object SecurityUtils {

    /**
     * Hashes a raw password string using SHA-256.
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Validates raw input against a stored hash.
     */
    fun verifyPassword(rawInput: String, storedHash: String): Boolean {
        return hashPassword(rawInput) == storedHash
    }
}