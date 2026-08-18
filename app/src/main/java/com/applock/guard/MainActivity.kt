package com.applock.guard

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.service.AppMonitorService
import com.applock.guard.ui.screens.applist.AppListScreen
import com.applock.guard.ui.screens.home.HomeScreen
import com.applock.guard.ui.screens.settings.SettingsScreen
import com.applock.guard.ui.screens.setup.SetupScreen
import com.applock.guard.ui.theme.AppLockTheme
import com.applock.guard.util.BiometricHelper
import com.applock.guard.util.PermissionHelper

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppLockTheme {
                AppNavigation()
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val navController = rememberNavController()
        val repository = AppLockApplication.instance.repository

        val startDestination = if (repository.isSetupComplete) "home" else "setup"

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            // Setup Screen
            composable("setup") {
                val isBiometricAvailable = remember {
                    try {
                        BiometricHelper.isBiometricAvailable(this@MainActivity)
                    } catch (e: Exception) {
                        false
                    }
                }

                SetupScreen(
                    isBiometricAvailable = isBiometricAvailable,
                    onSetupComplete = { lockType, lockValue, biometricEnabled ->
                        // Save lock configuration
                        when (lockType) {
                            "pin" -> repository.setPin(lockValue)
                            "pattern" -> repository.setPattern(lockValue)
                        }
                        repository.setBiometricEnabled(biometricEnabled)

                        // Request necessary permissions
                        requestPermissions()

                        // Start the monitoring service
                        AppMonitorService.start(this@MainActivity)

                        // Navigate to home
                        navController.navigate("home") {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                )
            }

            // Home Screen
            composable("home") {
                // Check permissions on home screen
                var hasPermissions by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    hasPermissions = PermissionHelper.hasAllRequiredPermissions(this@MainActivity)
                }

                HomeScreen(
                    onNavigateToAppList = {
                        navController.navigate("applist")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // App List Screen
            composable("applist") {
                AppListScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings Screen
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onChangePin = {
                        // Navigate to setup screen for re-configuration
                        navController.navigate("setup") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    onResetAll = {
                        // Stop service and reset
                        AppMonitorService.stop(this@MainActivity)
                        AppLockApplication.instance.securePreferences.resetAll()

                        // Navigate back to setup
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    private fun requestPermissions() {
        if (!PermissionHelper.hasUsageStatsPermission(this)) {
            PermissionHelper.requestUsageStatsPermission(this)
        }
        if (!PermissionHelper.hasOverlayPermission(this)) {
            PermissionHelper.requestOverlayPermission(this)
        }
    }
}
