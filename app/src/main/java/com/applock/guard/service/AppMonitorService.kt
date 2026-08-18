package com.applock.guard.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
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
    private var isCurrentlyShowingLock = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AppMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AppMonitorService onStartCommand")

        // Handle unlock confirmation from LockScreenActivity
        val unlockedPackage = intent?.getStringExtra(EXTRA_UNLOCKED_PACKAGE)
        if (unlockedPackage != null) {
            isCurrentlyShowingLock = false
            markAsUnlocked(unlockedPackage)
            return START_STICKY
        }

        val lockDismissed = intent?.getBooleanExtra(EXTRA_LOCK_DISMISSED, false) ?: false
        if (lockDismissed) {
            isCurrentlyShowingLock = false
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
                        !isAppLockInternal(currentPackage)
                    ) {
                        if (currentPackage != lastForegroundPackage) {
                            val isLocked = repository.isAppLocked(currentPackage)
                            val isRecentlyUnlocked = isRecentlyUnlocked(currentPackage)

                            if (isLocked && !isRecentlyUnlocked && !isCurrentlyShowingLock) {
                                Log.d(TAG, "🚨 Locked app opened in Real-Time: $currentPackage")
                                isCurrentlyShowingLock = true
                                showLockScreen(currentPackage)
                            }
                        }
                    }

                    lastForegroundPackage = currentPackage
                } catch (e: Exception) {
                    Log.e(TAG, "Error in real-time monitor", e)
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Highly responsive real-time foreground app detection via UsageEvents
     */
    private fun getForegroundPackageName(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 4000 // Query last 4 seconds

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var foregroundPackage: String? = null

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                foregroundPackage = event.packageName
            }
        }

        return foregroundPackage
    }

    private fun isAppLockInternal(pkg: String): Boolean {
        return pkg == packageName || pkg == "$packageName.debug"
    }

    private fun showLockScreen(packageName: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(LockScreenActivity.EXTRA_LOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun markAsUnlocked(packageName: String) {
        val timeoutSec = AppLockApplication.instance.securePreferences.relockTimeoutSeconds
        val timeoutMs = if (timeoutSec == 0) 3000L else timeoutSec * 1000L
        recentlyUnlocked[packageName] = System.currentTimeMillis() + timeoutMs
        Log.d(TAG, "Marked $packageName as unlocked for ${timeoutMs / 1000}s")
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AppMonitorService destroyed")
    }

    companion object {
        private const val TAG = "AppMonitorService"
        private const val POLL_INTERVAL_MS = 150L // 150ms real-time ultra-fast polling
        const val EXTRA_UNLOCKED_PACKAGE = "extra_unlocked_package"
        const val EXTRA_LOCK_DISMISSED = "extra_lock_dismissed"

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            context.stopService(intent)
        }

        fun notifyUnlocked(context: Context, packageName: String) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                putExtra(EXTRA_UNLOCKED_PACKAGE, packageName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyDismissed(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java).apply {
                putExtra(EXTRA_LOCK_DISMISSED, true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
