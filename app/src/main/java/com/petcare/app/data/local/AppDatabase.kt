package com.petcare.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.petcare.app.data.model.*

@Database(
    entities = [
        UserEntity::class,
        PetEntity::class,
        RoutineEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        AppointmentEntity::class,
        ActivityLogEntity::class,
        VaccinationEntity::class,
        LocationEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun petDao(): PetDao
    abstract fun routineDao(): RoutineDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun vaccinationDao(): VaccinationDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pet_care_database"
                )
                    // Schema was redesigned in v7; older development databases are rebuilt.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
