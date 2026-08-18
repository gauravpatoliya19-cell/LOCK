package com.applock.guard

import android.os.Bundle
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
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.service.AppMonitorService
import com.applock.guard.ui.screens.applist.AppListScreen
import com.applock.guard.ui.screens.home.HomeScreen
import com.applock.guard.ui.screens.lock.LockScreen
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

        // If setup is complete, first verify identity (fingerprint/password) before opening dashboard
        val startDestination = if (repository.isSetupComplete) "auth_gate" else "setup"

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
            // Verify Your Identity Gatekeeper
            composable("auth_gate") {
                LaunchedEffect(Unit) {
                    if (repository.isBiometricEnabled && BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                        BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = "Verify Your Identity",
                            subtitle = "Touch fingerprint sensor or use password",
                            onSuccess = {
                                navController.navigate("home") {
                                    popUpTo("auth_gate") { inclusive = true }
                                }
                            },
                            onError = { _, _ -> },
                            onFailed = {}
                        )
                    }
                }

                LockScreen(
                    title = "Verify Your Identity",
                    subtitle = "Use fingerprint or password to open App Lock",
                    lockType = repository.lockType,
                    isBiometricEnabled = repository.isBiometricEnabled,
                    onPinSubmit = { pin ->
                        if (repository.verifyPin(pin)) {
                            navController.navigate("home") {
                                popUpTo("auth_gate") { inclusive = true }
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onPatternSubmit = { pattern ->
                        if (repository.verifyPattern(pattern)) {
                            navController.navigate("home") {
                                popUpTo("auth_gate") { inclusive = true }
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onBiometricRequest = {
                        BiometricHelper.authenticate(
                            activity = this@MainActivity,
                            title = "Verify Your Identity",
                            subtitle = "Touch fingerprint sensor to unlock",
                            onSuccess = {
                                navController.navigate("home") {
                                    popUpTo("auth_gate") { inclusive = true }
                                }
                            },
                            onError = { _, _ -> },
                            onFailed = {}
                        )
                    },
                    onBackPressed = {
                        finish()
                    }
                )
            }

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

            // Home Screen Dashboard (Unified with Apps, Security, Settings, Design, About)
            composable("home") {
                HomeScreen(
                    onNavigateToSetup = {
                        navController.navigate("setup") {
                            popUpTo("home") { inclusive = false }
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
