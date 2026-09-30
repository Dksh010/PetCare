package com.petcare.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local user authentication state and session persistence using SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "petcare_session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PROFILE_IMAGE_URI = "profile_image_uri"
    }

    /**
     * Saves active session details upon successful login or sign-up.
     */
    fun createSession(userId: Long, email: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun saveProfileImageUri(uri: String?) {
        prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri).apply()
    }

    fun getProfileImageUri(): String? = prefs.getString(KEY_PROFILE_IMAGE_URI, null)

    /**
     * Returns true if a valid user session exists.
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Retrieves the email of the currently logged-in user.
     */
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    /**
     * Retrieves the stored user ID. Returns -1 if no session exists.
     */
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    /**
     * Clears all session keys to log the user out.
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
}