package com.applock.guard.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.applock.guard.data.db.LockedAppDao
import com.applock.guard.data.db.LockedAppEntity
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.util.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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

    // Fast in-memory cached set of locked package names for 0ms lock lookups
    private val lockedPackagesCache = ConcurrentHashMap.newKeySet<String>()

    init {
        // Initial sync of cached locked packages
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).kotlinx.coroutines.launch {
            try {
                val list = lockedAppDao.getActiveLockedAppsList()
                lockedPackagesCache.clear()
                lockedPackagesCache.addAll(list.map { it.packageName })
                Log.d("AppRepository", "Initialized locked cache with ${lockedPackagesCache.size} apps: $lockedPackagesCache")
            } catch (e: Exception) {
                Log.e("AppRepository", "Error initializing locked packages cache", e)
            }
        }
    }

    // ---------- Installed Apps (100% Compatible with Android 8 - 15) ----------

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val lockedList = lockedAppDao.getActiveLockedAppsList()
        val lockedPackages = lockedList.map { it.packageName }.toSet()
        lockedPackagesCache.clear()
        lockedPackagesCache.addAll(lockedPackages)

        val ownPackage = context.packageName
        val appList = mutableListOf<InstalledApp>()
        val seenPackages = mutableSetOf<String>()

        // 1. Primary Query: All Launchable apps via CATEGORY_LAUNCHER Intent
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != ownPackage && pkg != "$ownPackage.debug" && seenPackages.add(pkg)) {
                    val name = resolveInfo.loadLabel(pm).toString()
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    appList.add(
                        InstalledApp(
                            packageName = pkg,
                            appName = name,
                            isLocked = pkg in lockedPackages,
                            isSystemApp = isSystem
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error querying launcher intent apps", e)
        }

        // 2. Secondary Query: All Installed Applications with Launch Intent
        try {
            val installedApplications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (appInfo in installedApplications) {
                val pkg = appInfo.packageName
                if (pkg != ownPackage && pkg != "$ownPackage.debug" && seenPackages.add(pkg)) {
                    if (pm.getLaunchIntentForPackage(pkg) != null) {
                        val name = pm.getApplicationLabel(appInfo).toString()
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        appList.add(
                            InstalledApp(
                                packageName = pkg,
                                appName = name,
                                isLocked = pkg in lockedPackages,
                                isSystemApp = isSystem
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error querying installed applications", e)
        }

        // Sort: locked apps first, then alphabetical by app name
        appList.sortedWith(compareByDescending<InstalledApp> { it.isLocked }.thenBy { it.appName.lowercase() })
    }

    // ---------- Lock Management ----------

    val lockedApps: Flow<List<LockedAppEntity>> = lockedAppDao.getAllLockedApps()

    val lockedAppCount: Flow<Int> = lockedAppDao.getLockedAppCount()

    suspend fun toggleAppLock(packageName: String, appName: String, shouldLock: Boolean) = withContext(Dispatchers.IO) {
        if (shouldLock) {
            lockedPackagesCache.add(packageName)
            lockedAppDao.insert(LockedAppEntity(packageName, appName, true))
        } else {
            lockedPackagesCache.remove(packageName)
            lockedAppDao.deleteByPackage(packageName)
        }
        Log.d("AppRepository", "Toggled $packageName -> $shouldLock. Active cache: $lockedPackagesCache")
    }

    suspend fun lockAllApps(apps: List<InstalledApp>) = withContext(Dispatchers.IO) {
        for (app in apps) {
            lockedPackagesCache.add(app.packageName)
            lockedAppDao.insert(LockedAppEntity(app.packageName, app.appName, true))
        }
    }

    suspend fun unlockAllApps() = withContext(Dispatchers.IO) {
        lockedPackagesCache.clear()
        lockedAppDao.deleteAll()
    }

    suspend fun isAppLocked(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (lockedPackagesCache.contains(packageName)) return@withContext true
        val isLocked = lockedAppDao.isAppLocked(packageName)
        if (isLocked) lockedPackagesCache.add(packageName)
        return@withContext isLocked
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
