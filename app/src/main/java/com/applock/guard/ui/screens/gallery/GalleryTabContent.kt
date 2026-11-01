package com.applock.guard.ui.screens.gallery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.applock.guard.data.model.GalleryMediaItem
import com.applock.guard.ui.screens.home.GalleryFilter
import com.applock.guard.ui.screens.home.HomeViewModel
import com.applock.guard.util.PermissionHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTabContent(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasGalleryPermission.collectAsState()
    val allMedia by viewModel.galleryMedia.collectAsState()
    val isLoading by viewModel.isLoadingGallery.collectAsState()
    val currentFilter by viewModel.galleryFilter.collectAsState()
    val selectedIds by viewModel.selectedMediaIds.collectAsState()
    val isPhoneGalleryLocked by viewModel.isPhoneGalleryLocked.collectAsState()

    var previewItem by remember { mutableStateOf<GalleryMediaItem?>(null) }

    // System Permission Launcher for Gallery (Android 14+, Android 13, and Android 12-)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshGalleryPermissions()
    }

    if (!hasPermission) {
        // Permission Request UI
        GalleryPermissionPrompt(
            onRequestPermission = {
                val perms = PermissionHelper.getGalleryPermissions()
                permissionLauncher.launch(perms)
            },
            onOpenSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
    } else {
        // Filter media according to selection
        val filteredMedia = remember(allMedia, currentFilter) {
            when (currentFilter) {
                GalleryFilter.ALL -> allMedia
                GalleryFilter.PHOTOS -> allMedia.filter { !it.isVideo }
                GalleryFilter.VIDEOS -> allMedia.filter { it.isVideo }
                GalleryFilter.VAULT -> allMedia.filter { it.isVaulted }
            }
        }

        val photosCount = remember(allMedia) { allMedia.count { !it.isVideo } }
        val videosCount = remember(allMedia) { allMedia.count { it.isVideo } }
        val vaultCount = remember(allMedia) { allMedia.count { it.isVaulted } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Phone Gallery App Lock Banner
            PhoneGalleryLockCard(
                isLocked = isPhoneGalleryLocked,
                onToggleLock = { viewModel.togglePhoneGalleryAppLock(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Gallery Stats Row
            GalleryStatsRow(
                photosCount = photosCount,
                videosCount = videosCount,
                vaultCount = vaultCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips (All, Photos, Videos, Vault)
            GalleryFilterBar(
                selectedFilter = currentFilter,
                onFilterSelected = { viewModel.setGalleryFilter(it) },
                vaultCount = vaultCount
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-Select Action Bar (if items selected)
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MultiSelectActionBar(
                    selectedCount = selectedIds.size,
                    onLockSelected = { viewModel.lockSelectedMedia() },
                    onUnlockSelected = { viewModel.unlockSelectedMedia() },
                    onClearSelection = { viewModel.clearMediaSelection() }
                )
            }

            // Media Grid or Empty State
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else if (filteredMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (currentFilter == GalleryFilter.VAULT) Icons.Default.Lock else Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentFilter == GalleryFilter.VAULT)
                                "Private Vault is Empty\nTap any photo/video or long-press to lock into Vault!"
                            else
                                "No media found in this category.",
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredMedia,
                        key = { it.id }
                    ) { item ->
                        val isSelected = selectedIds.contains(item.id)

                        GalleryGridItem(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = selectedIds.isNotEmpty(),
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleMediaSelection(item.id)
                                } else {
                                    previewItem = item
                                }
                            },
                            onLongClick = {
                                viewModel.toggleMediaSelection(item.id)
                            },
                            onToggleVault = {
                                viewModel.toggleMediaVault(item)
                            }
                        )
                    }
                }
            }
        }
    }

    // Full-Screen Preview Dialog
    previewItem?.let { item ->
        MediaPreviewDialog(
            item = item,
            onDismiss = { previewItem = null },
            onToggleVault = {
                viewModel.toggleMediaVault(item)
                previewItem = item.copy(isVaulted = !item.isVaulted)
            }
        )
    }
}

// -------------------------------------------------------------
// Permission Request Prompt
// -------------------------------------------------------------
@Composable
private fun GalleryPermissionPrompt(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Allow Gallery Access",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "To browse all photos & videos and protect private media inside AppLock Guard's encrypted vault, please allow media permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Allow Gallery Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = "Open App Settings",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Phone Gallery App Lock Card
// -------------------------------------------------------------
@Composable
private fun PhoneGalleryLockCard(
    isLocked: Boolean,
    onToggleLock: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(
                1.dp,
                if (isLocked) Color(0xFF0284C7).copy(alpha = 0.6f) else Color(0xFF334155),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isLocked) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFF334155)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = if (isLocked) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Lock Phone Gallery App",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                text = if (isLocked) "Google Photos & Gallery are 100% Protected" else "Secure Google Photos, Samsung/MI Gallery",
                fontSize = 11.sp,
                color = if (isLocked) Color(0xFF38BDF8) else Color(0xFF94A3B8)
            )
        }

        Switch(
            checked = isLocked,
            onCheckedChange = onToggleLock,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0284C7),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

// -------------------------------------------------------------
// Gallery Stats Row (Photos, Videos, Vault Count)
// -------------------------------------------------------------
@Composable
private fun GalleryStatsRow(
    photosCount: Int,
    videosCount: Int,
    vaultCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBadge(
            label = "Photos",
            count = photosCount,
            icon = Icons.Default.Image,
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "Videos",
            count = videosCount,
            icon = Icons.Default.Videocam,
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "Vault",
            count = vaultCount,
            icon = Icons.Default.Lock,
            modifier = Modifier.weight(1f),
            isVault = true
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isVault: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(
                1.dp,
                if (isVault && count > 0) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isVault) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// -------------------------------------------------------------
// Filter Bar (All, Photos, Videos, Vault)
// -------------------------------------------------------------
@Composable
private fun GalleryFilterBar(
    selectedFilter: GalleryFilter,
    onFilterSelected: (GalleryFilter) -> Unit,
    vaultCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(19.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterItemButton(
            title = "All",
            isSelected = selectedFilter == GalleryFilter.ALL,
            onClick = { onFilterSelected(GalleryFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        FilterItemButton(
            title = "Photos",
            isSelected = selectedFilter == GalleryFilter.PHOTOS,
            onClick = { onFilterSelected(GalleryFilter.PHOTOS) },
            modifier = Modifier.weight(1f)
        )
        FilterItemButton(
            title = "Videos",
            isSelected = selectedFilter == GalleryFilter.VIDEOS,
            onClick = { onFilterSelected(GalleryFilter.VIDEOS) },
            modifier = Modifier.weight(1f)
        )
        FilterItemButton(
            title = if (vaultCount > 0) "🔒 Vault ($vaultCount)" else "🔒 Vault",
            isSelected = selectedFilter == GalleryFilter.VAULT,
            onClick = { onFilterSelected(GalleryFilter.VAULT) },
            modifier = Modifier.weight(1.3f)
        )
    }
}

@Composable
private fun FilterItemButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8)
        )
    }
}

// -------------------------------------------------------------
// Multi-Select Action Bar
// -------------------------------------------------------------
@Composable
private fun MultiSelectActionBar(
    selectedCount: Int,
    onLockSelected: () -> Unit,
    onUnlockSelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0284C7).copy(alpha = 0.2f))
            .border(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClearSelection, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$selectedCount selected",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onLockSelected,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lock Vault", fontSize = 11.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = onUnlockSelected,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Unlock", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// Individual Grid Media Item
// -------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryGridItem(
    item: GalleryMediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleVault: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isSelected) 2.dp else if (item.isVaulted) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF38BDF8) else if (item.isVaulted) Color(0xFF0284C7) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Coil AsyncImage Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.contentUri)
                .crossfade(true)
                .size(300, 300)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dim overlay when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0284C7).copy(alpha = 0.4f))
            )
        }

        // Selection Checkmark (top-start)
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Vault Lock Badge (top-end)
        if (item.isVaulted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF38BDF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vaulted",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Video Duration / Indicator (bottom-start)
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                if (item.formattedDuration.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = item.formattedDuration,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Full-Screen Preview Dialog
// -------------------------------------------------------------
@Composable
private fun MediaPreviewDialog(
    item: GalleryMediaItem,
    onDismiss: () -> Unit,
    onToggleVault: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // High-res Image preview
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 60.dp)
            )

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${item.bucketName} • ${item.formattedSize}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                // Vault Toggle Button
                IconButton(
                    onClick = onToggleVault,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isVaulted) Color(0xFF0284C7) else Color.Black.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (item.isVaulted) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Toggle Vault",
                        tint = Color.White
                    )
                }
            }

            // Bottom Info & Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onToggleVault,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isVaulted) Color(0xFF334155) else Color(0xFF0284C7)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (item.isVaulted) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.isVaulted) "Remove from Private Vault" else "Move to Private Vault 🔒",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
