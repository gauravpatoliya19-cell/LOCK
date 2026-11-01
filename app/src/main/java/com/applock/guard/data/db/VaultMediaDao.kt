package com.applock.guard.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMediaDao {

    @Query("SELECT * FROM vault_media ORDER BY lockedAt DESC")
    fun getAllVaultMedia(): Flow<List<VaultMediaEntity>>

    @Query("SELECT mediaUri FROM vault_media")
    suspend fun getAllVaultUris(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultMediaEntity)

    @Delete
    suspend fun delete(entity: VaultMediaEntity)

    @Query("DELETE FROM vault_media WHERE mediaUri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("SELECT COUNT(*) FROM vault_media")
    fun getVaultCount(): Flow<Int>
}
