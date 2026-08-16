package com.applock.guard.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.applock.guard.AppLockApplication
import com.applock.guard.service.AppMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppLockApplication
    private val repository = app.repository

    val lockedAppCount = repository.lockedAppCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    init {
        refreshServiceStatus()
    }

    fun refreshServiceStatus() {
        _isServiceRunning.value = AppMonitorService.isRunning(getApplication())
    }

    fun toggleService() {
        viewModelScope.launch {
            if (_isServiceRunning.value) {
                AppMonitorService.stop(getApplication())
                app.securePreferences.isServiceEnabled = false
            } else {
                AppMonitorService.start(getApplication())
                app.securePreferences.isServiceEnabled = true
            }
            // Small delay to let the service start/stop
            kotlinx.coroutines.delay(500)
            refreshServiceStatus()
        }
    }
}
