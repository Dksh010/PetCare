package com.petcare.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petcare.app.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for performing database operations on the 'tasks' table.
 */
@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE pet_id = :petId ORDER BY due_date ASC")
    fun getTasksForPet(petId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY due_date ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET is_completed = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, isCompleted: Boolean)
}