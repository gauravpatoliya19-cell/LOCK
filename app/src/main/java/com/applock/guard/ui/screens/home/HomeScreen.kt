package com.applock.guard.ui.screens.home

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.applock.guard.data.repository.InstalledApp
import com.applock.guard.ui.theme.*
import com.applock.guard.util.PermissionHelper
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToSetup: () -> Unit = {}
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
        viewModel.refreshServiceStatus()
        viewModel.loadApps()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Title Header
            TopTitleHeader()

            // 5 Top Navigation Tabs (Apps, Security, Settings, Design, About)
            TopTabBar(
                selectedTab = selectedTab,
                onTabSelect = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    MainTab.APPS -> AppsTabContent(viewModel)
                    MainTab.SECURITY -> SecurityTabContent(viewModel, onNavigateToSetup)
                    MainTab.SETTINGS -> SettingsTabContent(viewModel)
                    MainTab.DESIGN -> DesignTabContent()
                    MainTab.ABOUT -> AboutTabContent()
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Top Header Title
// -------------------------------------------------------------
@Composable
private fun TopTitleHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Secure Your Apps",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color(0xFF38BDF8)
        )
    }
}

// -------------------------------------------------------------
// Top Navigation Tab Bar (Apps, Security, Settings, Design, About)
// -------------------------------------------------------------
@Composable
private fun TopTabBar(
    selectedTab: MainTab,
    onTabSelect: (MainTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabIconItem(
            label = "Apps",
            icon = Icons.AutoMirrored.Filled.List,
            isSelected = selectedTab == MainTab.APPS,
            onClick = { onTabSelect(MainTab.APPS) }
        )
        TabIconItem(
            label = "Security",
            icon = Icons.Default.Shield,
            isSelected = selectedTab == MainTab.SECURITY,
            onClick = { onTabSelect(MainTab.SECURITY) }
        )
        TabIconItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            isSelected = selectedTab == MainTab.SETTINGS,
            onClick = { onTabSelect(MainTab.SETTINGS) }
        )
        TabIconItem(
            label = "Design",
            icon = Icons.Default.Brush,
            isSelected = selectedTab == MainTab.DESIGN,
            onClick = { onTabSelect(MainTab.DESIGN) }
        )
        TabIconItem(
            label = "About",
            icon = Icons.Default.Info,
            isSelected = selectedTab == MainTab.ABOUT,
            onClick = { onTabSelect(MainTab.ABOUT) }
        )
    }
}

@Composable
private fun TabIconItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
        animationSpec = tween(200),
        label = "tab_tint"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = tintColor
        )
    }
}

// -------------------------------------------------------------
// 1. APPS TAB (Segmented UNLOCKED / LOCKED, Banner, Card List)
// -------------------------------------------------------------
@Composable
private fun AppsTabContent(viewModel: HomeViewModel) {
    val apps by viewModel.apps.collectAsState()
    val appSubTab by viewModel.appSubTab.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val unlockedApps = remember(apps) { apps.filter { !it.isLocked } }
    val lockedApps = remember(apps) { apps.filter { it.isLocked } }

    val displayedApps = if (appSubTab == AppSubTab.UNLOCKED) unlockedApps else lockedApps

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Segmented Pill Switcher: [ UNLOCKED (count) ] | [ LOCKED (count) ]
        SegmentedPillSwitcher(
            currentTab = appSubTab,
            unlockedCount = unlockedApps.size,
            lockedCount = lockedApps.size,
            onTabSelected = { viewModel.selectAppSubTab(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Cute Phone Padlock Illustration Banner
        BannerHeader(
            isUnlockedTab = appSubTab == AppSubTab.UNLOCKED,
            count = displayedApps.size,
            onLockAllToggle = {
                if (appSubTab == AppSubTab.UNLOCKED) {
                    viewModel.lockAll()
                } else {
                    viewModel.unlockAll()
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search apps…", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color(0xFF38BDF8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // App List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (displayedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (appSubTab == AppSubTab.UNLOCKED)
                        "All apps are locked & protected! 🛡️"
                    else
                        "No locked apps yet.\nSwitch to Unlocked tab and tap the lock icon!",
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = displayedApps,
                    key = { it.packageName }
                ) { app ->
                    ModernAppCardItem(
                        app = app,
                        appIcon = viewModel.getAppIcon(app.packageName),
                        onToggle = { shouldLock ->
                            viewModel.toggleAppLock(app.packageName, app.appName, shouldLock)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Segmented Pill Tab Switcher [ UNLOCKED ] | [ LOCKED ]
// -------------------------------------------------------------
@Composable
private fun SegmentedPillSwitcher(
    currentTab: AppSubTab,
    unlockedCount: Int,
    lockedCount: Int,
    onTabSelected: (AppSubTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // UNLOCKED Tab Button
        val isUnlocked = currentTab == AppSubTab.UNLOCKED
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isUnlocked) Color(0xFF0284C7) else Color.Transparent)
                .clickable { onTabSelected(AppSubTab.UNLOCKED) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UNLOCKED ($unlockedCount)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isUnlocked) Color.White else Color(0xFF94A3B8)
            )
        }

        // LOCKED Tab Button
        val isLocked = currentTab == AppSubTab.LOCKED
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isLocked) Color(0xFF0284C7) else Color.Transparent)
                .clickable { onTabSelected(AppSubTab.LOCKED) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LOCKED ($lockedCount)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isLocked) Color.White else Color(0xFF94A3B8)
            )
        }
    }
}

// -------------------------------------------------------------
// Graphic Banner Header (Unlocked/Locked status + illustration)
// -------------------------------------------------------------
@Composable
private fun BannerHeader(
    isUnlockedTab: Boolean,
    count: Int,
    onLockAllToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isUnlockedTab) "Unlocked Apps ($count)" else "Locked Apps ($count)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = if (isUnlockedTab) "Tap padlock to secure apps" else "Protected with Fingerprint & PIN",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }

        // Quick Action Button (Lock All / Unlock All)
        Button(
            onClick = onLockAllToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isUnlockedTab) Color(0xFF0284C7) else Color(0xFF334155)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isUnlockedTab) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isUnlockedTab) "Lock All" else "Unlock All",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// -------------------------------------------------------------
// Modern App Card Item with Padlock Button
// -------------------------------------------------------------
@Composable
private fun ModernAppCardItem(
    app: InstalledApp,
    appIcon: Drawable?,
    onToggle: (Boolean) -> Unit
) {
    val cardBg = if (app.isLocked) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.7f)
    val cardBorder = if (app.isLocked) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF334155)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onToggle(!app.isLocked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon
        if (appIcon != null) {
            Image(
                painter = rememberDrawablePainter(drawable = appIcon),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // App Name + Package
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                if (app.isSystemApp) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(system)",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Text(
                text = if (app.isLocked) "Protected with Fingerprint & PIN" else "Unlocked",
                style = MaterialTheme.typography.bodySmall,
                color = if (app.isLocked) Color(0xFF38BDF8) else Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Big Blue Padlock Icon Button
        IconButton(
            onClick = { onToggle(!app.isLocked) },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (app.isLocked) Color(0xFF0284C7).copy(alpha = 0.2f)
                    else Color(0xFF334155).copy(alpha = 0.5f)
                )
        ) {
            Icon(
                imageVector = if (app.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (app.isLocked) "Locked" else "Unlocked",
                tint = if (app.isLocked) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2. SECURITY TAB
// -------------------------------------------------------------
@Composable
private fun SecurityTabContent(
    viewModel: HomeViewModel,
    onNavigateToSetup: () -> Unit
) {
    var isBiometricEnabled by remember { mutableStateOf(viewModel.repository.isBiometricEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Authentication Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            SecurityActionCard(
                title = "Change PIN / Pattern",
                subtitle = "Update master passcode or pattern lock",
                icon = Icons.Default.Lock,
                onClick = onNavigateToSetup
            )
        }

        item {
            SecurityToggleCard(
                title = "Fingerprint / Biometrics",
                subtitle = "Unlock instantly using your fingerprint sensor",
                icon = Icons.Default.Fingerprint,
                checked = isBiometricEnabled,
                onCheckedChange = {
                    isBiometricEnabled = it
                    viewModel.setBiometricEnabled(it)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Protection Behavior",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            SecurityActionCard(
                title = "Re-lock Timeout",
                subtitle = "Locks immediately when screen turns off or app is closed",
                icon = Icons.Default.Timer,
                onClick = {}
            )
        }

        item {
            SecurityActionCard(
                title = "Anti-Uninstall Protection",
                subtitle = "Prevent unauthorized uninstallation of AppLock",
                icon = Icons.Default.Shield,
                onClick = {}
            )
        }
    }
}

// -------------------------------------------------------------
// 3. SETTINGS TAB (Permissions & Protection Engine)
// -------------------------------------------------------------
@Composable
private fun SettingsTabContent(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val hasOverlay by viewModel.hasOverlay.collectAsState()
    val hasUsage by viewModel.hasUsage.collectAsState()
    val hasAccessibility by viewModel.hasAccessibility.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Real-Time Protection Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF38BDF8)
            )
        }

        item {
            SecurityToggleCard(
                title = "Protection Shield",
                subtitle = if (isServiceRunning) "Active - monitoring app launches" else "Paused",
                icon = Icons.Default.Security,
                checked = isServiceRunning,
                onCheckedChange = { viewModel.toggleService() }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Android System Permissions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF38BDF8)
            )
        }

        item {
            PermissionCard(
                title = "Accessibility Service (0ms Lock)",
                subtitle = "Provides instant 0ms app detection",
                isGranted = hasAccessibility,
                onClick = { PermissionHelper.requestAccessibilityPermission(context) }
            )
        }

        item {
            PermissionCard(
                title = "Display Over Other Apps (Overlay)",
                subtitle = "Displays lock screen immediately over apps",
                isGranted = hasOverlay,
                onClick = { PermissionHelper.requestOverlayPermission(context) }
            )
        }

        item {
            PermissionCard(
                title = "Usage Access",
                subtitle = "Detects app launch events in real-time",
                isGranted = hasUsage,
                onClick = { PermissionHelper.requestUsageStatsPermission(context) }
            )
        }

        item {
            PermissionCard(
                title = "Ignore Battery Optimization",
                subtitle = "Keeps lock service running without being killed",
                isGranted = true,
                onClick = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
            )
        }
    }
}

// -------------------------------------------------------------
// 4. DESIGN TAB
// -------------------------------------------------------------
@Composable
private fun DesignTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose Lock Screen Theme",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF38BDF8)
        )

        ThemeCardItem("Cyber Blue (Default)", "Modern blue/cyan gradient overlay", Color(0xFF0284C7), true)
        ThemeCardItem("Midnight Obsidian", "Ultra dark AMOLED friendly", Color(0xFF0F172A), false)
        ThemeCardItem("Emerald Guard", "Vibrant security green", Color(0xFF10B981), false)
        ThemeCardItem("Royal Purple", "Elegant velvet gradient", Color(0xFF8B5CF6), false)
    }
}

// -------------------------------------------------------------
// 5. ABOUT TAB
// -------------------------------------------------------------
@Composable
private fun AboutTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AppLock: Fingerprint & PIN",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Version 1.0.0 (Release Build)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Security Highlights:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• 0ms Real-Time Accessibility Engine", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• Hardware Biometric Sensor Authentication", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• SHA-256 Encrypted Passcode Storage", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text("• Zero Data Collection & 100% Offline", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// Helper UI Cards
// -------------------------------------------------------------
@Composable
private fun SecurityActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
    }
}

@Composable
private fun SecurityToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0284C7),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, if (isGranted) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        Text(
            text = if (isGranted) "Granted" else "Enable",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGranted) Color(0xFF10B981) else Color(0xFFF59E0B)
        )
    }
}

@Composable
private fun ThemeCardItem(title: String, subtitle: String, color: Color, isSelected: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, if (isSelected) color else Color(0xFF334155), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
        if (isSelected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = color)
        }
    }
}
