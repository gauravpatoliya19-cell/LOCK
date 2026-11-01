package com.applock.guard.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.applock.guard.data.db.VaultMediaDao
import com.applock.guard.data.db.VaultMediaEntity
import com.applock.guard.data.model.GalleryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val context: Context,
    private val vaultDao: VaultMediaDao,
    private val appRepository: AppRepository
) {

    val vaultCount: Flow<Int> = vaultDao.getVaultCount()

    // Common phone gallery package names across OEMs (Google, Samsung, Xiaomi, OnePlus, Oppo, Vivo, etc.)
    val commonGalleryPackages = listOf(
        "com.google.android.apps.photos",
        "com.sec.android.gallery3d",
        "com.miui.gallery",
        "com.android.gallery3d",
        "com.coloros.gallery3d",
        "com.oppo.gallery3d",
        "com.vivo.gallery",
        "com.oneplus.gallery",
        "com.huawei.photos",
        "com.motorola.gallery",
        "com.asus.ephoto"
    )

    /**
     * Reads all photos and videos from MediaStore and maps vault status.
     */
    suspend fun getAllGalleryMedia(): List<GalleryMediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<GalleryMediaItem>()
        val vaultedUris = try {
            vaultDao.getAllVaultUris().toHashSet()
        } catch (e: Exception) {
            hashSetOf<String>()
        }

        // 1. Fetch Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        try {
            val imageCursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            imageCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "IMG_$id"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val bucket = cursor.getString(bucketCol) ?: "Internal"

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val isVaulted = vaultedUris.contains(uri.toString())

                    mediaList.add(
                        GalleryMediaItem(
                            id = id,
                            contentUri = uri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            mimeType = mime,
                            isVideo = false,
                            duration = 0L,
                            bucketName = bucket,
                            isVaulted = isVaulted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fetch Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        try {
            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            videoCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "VID_$id"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val duration = cursor.getLong(durCol)
                    val bucket = cursor.getString(bucketCol) ?: "Videos"

                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val isVaulted = vaultedUris.contains(uri.toString())

                    mediaList.add(
                        GalleryMediaItem(
                            id = id,
                            contentUri = uri,
                            name = name,
                            size = size,
                            dateAdded = dateAdded,
                            mimeType = mime,
                            isVideo = true,
                            duration = duration,
                            bucketName = bucket,
                            isVaulted = isVaulted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sort all media by date added DESC
        mediaList.sortByDescending { it.dateAdded }
        mediaList
    }

    /**
     * Locks or unlocks a single media item in the private vault.
     */
    suspend fun toggleVault(item: GalleryMediaItem) = withContext(Dispatchers.IO) {
        val uriStr = item.contentUri.toString()
        if (item.isVaulted) {
            vaultDao.deleteByUri(uriStr)
        } else {
            vaultDao.insert(
                VaultMediaEntity(
                    mediaUri = uriStr,
                    mediaName = item.name,
                    isVideo = item.isVideo
                )
            )
        }
    }

    /**
     * Locks multiple selected media items in the vault.
     */
    suspend fun lockSelected(items: List<GalleryMediaItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            vaultDao.insert(
                VaultMediaEntity(
                    mediaUri = item.contentUri.toString(),
                    mediaName = item.name,
                    isVideo = item.isVideo
                )
            )
        }
    }

    /**
     * Unlocks multiple media items from the vault.
     */
    suspend fun unlockSelected(items: List<GalleryMediaItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            vaultDao.deleteByUri(item.contentUri.toString())
        }
    }

    /**
     * Finds installed Gallery applications on the phone.
     */
    fun getInstalledGalleryApps(): List<String> {
        val pm = context.packageManager
        return commonGalleryPackages.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Checks if any phone gallery app is currently locked.
     */
    suspend fun isPhoneGalleryLocked(): Boolean = withContext(Dispatchers.IO) {
        val installedGalleries = getInstalledGalleryApps()
        if (installedGalleries.isEmpty()) return@withContext false
        installedGalleries.any { appRepository.isAppLocked(it) }
    }

    /**
     * Locks or unlocks all phone gallery applications in AppLock.
     */
    suspend fun setPhoneGalleryLock(lock: Boolean) = withContext(Dispatchers.IO) {
        val installedGalleries = getInstalledGalleryApps()
        val pm = context.packageManager
        for (pkg in installedGalleries) {
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: Exception) {
                "Gallery"
            }
            appRepository.toggleAppLock(pkg, appName, lock)
        }
    }
}
