package com.applock.guard.ui.screens.applist

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applock.guard.AppLockApplication
import com.applock.guard.data.repository.InstalledApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AppLockApplication).repository

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allApps: List<InstalledApp> = emptyList()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            allApps = repository.getInstalledApps()
            applyFilter()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun toggleAppLock(packageName: String, appName: String, shouldLock: Boolean) {
        viewModelScope.launch {
            repository.toggleAppLock(packageName, appName, shouldLock)
            // Update local list
            allApps = allApps.map {
                if (it.packageName == packageName) it.copy(isLocked = shouldLock) else it
            }
            applyFilter()
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            getApplication<Application>().packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
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
