package com.applock.guard.ui.screens.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.applock.guard.ui.components.PatternLock
import com.applock.guard.ui.components.PinInput
import com.applock.guard.ui.theme.*

enum class SetupStep {
    CHOOSE_TYPE, ENTER_LOCK, CONFIRM_LOCK, ENABLE_BIOMETRIC, COMPLETE
}

@Composable
fun SetupScreen(
    isBiometricAvailable: Boolean,
    onSetupComplete: (lockType: String, lockValue: String, biometricEnabled: Boolean) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedType by remember { mutableStateOf("") } // "pin" or "pattern"
    var firstEntry by remember { mutableStateOf("") }
    var confirmEntry by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var patternError by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val steps = listOf(
        SetupStep.CHOOSE_TYPE,
        SetupStep.ENTER_LOCK,
        SetupStep.CONFIRM_LOCK,
        if (isBiometricAvailable) SetupStep.ENABLE_BIOMETRIC else null,
        SetupStep.COMPLETE
    ).filterNotNull()

    val step = steps[currentStep]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceDark, PrimaryDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Step indicator
            StepIndicator(current = currentStep, total = steps.size)

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                },
                label = "setup_step"
            ) { targetStep ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (targetStep) {
                        SetupStep.CHOOSE_TYPE -> {
                            // Title
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Secure Your Apps",
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose your lock method",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(48.dp))

                            LockTypeCard(
                                icon = Icons.Default.Pin,
                                title = "PIN Code",
                                description = "Use a 4–6 digit PIN",
                                isSelected = selectedType == "pin",
                                onClick = { selectedType = "pin" }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LockTypeCard(
                                icon = Icons.Default.Grid3x3,
                                title = "Pattern",
                                description = "Draw a 3×3 pattern",
                                isSelected = selectedType == "pattern",
                                onClick = { selectedType = "pattern" }
                            )

                            Spacer(modifier = Modifier.height(40.dp))

                            GradientButton(
                                text = "Continue",
                                enabled = selectedType.isNotEmpty(),
                                onClick = { currentStep++ }
                            )
                        }

                        SetupStep.ENTER_LOCK -> {
                            Text(
                                text = if (selectedType == "pin") "Set Your PIN" else "Draw Your Pattern",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedType == "pin") "Enter a 4–6 digit PIN"
                                else "Connect at least 3 dots",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            if (selectedType == "pin") {
                                PinInput(
                                    pin = pin,
                                    maxLength = 6,
                                    onDigitClick = { digit ->
                                        if (pin.length < 6) {
                                            pin += digit
                                            pinError = false
                                            errorMessage = null
                                        }
                                        if (pin.length >= 4) {
                                            // Auto-proceed after typing a valid-length PIN
                                        }
                                    },
                                    onDeleteClick = {
                                        if (pin.isNotEmpty()) {
                                            pin = pin.dropLast(1)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                AnimatedVisibility(visible = pin.length >= 4) {
                                    GradientButton(
                                        text = "Continue",
                                        onClick = {
                                            firstEntry = pin
                                            pin = ""
                                            currentStep++
                                        }
                                    )
                                }
                            } else {
                                PatternLock(
                                    onPatternComplete = { dots ->
                                        firstEntry = dots.joinToString("-")
                                        currentStep++
                                    }
                                )
                            }
                        }

                        SetupStep.CONFIRM_LOCK -> {
                            Text(
                                text = if (selectedType == "pin") "Confirm Your PIN" else "Confirm Your Pattern",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedType == "pin") "Re-enter your PIN"
                                else "Redraw your pattern",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )

                            errorMessage?.let { msg ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ErrorRed
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            if (selectedType == "pin") {
                                PinInput(
                                    pin = pin,
                                    maxLength = 6,
                                    onDigitClick = { digit ->
                                        if (pin.length < 6) {
                                            pin += digit
                                            pinError = false
                                            errorMessage = null
                                        }
                                    },
                                    onDeleteClick = {
                                        if (pin.isNotEmpty()) {
                                            pin = pin.dropLast(1)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                AnimatedVisibility(visible = pin.length >= 4) {
                                    GradientButton(
                                        text = "Confirm",
                                        onClick = {
                                            if (pin == firstEntry) {
                                                confirmEntry = pin
                                                pin = ""
                                                currentStep++
                                            } else {
                                                errorMessage = "PINs don't match. Try again."
                                                pin = ""
                                                pinError = true
                                            }
                                        }
                                    )
                                }
                            } else {
                                PatternLock(
                                    isError = patternError,
                                    onPatternComplete = { dots ->
                                        val patternStr = dots.joinToString("-")
                                        if (patternStr == firstEntry) {
                                            confirmEntry = patternStr
                                            currentStep++
                                        } else {
                                            errorMessage = "Patterns don't match. Try again."
                                            patternError = true
                                        }
                                    }
                                )
                            }
                        }

                        SetupStep.ENABLE_BIOMETRIC -> {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Enable Fingerprint",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Quickly unlock apps with your fingerprint",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceCard)
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fingerprint Unlock",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Use your fingerprint as an alternative",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { biometricEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ToggleThumb,
                                        checkedTrackColor = ToggleOn,
                                        uncheckedThumbColor = ToggleThumb.copy(alpha = 0.7f),
                                        uncheckedTrackColor = ToggleOff
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(40.dp))

                            GradientButton(
                                text = "Continue",
                                onClick = { currentStep++ }
                            )
                        }

                        SetupStep.COMPLETE -> {
                            Spacer(modifier = Modifier.height(48.dp))
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(96.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "You're All Set!",
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your lock has been configured.\nNow choose which apps to protect.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            GradientButton(
                                text = "Get Started",
                                onClick = {
                                    onSetupComplete(selectedType, firstEntry, biometricEnabled)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .size(if (i == current) 24.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (i <= current) {
                            Brush.linearGradient(
                                colors = listOf(AccentGradientStart, AccentGradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(PinDotInactive, PinDotInactive)
                            )
                        }
                    )
            )
        }
    }
}

@Composable
private fun LockTypeCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AccentBlue.copy(alpha = 0.1f) else SurfaceCard
    val borderColor = if (isSelected) AccentBlue else TextMuted.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) AccentBlue.copy(alpha = 0.2f) else SurfaceLight
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentBlue,
            disabledContainerColor = AccentBlue.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
