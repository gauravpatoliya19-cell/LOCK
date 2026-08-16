package com.applock.guard.service

import android.os.Bundle
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

        lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE) ?: run {
            finish()
            return
        }

        val repository = AppLockApplication.instance.repository

        setContent {
            AppLockTheme {
                LockScreen(
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
                        // Go to home screen instead of the locked app
                        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                            addCategory(android.content.Intent.CATEGORY_HOME)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }

        // Auto-trigger biometric if enabled
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
            onSuccess = { onUnlockSuccess() },
            onError = { _, _ -> /* User cancelled or error — stay on lock screen */ },
            onFailed = { /* Wrong fingerprint — stay on lock screen */ }
        )
    }

    private fun onUnlockSuccess() {
        // Notify service that this app was unlocked
        AppMonitorService.notifyUnlocked(this, lockedPackage)
        finish()
    }

    @Deprecated("Use onBackPressed callback in Compose")
    override fun onBackPressed() {
        // Prevent going back to the locked app
        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
    }
}
