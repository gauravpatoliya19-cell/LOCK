package com.applock.guard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.applock.guard.AppLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val recentlyUnlocked = mutableMapOf<String, Long>()
    private var lastPackageName: String? = null
    private var isLockScreenShowing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "⚡ AppLock Accessibility Service Connected & Active!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Ignore our own App Lock app activities
        if (packageName == this.packageName || packageName == "${this.packageName}.debug") {
            if (className.contains("LockScreenActivity")) {
                isLockScreenShowing = true
            }
            return
        }

        // Ignore Android System UI navigation / keyboard
        if (packageName == "com.android.systemui" || packageName == "com.google.android.inputmethod.latin") {
            return
        }

        if (packageName != lastPackageName) {
            lastPackageName = packageName
            checkAndLockApp(packageName)
        }
    }

    private fun checkAndLockApp(packageName: String) {
        serviceScope.launch {
            try {
                val repository = AppLockApplication.instance.repository
                val isLocked = repository.isAppLocked(packageName)
                val isRecentlyUnlocked = isRecentlyUnlocked(packageName)

                if (isLocked && !isRecentlyUnlocked && !isLockScreenShowing) {
                    Log.d(TAG, "🚨 REAL-TIME LOCK TRIGGERED for: $packageName")
                    isLockScreenShowing = true
                    showLockOverlay(packageName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking app lock", e)
            }
        }
    }

    private fun showLockOverlay(packageName: String) {
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

    fun markAsUnlocked(packageName: String) {
        isLockScreenShowing = false
        val timeoutSec = AppLockApplication.instance.securePreferences.relockTimeoutSeconds
        val timeoutMs = if (timeoutSec == 0) 3000L else timeoutSec * 1000L
        recentlyUnlocked[packageName] = System.currentTimeMillis() + timeoutMs
        Log.d(TAG, "Marked $packageName as unlocked for ${timeoutMs / 1000}s")
    }

    fun markLockDismissed() {
        isLockScreenShowing = false
    }

    private fun isRecentlyUnlocked(packageName: String): Boolean {
        val expiry = recentlyUnlocked[packageName] ?: return false
        if (System.currentTimeMillis() > expiry) {
            recentlyUnlocked.remove(packageName)
            return false
        }
        return true
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppLock Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "AppLock Accessibility Service Destroyed")
    }

    companion object {
        private const val TAG = "AppLockA11yService"
        var instance: AppLockAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun unlockPackage(packageName: String) {
            instance?.markAsUnlocked(packageName)
        }

        fun lockDismissed() {
            instance?.markLockDismissed()
        }
    }
}
