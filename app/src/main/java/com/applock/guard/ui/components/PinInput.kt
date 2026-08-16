package com.applock.guard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applock.guard.ui.theme.*

@Composable
fun PinInput(
    pin: String,
    maxLength: Int = 6,
    showBiometric: Boolean = false,
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN dots
        PinDots(currentLength = pin.length, maxLength = maxLength)

        Spacer(modifier = Modifier.height(40.dp))

        // Numeric keypad
        NumericKeypad(
            showBiometric = showBiometric,
            onDigitClick = onDigitClick,
            onDeleteClick = onDeleteClick,
            onBiometricClick = onBiometricClick
        )
    }
}

@Composable
private fun PinDots(
    currentLength: Int,
    maxLength: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxLength) {
            val isActive = i < currentLength
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
                label = "dot_scale"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (isActive) {
                            Brush.linearGradient(
                                colors = listOf(AccentGradientStart, AccentGradientEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(PinDotInactive, PinDotInactive)
                            )
                        }
                    )
                    .then(
                        if (!isActive) Modifier.border(1.dp, TextMuted, CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
private fun NumericKeypad(
    showBiometric: Boolean,
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('B', '0', 'D') // B = Biometric, D = Delete
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        'B' -> {
                            if (showBiometric) {
                                KeypadButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBiometricClick()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Fingerprint",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(72.dp))
                            }
                        }
                        'D' -> {
                            KeypadButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDeleteClick()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDigitClick(key)
                                }
                            ) {
                                Text(
                                    text = key.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 26.sp
                                    ),
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(SurfaceCard.copy(alpha = 0.6f))
            .border(1.dp, TextMuted.copy(alpha = 0.2f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
