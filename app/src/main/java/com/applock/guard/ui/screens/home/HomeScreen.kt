package com.applock.guard.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.applock.guard.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToAppList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val lockedCount by viewModel.lockedAppCount.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatus()
    }

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
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                text = "AppLock Guard",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Keep your apps protected",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Shield Status Card
            ShieldStatusCard(
                isActive = isServiceRunning,
                lockedCount = lockedCount,
                onToggle = { viewModel.toggleService() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Lock,
                    value = lockedCount.toString(),
                    label = "Locked Apps",
                    color = AccentBlue
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shield,
                    value = if (isServiceRunning) "Active" else "Off",
                    label = "Protection",
                    color = if (isServiceRunning) SuccessGreen else ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActionCard(
                icon = Icons.Default.Apps,
                title = "Manage Apps",
                subtitle = "Select apps to lock or unlock",
                onClick = onNavigateToAppList
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionCard(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Change PIN, biometric, and more",
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun ShieldStatusCard(
    isActive: Boolean,
    lockedCount: Int,
    onToggle: () -> Unit
) {
    val bgBrush = if (isActive) {
        Brush.linearGradient(colors = listOf(AccentGradientStart.copy(alpha = 0.15f), AccentGradientEnd.copy(alpha = 0.15f)))
    } else {
        Brush.linearGradient(colors = listOf(ErrorRed.copy(alpha = 0.1f), ErrorRed.copy(alpha = 0.05f)))
    }
    val borderColor = if (isActive) AccentBlue.copy(alpha = 0.3f) else ErrorRed.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) AccentBlue.copy(alpha = 0.2f)
                        else ErrorRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isActive) AccentBlue else ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isActive) "Protection Active" else "Protection Off",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isActive) "$lockedCount apps are protected"
                    else "Your apps are unprotected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Toggle button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) SuccessGreen.copy(alpha = 0.2f)
                        else AccentBlue.copy(alpha = 0.2f)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isActive) "Stop" else "Start",
                    tint = if (isActive) SuccessGreen else AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, TextMuted.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

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
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
