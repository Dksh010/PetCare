package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.CareTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CareTaskDao {

    @Insert
    suspend fun insertTask(task: CareTaskEntity): Long

    @Update
    suspend fun updateTask(task: CareTaskEntity)

    @Delete
    suspend fun deleteTask(task: CareTaskEntity)

    @Query("DELETE FROM care_tasks WHERE pet_id = :petId")
    suspend fun deleteTasksForPet(petId: Long)

    @Query("SELECT * FROM care_tasks WHERE pet_id = :petId ORDER BY time_of_day ASC")
    fun getTasksForPet(petId: Long): Flow<List<CareTaskEntity>>

    @Query("SELECT * FROM care_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): CareTaskEntity?

    @Query("UPDATE care_tasks SET is_completed = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean)
}