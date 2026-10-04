package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.VaccinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccinationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccination(vaccination: VaccinationEntity): Long

    @Update
    suspend fun updateVaccination(vaccination: VaccinationEntity)

    @Delete
    suspend fun deleteVaccination(vaccination: VaccinationEntity)

    @Query("SELECT * FROM vaccinations WHERE pet_id = :petId ORDER BY next_due_date ASC")
    fun getVaccinationsForPet(petId: Long): Flow<List<VaccinationEntity>>
}
