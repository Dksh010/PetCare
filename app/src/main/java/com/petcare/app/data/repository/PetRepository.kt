package com.petcare.app.data.repository

import com.petcare.app.data.local.*
import com.petcare.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Extended Repository bridging all Room DAOs to the ViewModel layer.
 */
class PetRepository(
    private val petDao: PetDao,
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val reminderDao: ReminderDao,
    private val appointmentDao: AppointmentDao,
    private val activityLogDao: ActivityLogDao,
    private val vaccinationDao: VaccinationDao
) {

    // --- PET OPERATIONS ---
    fun getAllPets(userId: Long): Flow<List<PetEntity>> = petDao.getAllPets(userId)

    suspend fun insertPet(pet: PetEntity): Long = petDao.insertPet(pet)
    suspend fun updatePet(pet: PetEntity) = petDao.updatePet(pet)
    suspend fun deletePet(pet: PetEntity) = petDao.deletePet(pet)
    suspend fun getPetById(petId: Long): PetEntity? = petDao.getPetById(petId)

    // --- TASK OPERATIONS ---
    fun getTasksForPet(petId: Long): Flow<List<TaskEntity>> = taskDao.getTasksForPet(petId)
    val pendingTasks: Flow<List<TaskEntity>> = taskDao.getPendingTasks()
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)
    suspend fun setTaskCompletionStatus(taskId: Long, isCompleted: Boolean) = taskDao.updateTaskStatus(taskId, isCompleted)

    // --- USER / AUTH OPERATIONS ---
    suspend fun insertUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    suspend fun getUserById(userId: Long): UserEntity? = userDao.getUserById(userId)
    suspend fun deleteAccount(userId: Long) {
        petDao.deletePetsForUser(userId)
        userDao.deleteUserById(userId)
    }

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
    suspend fun deleteActivityLog(log: ActivityLogEntity) = activityLogDao.deleteActivityLog(log)

    // --- VACCINATION OPERATIONS ---
    fun getVaccinationsForPet(petId: Long): Flow<List<VaccinationEntity>> = vaccinationDao.getVaccinationsForPet(petId)
    suspend fun insertVaccination(vaccination: VaccinationEntity): Long = vaccinationDao.insertVaccination(vaccination)
    suspend fun updateVaccination(vaccination: VaccinationEntity) = vaccinationDao.updateVaccination(vaccination)
    suspend fun deleteVaccination(vaccination: VaccinationEntity) = vaccinationDao.deleteVaccination(vaccination)
}