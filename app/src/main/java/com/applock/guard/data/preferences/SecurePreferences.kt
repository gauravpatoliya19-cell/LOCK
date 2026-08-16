package com.applock.guard.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ---------- PIN / Pattern ----------

    var hashedPin: String?
        get() = prefs.getString(KEY_HASHED_PIN, null)
        set(value) = prefs.edit().putString(KEY_HASHED_PIN, value).apply()

    var hashedPattern: String?
        get() = prefs.getString(KEY_HASHED_PATTERN, null)
        set(value) = prefs.edit().putString(KEY_HASHED_PATTERN, value).apply()

    // ---------- Lock Type ----------

    var lockType: LockType
        get() {
            val ordinal = prefs.getInt(KEY_LOCK_TYPE, LockType.NONE.ordinal)
            return LockType.entries.getOrElse(ordinal) { LockType.NONE }
        }
        set(value) = prefs.edit().putInt(KEY_LOCK_TYPE, value.ordinal).apply()

    // ---------- Biometric ----------

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    // ---------- Setup ----------

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    // ---------- Service ----------

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    // ---------- Re-lock Timeout ----------

    var relockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_RELOCK_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_RELOCK_TIMEOUT, value).apply()

    // ---------- Reset ----------

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    enum class LockType {
        NONE, PIN, PATTERN
    }

    companion object {
        private const val PREFS_FILE_NAME = "app_lock_secure_prefs"
        private const val KEY_HASHED_PIN = "hashed_pin"
        private const val KEY_HASHED_PATTERN = "hashed_pattern"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_RELOCK_TIMEOUT = "relock_timeout"
    }
}
