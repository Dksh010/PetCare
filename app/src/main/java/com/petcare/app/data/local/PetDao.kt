package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.PetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for performing database operations on the 'pets' table.
 */
@Dao
interface PetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE user_id = :userId")
    suspend fun deletePetsForUser(userId: Long)

    @Query("SELECT * FROM pets WHERE user_id = :userId ORDER BY name ASC")
    fun getAllPets(userId: Long): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    suspend fun getPetById(petId: Long): PetEntity?
}