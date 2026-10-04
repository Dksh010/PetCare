package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.AppointmentEntity
import com.petcare.app.data.model.PlaceVisit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments WHERE pet_id = :petId ORDER BY date_millis ASC")
    fun getAppointmentsForPet(petId: Long): Flow<List<AppointmentEntity>>

    @Query(
        "SELECT a.title AS title, p.name AS petName, a.date_millis AS whenMillis " +
            "FROM appointments a JOIN pets p ON p.id = a.pet_id " +
            "WHERE a.location_id = :locationId ORDER BY a.date_millis DESC"
    )
    suspend fun getVisitsForLocation(locationId: Long): List<PlaceVisit>
}
