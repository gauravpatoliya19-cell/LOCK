package com.applock.guard.ui.screens.home

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applock.guard.AppLockApplication
import com.applock.guard.data.model.GalleryMediaItem
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
    GALLERY,
    SECURITY,
    SETTINGS,
    DESIGN,
    ABOUT
}

enum class AppSubTab {
    UNLOCKED,
    LOCKED
}

enum class GalleryFilter {
    ALL,
    PHOTOS,
    VIDEOS,
    VAULT
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppLockApplication
    val repository = app.repository
    val galleryRepository = app.galleryRepository
    val securePreferences = app.securePreferences

    val lockedAppCount = repository.lockedAppCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val vaultMediaCount = galleryRepository.vaultCount
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

    private val _hasGalleryPermission = MutableStateFlow(false)
    val hasGalleryPermission: StateFlow<Boolean> = _hasGalleryPermission.asStateFlow()

    // Gallery state
    private val _galleryMedia = MutableStateFlow<List<GalleryMediaItem>>(emptyList())
    val galleryMedia: StateFlow<List<GalleryMediaItem>> = _galleryMedia.asStateFlow()

    private val _isLoadingGallery = MutableStateFlow(false)
    val isLoadingGallery: StateFlow<Boolean> = _isLoadingGallery.asStateFlow()

    private val _galleryFilter = MutableStateFlow(GalleryFilter.ALL)
    val galleryFilter: StateFlow<GalleryFilter> = _galleryFilter.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds: StateFlow<Set<Long>> = _selectedMediaIds.asStateFlow()

    private val _isPhoneGalleryLocked = MutableStateFlow(false)
    val isPhoneGalleryLocked: StateFlow<Boolean> = _isPhoneGalleryLocked.asStateFlow()

    private var allApps: List<InstalledApp> = emptyList()

    init {
        refreshServiceStatus()
        refreshPermissions()
        loadApps()
        refreshGalleryPermissions()
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        if (tab == MainTab.GALLERY && _hasGalleryPermission.value && _galleryMedia.value.isEmpty()) {
            loadGalleryMedia()
        }
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
            checkPhoneGalleryLockStatus()
        }
    }

    fun lockAll() {
        allApps = allApps.map { it.copy(isLocked = true) }
        applyFilter()
        viewModelScope.launch {
            repository.lockAllApps(allApps)
            checkPhoneGalleryLockStatus()
        }
    }

    fun unlockAll() {
        allApps = allApps.map { it.copy(isLocked = false) }
        applyFilter()
        viewModelScope.launch {
            repository.unlockAllApps()
            checkPhoneGalleryLockStatus()
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
        refreshGalleryPermissions()
    }

    fun refreshGalleryPermissions() {
        val ctx = getApplication<Application>()
        val hasPerm = PermissionHelper.hasGalleryPermission(ctx)
        _hasGalleryPermission.value = hasPerm
        if (hasPerm) {
            loadGalleryMedia()
            checkPhoneGalleryLockStatus()
        }
    }

    fun checkPhoneGalleryLockStatus() {
        viewModelScope.launch {
            _isPhoneGalleryLocked.value = galleryRepository.isPhoneGalleryLocked()
        }
    }

    fun togglePhoneGalleryAppLock(lock: Boolean) {
        viewModelScope.launch {
            galleryRepository.setPhoneGalleryLock(lock)
            _isPhoneGalleryLocked.value = lock
            // Also refresh app list
            allApps = repository.getInstalledApps()
            applyFilter()
        }
    }

    fun loadGalleryMedia() {
        viewModelScope.launch {
            _isLoadingGallery.value = true
            val media = galleryRepository.getAllGalleryMedia()
            _galleryMedia.value = media
            _isLoadingGallery.value = false
        }
    }

    fun setGalleryFilter(filter: GalleryFilter) {
        _galleryFilter.value = filter
    }

    fun toggleMediaVault(item: GalleryMediaItem) {
        viewModelScope.launch {
            galleryRepository.toggleVault(item)
            val updated = _galleryMedia.value.map {
                if (it.id == item.id) it.copy(isVaulted = !item.isVaulted) else it
            }
            _galleryMedia.value = updated
        }
    }

    fun toggleMediaSelection(id: Long) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedMediaIds.value = current
    }

    fun clearMediaSelection() {
        _selectedMediaIds.value = emptySet()
    }

    fun lockSelectedMedia() {
        viewModelScope.launch {
            val selected = _galleryMedia.value.filter { _selectedMediaIds.value.contains(it.id) }
            galleryRepository.lockSelected(selected)
            val updated = _galleryMedia.value.map {
                if (_selectedMediaIds.value.contains(it.id)) it.copy(isVaulted = true) else it
            }
            _galleryMedia.value = updated
            clearMediaSelection()
        }
    }

    fun unlockSelectedMedia() {
        viewModelScope.launch {
            val selected = _galleryMedia.value.filter { _selectedMediaIds.value.contains(it.id) }
            galleryRepository.unlockSelected(selected)
            val updated = _galleryMedia.value.map {
                if (_selectedMediaIds.value.contains(it.id)) it.copy(isVaulted = false) else it
            }
            _galleryMedia.value = updated
            clearMediaSelection()
        }
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
