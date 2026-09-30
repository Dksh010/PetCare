package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an extended Pet profile in the local Room database.
 */
@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class PetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long = 1L, // Default to 1L for migration safety, will be updated correctly for new pets

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "species")
    val species: String, // e.g., Dog, Cat, Bird

    @ColumnInfo(name = "breed")
    val breed: String,

    @ColumnInfo(name = "age")
    val age: Int,

    @ColumnInfo(name = "owner_contact")
    val ownerContact: String,

    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null,

    @ColumnInfo(name = "weight")
    val weight: String = "",

    @ColumnInfo(name = "dietary_prefs")
    val dietaryPrefs: String = "",

    @ColumnInfo(name = "vaccination_history")
    val vaccinationHistory: String = "",

    @ColumnInfo(name = "allergies")
    val allergies: String = "",

    @ColumnInfo(name = "favorite_toys")
    val favoriteToys: String = "",

    // New Fields requested for extensive profile management
    @ColumnInfo(name = "gender")
    val gender: String = "",

    @ColumnInfo(name = "birthday")
    val birthday: String = "",

    @ColumnInfo(name = "sterilized")
    val sterilized: Boolean = false,

    @ColumnInfo(name = "fur_color")
    val furColor: String = "",

    @ColumnInfo(name = "body_size")
    val bodySize: String = "Medium", // Small, Medium, Large, X-Large

    @ColumnInfo(name = "dietary_restrictions")
    val dietaryRestrictions: String = "",

    @ColumnInfo(name = "current_medications")
    val currentMedications: String = "",

    @ColumnInfo(name = "medical_conditions")
    val medicalConditions: String = "",

    @ColumnInfo(name = "emergency_contact_name")
    val emergencyContactName: String = "",

    @ColumnInfo(name = "emergency_contact_phone")
    val emergencyContactPhone: String = "",

    @ColumnInfo(name = "emergency_contact_relationship")
    val emergencyContactRelationship: String = "",

    @ColumnInfo(name = "veterinarian_name")
    val veterinarianName: String = "",

    @ColumnInfo(name = "veterinarian_clinic")
    val veterinarianClinic: String = "",

    @ColumnInfo(name = "veterinarian_phone")
    val veterinarianPhone: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)