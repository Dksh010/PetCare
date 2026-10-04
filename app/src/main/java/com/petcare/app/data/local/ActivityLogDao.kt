package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petcare.app.data.model.ActivityLogEntity
import com.petcare.app.data.model.PlaceVisit
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    @Delete
    suspend fun deleteActivityLog(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_logs WHERE pet_id = :petId ORDER BY timestamp DESC")
    fun getActivityLogsForPet(petId: Long): Flow<List<ActivityLogEntity>>

    @Query(
        "SELECT l.type AS title, p.name AS petName, l.timestamp AS whenMillis " +
            "FROM activity_logs l JOIN pets p ON p.id = l.pet_id " +
            "WHERE l.location_id = :locationId ORDER BY l.timestamp DESC"
    )
    suspend fun getVisitsForLocation(locationId: Long): List<PlaceVisit>
}
