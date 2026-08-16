package com.applock.guard.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applock.guard.AppLockApplication
import com.applock.guard.BuildConfig
import com.applock.guard.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangePin: () -> Unit = {},
    onResetAll: () -> Unit = {}
) {
    val prefs = AppLockApplication.instance.securePreferences
    var biometricEnabled by remember { mutableStateOf(prefs.isBiometricEnabled) }
    var autoStart by remember { mutableStateOf(prefs.isServiceEnabled) }
    var showResetDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Security Section
            SectionTitle("Security")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Change PIN / Pattern",
                subtitle = "Update your master lock",
                onClick = onChangePin,
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Fingerprint,
                title = "Fingerprint Authentication",
                subtitle = "Use fingerprint to unlock apps",
                trailing = {
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = {
                            biometricEnabled = it
                            prefs.isBiometricEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ToggleThumb,
                            checkedTrackColor = ToggleOn,
                            uncheckedThumbColor = ToggleThumb.copy(alpha = 0.7f),
                            uncheckedTrackColor = ToggleOff
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // General Section
            SectionTitle("General")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.PhoneAndroid,
                title = "Auto-start on Boot",
                subtitle = "Start protection after device restarts",
                trailing = {
                    Switch(
                        checked = autoStart,
                        onCheckedChange = {
                            autoStart = it
                            prefs.isServiceEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ToggleThumb,
                            checkedTrackColor = ToggleOn,
                            uncheckedThumbColor = ToggleThumb.copy(alpha = 0.7f),
                            uncheckedTrackColor = ToggleOff
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Default.Timer,
                title = "Re-lock Timeout",
                subtitle = "${prefs.relockTimeoutSeconds} seconds after unlocking",
                onClick = { /* Could open a dialog to change timeout */ },
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Danger Zone
            SectionTitle("Danger Zone")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Delete,
                title = "Reset All Settings",
                subtitle = "Remove PIN, pattern, and all locked apps",
                iconColor = ErrorRed,
                onClick = { showResetDialog = true },
                trailing = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // About
            SectionTitle("About")
            Spacer(modifier = Modifier.height(12.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = "AppLock Guard",
                subtitle = "Version ${BuildConfig.VERSION_NAME}"
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("Reset All Settings?", color = TextPrimary)
            },
            text = {
                Text(
                    "This will remove your PIN, pattern, fingerprint settings, and unlock all apps. This action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onResetAll()
                }) {
                    Text("Reset", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AccentBlue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color = AccentBlue,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, TextMuted.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}
