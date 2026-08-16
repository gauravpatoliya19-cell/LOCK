package com.applock.guard.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.applock.guard.AppLockApplication
import com.applock.guard.MainActivity
import com.applock.guard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val recentlyUnlocked = mutableMapOf<String, Long>()
    private var lastForegroundPackage: String? = null

    override fun onBind(intent: IBinder?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppMonitorService started")

        // Handle unlock action from LockScreenActivity
        val unlockedPackage = intent?.getStringExtra(EXTRA_UNLOCKED_PACKAGE)
        if (unlockedPackage != null) {
            markAsUnlocked(unlockedPackage)
            return START_STICKY
        }

        startForeground(AppLockApplication.NOTIFICATION_ID, createNotification())
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            val repository = AppLockApplication.instance.repository

            while (isActive) {
                try {
                    val currentPackage = getForegroundPackageName()

                    if (currentPackage != null &&
                        currentPackage != packageName &&
                        currentPackage != lastForegroundPackage
                    ) {
                        // Check if this app is locked
                        val isLocked = repository.isAppLocked(currentPackage)
                        val isRecentlyUnlocked = isRecentlyUnlocked(currentPackage)

                        if (isLocked && !isRecentlyUnlocked) {
                            Log.d(TAG, "Locked app detected: $currentPackage")
                            showLockScreen(currentPackage)
                        }
                    }

                    lastForegroundPackage = currentPackage
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring apps", e)
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Gets the package name of the currently foreground app using UsageStatsManager.
     */
    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000 // Look back 5 seconds

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var foregroundPackage: String? = null

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPackage = event.packageName
            }
        }

        return foregroundPackage
    }

    private fun showLockScreen(packageName: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockScreenActivity.EXTRA_LOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun markAsUnlocked(packageName: String) {
        val timeout = AppLockApplication.instance.securePreferences.relockTimeoutSeconds * 1000L
        recentlyUnlocked[packageName] = System.currentTimeMillis() + timeout
        Log.d(TAG, "Marked $packageName as unlocked for ${timeout / 1000}s")
    }

    private fun isRecentlyUnlocked(packageName: String): Boolean {
        val expiry = recentlyUnlocked[packageName] ?: return false
        if (System.currentTimeMillis() > expiry) {
            recentlyUnlocked.remove(packageName)
            return false
        }
        return true
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AppLockApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AppMonitorService destroyed")
    }

    companion object {
        private const val TAG = "AppMonitorService"
        private const val POLL_INTERVAL_MS = 500L
        const val EXTRA_UNLOCKED_PACKAGE = "extra_unlocked_package"

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }

        fun notifyUnlocked(context: Context, packageName: String) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                putExtra(EXTRA_UNLOCKED_PACKAGE, packageName)
            }
            context.startService(intent)
        }

        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (AppMonitorService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}
