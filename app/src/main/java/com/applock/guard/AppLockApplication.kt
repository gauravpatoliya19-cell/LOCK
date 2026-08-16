package com.applock.guard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.applock.guard.data.db.AppLockDatabase
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.data.repository.AppRepository

class AppLockApplication : Application() {

    lateinit var database: AppLockDatabase
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = AppLockDatabase.getInstance(this)

        // Initialize encrypted preferences
        securePreferences = SecurePreferences(this)

        // Initialize repository
        repository = AppRepository(
            lockedAppDao = database.lockedAppDao(),
            securePreferences = securePreferences,
            context = this
        )

        // Create notification channel for foreground service
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "app_lock_service"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: AppLockApplication
            private set
    }
}
