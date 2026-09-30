package com.petcare.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vaccinations",
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
data class VaccinationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "pet_id")
    val petId: Long,

    @ColumnInfo(name = "vaccine_name")
    val vaccineName: String,

    @ColumnInfo(name = "date_given")
    val dateGiven: String,

    @ColumnInfo(name = "next_due_date")
    val nextDueDate: String,

    @ColumnInfo(name = "notes")
    val notes: String = ""
)