package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A named pet care routine (e.g. "Morning routine") that repeats daily or on chosen weekdays.
 * Its individual steps are stored as [TaskEntity] rows linked by routine_id.
 */
@Entity(
    tableName = "routines",
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
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pet_id")
    val petId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "repeat_type")
    val repeatType: String = REPEAT_DAILY,

    @ColumnInfo(name = "days_of_week") // CSV of Calendar.DAY_OF_WEEK values (1=Sunday ... 7=Saturday)
    val daysOfWeek: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun days(): List<Int> = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }

    fun isScheduledOn(dayOfWeek: Int): Boolean =
        repeatType == REPEAT_DAILY || dayOfWeek in days()

    companion object {
        const val REPEAT_DAILY = "DAILY"
        const val REPEAT_WEEKLY = "WEEKLY"
    }
}
