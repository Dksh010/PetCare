package com.petcare.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.petcare.app.data.local.AppDatabase
import com.petcare.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository bridging all Room DAOs to the ViewModel layer.
 */
class PetRepository(private val db: AppDatabase) {

    private val petDao = db.petDao()
    private val routineDao = db.routineDao()
    private val taskDao = db.taskDao()
    private val userDao = db.userDao()
    private val reminderDao = db.reminderDao()
    private val appointmentDao = db.appointmentDao()
    private val activityLogDao = db.activityLogDao()
    private val vaccinationDao = db.vaccinationDao()
    private val locationDao = db.locationDao()

    // --- PET OPERATIONS ---
    fun getAllPets(userId: Long): Flow<List<PetEntity>> = petDao.getAllPets(userId)
    suspend fun insertPet(pet: PetEntity): Long = petDao.insertPet(pet)
    suspend fun updatePet(pet: PetEntity) = petDao.updatePet(pet)
    suspend fun deletePet(pet: PetEntity) = petDao.deletePet(pet)
    suspend fun getPetById(petId: Long): PetEntity? = petDao.getPetById(petId)

    // --- ROUTINE & TASK OPERATIONS ---
    fun getRoutinesForPet(petId: Long): Flow<List<RoutineEntity>> = routineDao.getRoutinesForPet(petId)
    fun getTasksForPet(petId: Long): Flow<List<TaskEntity>> = taskDao.getTasksForPet(petId)
    suspend fun getRoutineById(routineId: Long): RoutineEntity? = routineDao.getRoutineById(routineId)
    suspend fun getTasksForRoutine(routineId: Long): List<TaskEntity> = taskDao.getTasksForRoutine(routineId)

    /**
     * Inserts or updates a routine together with its steps. Steps missing from [steps]
     * are deleted; existing steps keep their id (and today's completion state).
     */
    suspend fun saveRoutine(routine: RoutineEntity, steps: List<TaskEntity>): Long = db.withTransaction {
        val routineId = if (routine.id == 0L) {
            routineDao.insertRoutine(routine)
        } else {
            routineDao.updateRoutine(routine)
            routine.id
        }
        val keptIds = steps.map { it.id }.filter { it != 0L }.toSet()
        taskDao.getTasksForRoutine(routineId)
            .filter { it.id !in keptIds }
            .forEach { taskDao.deleteTask(it) }
        steps.forEach { step ->
            val task = step.copy(routineId = routineId, petId = routine.petId)
            if (task.id == 0L) taskDao.insertTask(task) else taskDao.updateTask(task)
        }
        routineId
    }

    /** Re-inserts a deleted routine with its original ids (used by Undo). */
    suspend fun restoreRoutine(routine: RoutineEntity, steps: List<TaskEntity>) = db.withTransaction {
        routineDao.insertRoutine(routine)
        taskDao.insertTasks(steps)
    }

    suspend fun deleteRoutine(routine: RoutineEntity) = routineDao.deleteRoutine(routine)
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun setTaskCompletedOn(taskId: Long, date: String) = taskDao.setCompletedOn(taskId, date)

    // --- USER / AUTH OPERATIONS ---
    suspend fun insertUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun getUserById(userId: Long): UserEntity? = userDao.getUserById(userId)
    // Pets, places and all care data cascade from the user row.
    suspend fun deleteAccount(userId: Long) = userDao.deleteUserById(userId)

    // --- REMINDER OPERATIONS ---
    fun getRemindersForPet(petId: Long): Flow<List<ReminderEntity>> = reminderDao.getRemindersForPet(petId)
    suspend fun insertReminder(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)

    // --- APPOINTMENT OPERATIONS ---
    fun getAppointmentsForPet(petId: Long): Flow<List<AppointmentEntity>> = appointmentDao.getAppointmentsForPet(petId)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long = appointmentDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: AppointmentEntity) = appointmentDao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: AppointmentEntity) = appointmentDao.deleteAppointment(appointment)

    // --- ACTIVITY LOG OPERATIONS ---
    fun getActivityLogsForPet(petId: Long): Flow<List<ActivityLogEntity>> = activityLogDao.getActivityLogsForPet(petId)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long = activityLogDao.insertActivityLog(log)

    // --- VACCINATION OPERATIONS ---
    fun getVaccinationsForPet(petId: Long): Flow<List<VaccinationEntity>> = vaccinationDao.getVaccinationsForPet(petId)
    suspend fun insertVaccination(vaccination: VaccinationEntity): Long = vaccinationDao.insertVaccination(vaccination)
    suspend fun updateVaccination(vaccination: VaccinationEntity) = vaccinationDao.updateVaccination(vaccination)
    suspend fun deleteVaccination(vaccination: VaccinationEntity) = vaccinationDao.deleteVaccination(vaccination)

    // --- GEOTAGGED PLACES ---
    fun getLocationsForUser(userId: Long): Flow<List<LocationEntity>> = locationDao.getLocationsForUser(userId)
    suspend fun insertLocation(location: LocationEntity): Long = locationDao.insertLocation(location)
    suspend fun deleteLocation(location: LocationEntity) = locationDao.deleteLocation(location)

    /** Appointments and activities linked to a place, newest first. */
    suspend fun getVisitsForLocation(locationId: Long): List<PlaceVisit> =
        (appointmentDao.getVisitsForLocation(locationId) + activityLogDao.getVisitsForLocation(locationId))
            .sortedByDescending { it.whenMillis }

    companion object {
        fun from(context: Context) = PetRepository(AppDatabase.getDatabase(context))
    }
}
