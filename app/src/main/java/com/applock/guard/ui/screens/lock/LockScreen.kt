package com.applock.guard.ui.screens.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.guard.data.preferences.SecurePreferences
import com.applock.guard.ui.components.PatternLock
import com.applock.guard.ui.components.PinInput
import com.applock.guard.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    title: String = "Verify Your Identity",
    subtitle: String? = null,
    lockType: SecurePreferences.LockType,
    isBiometricEnabled: Boolean,
    onPinSubmit: (String) -> Boolean,
    onPatternSubmit: (String) -> Boolean,
    onBiometricRequest: () -> Unit,
    onBackPressed: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var attemptsRemaining by remember { mutableIntStateOf(5) }
    var isLockedOut by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, PrimaryDark, SurfaceDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            // Lock / Fingerprint Glowing icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(AccentBlue.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBiometricEnabled) Icons.Default.Fingerprint else Icons.Default.Lock,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle ?: if (isBiometricEnabled) {
                    "Use fingerprint or ${if (lockType == SecurePreferences.LockType.PATTERN) "pattern" else "PIN"} to continue"
                } else {
                    when (lockType) {
                        SecurePreferences.LockType.PIN -> "Enter PIN code to continue"
                        SecurePreferences.LockType.PATTERN -> "Draw pattern to continue"
                        else -> "Authenticate to continue"
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            // Error message
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (isLockedOut) {
                Text(
                    text = "Too many failed attempts.\nTry again in $lockoutSeconds seconds.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ErrorRed,
                    textAlign = TextAlign.Center
                )
            } else {
                when (lockType) {
                    SecurePreferences.LockType.PIN -> {
                        PinInput(
                            pin = pin,
                            maxLength = 6,
                            showBiometric = isBiometricEnabled,
                            onDigitClick = { digit ->
                                if (pin.length < 6) {
                                    pin += digit
                                    isError = false
                                    errorMessage = null
                                }

                                if (pin.length >= 4) {
                                    scope.launch {
                                        delay(150)
                                        val currentPin = pin
                                        if (currentPin.length >= 4) {
                                            val success = onPinSubmit(currentPin)
                                            if (!success) {
                                                handleFailedAttempt(
                                                    attemptsRemaining = attemptsRemaining,
                                                    onUpdate = { remaining, locked, seconds, msg ->
                                                        attemptsRemaining = remaining
                                                        isLockedOut = locked
                                                        lockoutSeconds = seconds
                                                        errorMessage = msg
                                                        isError = true
                                                    },
                                                    onLockout = {
                                                        scope.launch {
                                                            for (i in 30 downTo 0) {
                                                                lockoutSeconds = i
                                                                delay(1000)
                                                            }
                                                            isLockedOut = false
                                                            attemptsRemaining = 5
                                                        }
                                                    }
                                                )
                                                pin = ""
                                            }
                                        }
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                            },
                            onBiometricClick = onBiometricRequest
                        )
                    }

                    SecurePreferences.LockType.PATTERN -> {
                        PatternLock(
                            isError = isError,
                            onPatternComplete = { dots ->
                                val patternStr = dots.joinToString("-")
                                val success = onPatternSubmit(patternStr)
                                if (!success) {
                                    handleFailedAttempt(
                                        attemptsRemaining = attemptsRemaining,
                                        onUpdate = { remaining, locked, seconds, msg ->
                                            attemptsRemaining = remaining
                                            isLockedOut = locked
                                            lockoutSeconds = seconds
                                            errorMessage = msg
                                            isError = true
                                        },
                                        onLockout = {
                                            scope.launch {
                                                for (i in 30 downTo 0) {
                                                    lockoutSeconds = i
                                                    delay(1000)
                                                }
                                                isLockedOut = false
                                                attemptsRemaining = 5
                                            }
                                        }
                                    )
                                }
                            }
                        )

                        if (isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(20.dp))
                            TextButton(onClick = onBiometricRequest) {
                                Text(
                                    text = "Touch Fingerprint Sensor",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    else -> { /* None */ }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Exit / Go to Home Button
            TextButton(onClick = onBackPressed) {
                Text(
                    text = "Exit / Go to Home Screen",
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun handleFailedAttempt(
    attemptsRemaining: Int,
    onUpdate: (remaining: Int, lockedOut: Boolean, lockoutSeconds: Int, message: String) -> Unit,
    onLockout: () -> Unit
) {
    val remaining = attemptsRemaining - 1
    if (remaining <= 0) {
        onUpdate(0, true, 30, "Too many failed attempts! Cooldown active.")
        onLockout()
    } else {
        onUpdate(remaining, false, 0, "Wrong password! $remaining attempts remaining.")
    }
}
