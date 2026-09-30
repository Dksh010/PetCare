package com.petcare.app.util

/**
 * App-wide constants for PetCare.
 */
object Constants {
    const val DATABASE_NAME = "pet_care_database"
    const val PREF_NAME = "petcare_session_prefs"

    // Intent Extras
    const val EXTRA_PET_ID = "extra_pet_id"
    const val EXTRA_IS_EDIT_MODE = "extra_is_edit_mode"

    // Task Categories
    const val CATEGORY_FEEDING = "Feeding"
    const val CATEGORY_EXERCISE = "Exercise"
    const val CATEGORY_GROOMING = "Grooming"
    const val CATEGORY_MEDICATION = "Medication"
    const val CATEGORY_HEALTHCARE = "Healthcare"

    // Animation Durations
    const val ANIM_DURATION_SHORT = 300L
    const val ANIM_DURATION_MEDIUM = 500L
}