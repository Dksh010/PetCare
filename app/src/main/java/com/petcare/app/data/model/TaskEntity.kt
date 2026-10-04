package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single step of a pet care routine (e.g. "Breakfast – 1 cup kibble at 08:00").
 * Completion is tracked per day: [completedOn] holds the date (yyyy-MM-dd) it was last ticked,
 * so the checklist resets automatically every day.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pet_id"]), Index(value = ["routine_id"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pet_id")
    val petId: Long,

    @ColumnInfo(name = "routine_id")
    val routineId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "category")
    val category: String, // Feeding, Walk, Medication, Grooming, Play, Training, Other

    @ColumnInfo(name = "time") // 24h HH:mm so it sorts correctly
    val time: String,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "completed_on")
    val completedOn: String = ""
) {
    fun isDoneOn(date: String): Boolean = completedOn == date
}
