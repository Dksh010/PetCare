package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    @Delete
    suspend fun deleteActivityLog(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_logs WHERE pet_id = :petId ORDER BY timestamp DESC")
    fun getActivityLogsForPet(petId: Long): Flow<List<ActivityLogEntity>>

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllActivityLogs()
}