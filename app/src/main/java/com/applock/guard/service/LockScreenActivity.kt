package com.applock.guard.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.applock.guard.AppLockApplication
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.ui.screens.lock.LockScreen
import com.applock.guard.ui.theme.AppLockTheme
import com.applock.guard.util.BiometricHelper

class LockScreenActivity : FragmentActivity() {

    private var lockedPackage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make overlay appearance instant without transition lag
        overridePendingTransition(0, 0)

        // Set Window Flags to display immediately over any running app (WhatsApp, Instagram, etc.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: run {
            finish()
            return
        }

        val repository = AppLockApplication.instance.repository

        setContent {
            AppLockTheme {
                LockScreen(
                    title = "Verify Your Identity",
                    subtitle = "App is locked. Use fingerprint or password to unlock.",
                    lockType = repository.lockType,
                    isBiometricEnabled = repository.isBiometricEnabled,
                    onPinSubmit = { pin ->
                        if (repository.verifyPin(pin)) {
                            onUnlockSuccess()
                            true
                        } else {
                            false
                        }
                    },
                    onPatternSubmit = { pattern ->
                        if (repository.verifyPattern(pattern)) {
                            onUnlockSuccess()
                            true
                        } else {
                            false
                        }
                    },
                    onBiometricRequest = {
                        triggerBiometric()
                    },
                    onBackPressed = {
                        goToHomeScreen()
                    }
                )
            }
        }

        // Automatic Biometric Trigger the moment overlay pops up!
        if (repository.isBiometricEnabled &&
            repository.lockType != SecurePreferences.LockType.NONE &&
            BiometricHelper.isBiometricAvailable(this)
        ) {
            triggerBiometric()
        }
    }

    private fun triggerBiometric() {
        if (!BiometricHelper.isBiometricAvailable(this)) return

        BiometricHelper.authenticate(
            activity = this,
            title = "Verify Your Identity",
            subtitle = "Touch fingerprint sensor to unlock",
            onSuccess = {
                onUnlockSuccess()
            },
            onError = { _, _ ->
                // User cancelled or error - PIN/Pattern keypad remains active
            },
            onFailed = {
                // Wrong fingerprint - Stay on lock screen
            }
        )
    }

    private fun onUnlockSuccess() {
        AppLockAccessibilityService.unlockPackage(lockedPackage)
        AppMonitorService.notifyUnlocked(this, lockedPackage)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun goToHomeScreen() {
        AppLockAccessibilityService.lockDismissed()
        AppMonitorService.notifyDismissed(this)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
        overridePendingTransition(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goToHomeScreen()
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
    }
}
