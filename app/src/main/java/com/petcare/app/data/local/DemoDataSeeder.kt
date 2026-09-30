package com.petcare.app.data.local

import android.content.Context
import com.petcare.app.data.model.*
import com.petcare.app.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds the database with demo personas and records for presentation purposes.
 */
object DemoDataSeeder {

    suspend fun seedDataIfEmpty(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()
        val petDao = db.petDao()
        val taskDao = db.taskDao()
        val logDao = db.activityLogDao()

        withContext(Dispatchers.IO) {
            var emily = userDao.getUserByEmail("emily@petcare.com")
            
            if (emily == null) {
                val emilyId = userDao.insertUser(UserEntity(
                    name = "Emily",
                    email = "emily@petcare.com",
                    passwordHash = SecurityUtils.hashPassword("password123")
                ))
                emily = userDao.getUserById(emilyId)
            }

            val emilyId = emily!!.id

            // Check if pets for Emily exist (simple check by ID 1 which is usually Max)
            if (petDao.getPetById(1L) == null) {
                val maxId = petDao.insertPet(PetEntity(
                    userId = emilyId,
                    name = "Max",
                    species = "Dog",
                    breed = "Golden Retriever",
                    age = 3,
                    ownerContact = "555-0101",
                    weight = "28kg",
                    gender = "Male",
                    bodySize = "Large",
                    dietaryPrefs = "High-protein dry food",
                    allergies = "None"
                ))

                petDao.insertPet(PetEntity(
                    userId = emilyId,
                    name = "Luna",
                    species = "Cat",
                    breed = "Siamese",
                    age = 2,
                    ownerContact = "555-0101",
                    weight = "4.5kg",
                    gender = "Female",
                    bodySize = "Small",
                    dietaryPrefs = "Wet food once daily"
                ))

                taskDao.insertTask(TaskEntity(petId = maxId, title = "Morning Walk", category = "Exercise", dueDate = "08:00 AM", description = "Park walk"))
                taskDao.insertTask(TaskEntity(petId = maxId, title = "Breakfast", category = "Feeding", dueDate = "08:30 AM", description = "1 cup kibble"))
                taskDao.insertTask(TaskEntity(petId = maxId, title = "Evening Walk", category = "Exercise", dueDate = "06:00 PM", description = "Street walk"))

                logDao.insertActivityLog(ActivityLogEntity(petId = maxId, type = "Walk", duration = 30, notes = "Max was very active today!"))
            }
        }
    }
}