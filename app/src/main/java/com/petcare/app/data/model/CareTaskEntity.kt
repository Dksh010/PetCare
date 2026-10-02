package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a care task for a pet (e.g., feeding, walking, medication).
 */
@Entity(
    tableName = "care_tasks",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pet_id"])]
)
data class CareTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pet_id")
    val petId: Long = 0,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "task_type") // e.g., FEEDING, WALK, MEDICATION, GROOMING
    val taskType: String = "",

    @ColumnInfo(name = "schedule_type") // DAILY, WEEKLY, ONETIME
    val scheduleType: String = "",

    @ColumnInfo(name = "time_of_day") // HH:mm format for daily tasks
    val timeOfDay: String = "",

    @ColumnInfo(name = "days_of_week") // CSV of calendar days (1=Sunday, ..., 7=Saturday) for weekly
    val daysOfWeek: String = "",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "last_completed")
    val lastCompleted: Long = 0, // timestamp

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)