package com.applock.guard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey
    val mediaUri: String,
    val mediaName: String,
    val isVideo: Boolean,
    val lockedAt: Long = System.currentTimeMillis()
)
