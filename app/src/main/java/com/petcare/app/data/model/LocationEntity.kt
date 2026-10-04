package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A place the user has geotagged (vet clinic, grooming salon, dog park, pet store, shelter).
 * Appointments and activity logs can be linked to it and are shown on the map.
 */
@Entity(
    tableName = "locations",
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
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "location_type") // see PlaceTypes: VET, GROOMING, PARK, SHOP, SHELTER, OTHER
    val locationType: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/** A row describing something that happened (or will happen) at a geotagged place. */
data class PlaceVisit(
    val title: String,
    val petName: String,
    val whenMillis: Long
)
