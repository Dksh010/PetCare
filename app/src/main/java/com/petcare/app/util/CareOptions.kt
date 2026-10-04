package com.petcare.app.util

/**
 * Fixed option lists shared by the routine editor, item dialogs, SMS delegation and the map.
 */
object CareOptions {

    const val CATEGORY_FEEDING = "Feeding"
    const val CATEGORY_MEDICATION = "Medication"

    val taskCategories = listOf(
        CATEGORY_FEEDING, "Walk", CATEGORY_MEDICATION, "Grooming", "Play", "Training", "Other"
    )

    val reminderTypes = listOf(
        CATEGORY_FEEDING, CATEGORY_MEDICATION, "Vaccination", "Grooming", "Vet Visit", "Other"
    )

    fun categoryEmoji(category: String): String = when (category) {
        CATEGORY_FEEDING -> "🍚"
        "Walk" -> "🏃"
        CATEGORY_MEDICATION -> "💊"
        "Grooming" -> "✂️"
        "Play" -> "🎾"
        "Training" -> "🎓"
        "Vaccination" -> "💉"
        "Vet Visit" -> "🩺"
        else -> "🐾"
    }
}

/**
 * Types of place that can be geotagged. [code] is what gets stored in LocationEntity.locationType.
 */
enum class PlaceType(val code: String, val label: String, val emoji: String) {
    VET("VET", "Veterinary clinic", "🩺"),
    GROOMING("GROOMING", "Grooming salon", "✂️"),
    PARK("PARK", "Dog park", "🎾"),
    SHOP("SHOP", "Pet supply store", "🛍️"),
    SHELTER("SHELTER", "Animal shelter", "🏠"),
    OTHER("OTHER", "Other place", "📍");

    companion object {
        fun fromCode(code: String): PlaceType = entries.firstOrNull { it.code == code } ?: OTHER
        fun fromLabel(label: String): PlaceType = entries.firstOrNull { it.label == label } ?: OTHER
    }
}
