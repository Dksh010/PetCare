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
 * Data Access Object for routine steps stored in the 'tasks' table.
 */
@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE pet_id = :petId ORDER BY time ASC")
    fun getTasksForPet(petId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE routine_id = :routineId ORDER BY time ASC")
    suspend fun getTasksForRoutine(routineId: Long): List<TaskEntity>

    @Query("UPDATE tasks SET completed_on = :completedOn WHERE id = :taskId")
    suspend fun setCompletedOn(taskId: Long, completedOn: String)
}
