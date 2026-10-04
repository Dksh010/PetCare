package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["pet_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["pet_id"]), Index(value = ["location_id"])]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pet_id")
    val petId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "clinic")
    val clinic: String,

    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "status")
    val status: String = STATUS_SCHEDULED,

    @ColumnInfo(name = "location_id") // geotagged place, optional
    val locationId: Long? = null
) {
    val isCompleted: Boolean get() = status == STATUS_COMPLETED

    companion object {
        const val STATUS_SCHEDULED = "Scheduled"
        const val STATUS_COMPLETED = "Completed"
    }
}

