package com.applock.guard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.applock.guard.AppLockApplication

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, checking if service should start")

            val app = context.applicationContext as AppLockApplication
            if (app.securePreferences.isServiceEnabled && app.securePreferences.isSetupComplete) {
                Log.d(TAG, "Starting AppMonitorService after boot")
                AppMonitorService.start(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
