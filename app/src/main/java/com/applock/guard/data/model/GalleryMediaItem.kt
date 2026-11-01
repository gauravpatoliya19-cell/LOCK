package com.applock.guard.data.model

import android.net.Uri

data class GalleryMediaItem(
    val id: Long,
    val contentUri: Uri,
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val duration: Long = 0L,
    val bucketName: String = "Internal",
    val isVaulted: Boolean = false
) {
    val formattedDuration: String
        get() {
            if (!isVideo || duration <= 0) return ""
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val kb = size / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) {
                "%.1f MB".format(mb)
            } else {
                "%.0f KB".format(kb)
            }
        }
}
