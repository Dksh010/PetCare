package com.petcare.app.data.local

import android.content.Context
import com.petcare.app.data.model.*
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Seeds a demo account (emily@petcare.com / Password1!) with pets, routines, reminders,
 * an appointment and a geotagged place. Runs only when the demo user does not exist yet,
 * so anything the user deletes stays deleted.
 */
object DemoDataSeeder {

    const val DEMO_EMAIL = "emily@petcare.com"
    const val DEMO_PASSWORD = "Password1!"

    suspend fun seedDataIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val repo = PetRepository.from(context)
        if (repo.getUserByEmail(DEMO_EMAIL) != null) return@withContext

        val emilyId = repo.insertUser(
            UserEntity(name = "Emily", email = DEMO_EMAIL, passwordHash = SecurityUtils.hashPassword(DEMO_PASSWORD))
        )

        val maxId = repo.insertPet(PetEntity(
            userId = emilyId, name = "Max", species = "Dog", breed = "Golden Retriever", age = 3,
            ownerContact = "555-0101", weight = "28kg", gender = "Male", bodySize = "Large",
            dietaryPrefs = "High-protein dry food", allergies = "None",
            currentMedications = "Joint supplement with breakfast"
        ))
        val lunaId = repo.insertPet(PetEntity(
            userId = emilyId, name = "Luna", species = "Cat", breed = "Siamese", age = 2,
            ownerContact = "555-0101", weight = "4.5kg", gender = "Female", bodySize = "Small",
            dietaryPrefs = "Wet food once daily"
        ))

        repo.saveRoutine(
            RoutineEntity(petId = maxId, name = "Morning routine"),
            listOf(
                TaskEntity(petId = maxId, routineId = 0, title = "Morning walk", category = "Walk", time = "07:30", notes = "Park loop, 30 min"),
                TaskEntity(petId = maxId, routineId = 0, title = "Breakfast", category = "Feeding", time = "08:00", notes = "1 cup kibble"),
                TaskEntity(petId = maxId, routineId = 0, title = "Joint supplement", category = "Medication", time = "08:05", notes = "1 tablet in food")
            )
        )
        repo.saveRoutine(
            RoutineEntity(petId = maxId, name = "Evening routine"),
            listOf(
                TaskEntity(petId = maxId, routineId = 0, title = "Dinner", category = "Feeding", time = "18:00", notes = "1 cup kibble"),
                TaskEntity(petId = maxId, routineId = 0, title = "Evening walk", category = "Walk", time = "19:00")
            )
        )
        repo.saveRoutine(
            RoutineEntity(petId = maxId, name = "Grooming day", repeatType = RoutineEntity.REPEAT_WEEKLY,
                daysOfWeek = "${Calendar.SATURDAY}"),
            listOf(TaskEntity(petId = maxId, routineId = 0, title = "Brush coat", category = "Grooming", time = "10:00"))
        )
        repo.saveRoutine(
            RoutineEntity(petId = lunaId, name = "Daily care"),
            listOf(
                TaskEntity(petId = lunaId, routineId = 0, title = "Wet food", category = "Feeding", time = "08:30"),
                TaskEntity(petId = lunaId, routineId = 0, title = "Clean litter box", category = "Other", time = "20:00")
            )
        )

        repo.insertReminder(ReminderEntity(petId = maxId, title = "Flea treatment", type = "Medication", time = "09:00"))

        val clinicId = repo.insertLocation(LocationEntity(
            userId = emilyId, name = "Kathmandu Vet Clinic", address = "Lazimpat, Kathmandu",
            latitude = 27.7180, longitude = 85.3250, locationType = "VET"
        ))
        val parkId = repo.insertLocation(LocationEntity(
            userId = emilyId, name = "Central Dog Park", address = "Baluwatar, Kathmandu",
            latitude = 27.7200, longitude = 85.3300, locationType = "PARK"
        ))

        val inThreeDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
        }.timeInMillis
        repo.insertAppointment(AppointmentEntity(
            petId = maxId, title = "Annual check-up", clinic = "Kathmandu Vet Clinic",
            dateMillis = inThreeDays, locationId = clinicId
        ))

        val nextYear = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis
        repo.insertVaccination(VaccinationEntity(petId = maxId, vaccineName = "Rabies", nextDueDate = nextYear))

        repo.insertActivityLog(ActivityLogEntity(
            petId = maxId, type = "Walk", duration = 30, notes = "Max was very active today!", locationId = parkId
        ))
    }
}
