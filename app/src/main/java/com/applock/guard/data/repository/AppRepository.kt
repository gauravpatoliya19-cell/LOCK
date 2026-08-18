package com.applock.guard.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.applock.guard.data.db.LockedAppDao
import com.applock.guard.data.db.LockedAppEntity
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.util.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isLocked: Boolean,
    val isSystemApp: Boolean
)

class AppRepository(
    private val lockedAppDao: LockedAppDao,
    val securePreferences: SecurePreferences,
    private val context: Context
) {

    // ---------- Installed Apps ----------

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val lockedPackages = lockedAppDao.getActiveLockedAppsList()
            .map { it.packageName }
            .toSet()

        val ownPackage = context.packageName

        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // Filter out our own app and show all user-launchable apps
                appInfo.packageName != ownPackage &&
                appInfo.packageName != "$ownPackage.debug" &&
                pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    isLocked = appInfo.packageName in lockedPackages,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedWith(compareByDescending<InstalledApp> { it.isLocked }.thenBy { it.appName })
    }

    // ---------- Lock Management ----------

    val lockedApps: Flow<List<LockedAppEntity>> = lockedAppDao.getAllLockedApps()

    val lockedAppCount: Flow<Int> = lockedAppDao.getLockedAppCount()

    suspend fun toggleAppLock(packageName: String, appName: String, shouldLock: Boolean) = withContext(Dispatchers.IO) {
        if (shouldLock) {
            lockedAppDao.insert(LockedAppEntity(packageName, appName, true))
        } else {
            lockedAppDao.deleteByPackage(packageName)
        }
    }

    suspend fun isAppLocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext lockedAppDao.isAppLocked(packageName)
    }

    suspend fun unlockAll() = withContext(Dispatchers.IO) {
        lockedAppDao.deleteAll()
    }

    // ---------- PIN / Pattern Verification ----------

    val isSetupComplete: Boolean
        get() = securePreferences.isSetupComplete

    val lockType: SecurePreferences.LockType
        get() = securePreferences.lockType

    val isBiometricEnabled: Boolean
        get() = securePreferences.isBiometricEnabled

    fun setPin(pin: String) {
        securePreferences.hashedPin = CryptoHelper.hashSha256(pin)
        securePreferences.lockType = SecurePreferences.LockType.PIN
        securePreferences.isSetupComplete = true
    }

    fun setPattern(pattern: String) {
        securePreferences.hashedPattern = CryptoHelper.hashSha256(pattern)
        securePreferences.lockType = SecurePreferences.LockType.PATTERN
        securePreferences.isSetupComplete = true
    }

    fun verifyPin(pin: String): Boolean {
        val hashedInput = CryptoHelper.hashSha256(pin)
        return hashedInput == securePreferences.hashedPin
    }

    fun verifyPattern(pattern: String): Boolean {
        val hashedInput = CryptoHelper.hashSha256(pattern)
        return hashedInput == securePreferences.hashedPattern
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securePreferences.isBiometricEnabled = enabled
    }
}
