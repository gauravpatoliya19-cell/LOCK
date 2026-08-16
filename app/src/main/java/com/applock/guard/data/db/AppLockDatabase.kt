package com.applock.guard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [LockedAppEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppLockDatabase : RoomDatabase() {

    abstract fun lockedAppDao(): LockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppLockDatabase? = null

        fun getInstance(context: Context): AppLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLockDatabase::class.java,
                    "app_lock_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
