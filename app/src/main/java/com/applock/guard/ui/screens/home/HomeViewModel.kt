package com.applock.guard.ui.screens.home

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applock.guard.AppLockApplication
import com.applock.guard.data.repository.InstalledApp
import com.applock.guard.service.AppLockAccessibilityService
import com.applock.guard.service.AppMonitorService
import com.applock.guard.util.BiometricHelper
import com.applock.guard.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    APPS,
    SECURITY,
    SETTINGS,
    DESIGN,
    ABOUT
}

enum class AppSubTab {
    UNLOCKED,
    LOCKED
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppLockApplication
    val repository = app.repository
    val securePreferences = app.securePreferences

    val lockedAppCount = repository.lockedAppCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedTab = MutableStateFlow(MainTab.APPS)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _appSubTab = MutableStateFlow(AppSubTab.UNLOCKED)
    val appSubTab: StateFlow<AppSubTab> = _appSubTab.asStateFlow()

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(true)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // Permissions state
    private val _hasOverlay = MutableStateFlow(false)
    val hasOverlay: StateFlow<Boolean> = _hasOverlay.asStateFlow()

    private val _hasUsage = MutableStateFlow(false)
    val hasUsage: StateFlow<Boolean> = _hasUsage.asStateFlow()

    private val _hasAccessibility = MutableStateFlow(false)
    val hasAccessibility: StateFlow<Boolean> = _hasAccessibility.asStateFlow()

    private var allApps: List<InstalledApp> = emptyList()

    init {
        refreshServiceStatus()
        refreshPermissions()
        loadApps()
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectAppSubTab(subTab: AppSubTab) {
        _appSubTab.value = subTab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            allApps = repository.getInstalledApps()
            applyFilter()
            _isLoadingApps.value = false
        }
    }

    fun toggleAppLock(packageName: String, appName: String, shouldLock: Boolean) {
        // 1. Instant local state update
        allApps = allApps.map {
            if (it.packageName == packageName) it.copy(isLocked = shouldLock) else it
        }
        applyFilter()

        // 2. Persist to Room Database
        viewModelScope.launch {
            repository.toggleAppLock(packageName, appName, shouldLock)
            refreshServiceStatus()
        }
    }

    fun lockAll() {
        allApps = allApps.map { it.copy(isLocked = true) }
        applyFilter()
        viewModelScope.launch {
            repository.lockAllApps(allApps)
        }
    }

    fun unlockAll() {
        allApps = allApps.map { it.copy(isLocked = false) }
        applyFilter()
        viewModelScope.launch {
            repository.unlockAllApps()
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            getApplication<Application>().packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _hasOverlay.value = PermissionHelper.hasOverlayPermission(ctx)
        _hasUsage.value = PermissionHelper.hasUsageStatsPermission(ctx)
        _hasAccessibility.value = PermissionHelper.isAccessibilityServiceEnabled(ctx)
    }

    fun refreshServiceStatus() {
        val ctx = getApplication<Application>()
        _isServiceRunning.value = AppMonitorService.isRunning(ctx) || AppLockAccessibilityService.isRunning()
    }

    fun toggleService() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            if (_isServiceRunning.value) {
                AppMonitorService.stop(ctx)
                securePreferences.isServiceEnabled = false
            } else {
                AppMonitorService.start(ctx)
                securePreferences.isServiceEnabled = true
            }
            kotlinx.coroutines.delay(400)
            refreshServiceStatus()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        _apps.value = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.lowercase().contains(query) ||
                it.packageName.lowercase().contains(query)
            }
        }
    }
}
