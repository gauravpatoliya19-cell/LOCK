package com.applock.guard.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps ORDER BY appName ASC")
    fun getAllLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1 ORDER BY appName ASC")
    fun getActiveLockedApps(): Flow<List<LockedAppEntity>>

    @Query("SELECT * FROM locked_apps WHERE isLocked = 1")
    suspend fun getActiveLockedAppsList(): List<LockedAppEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM locked_apps WHERE packageName = :packageName AND isLocked = 1)")
    suspend fun isAppLocked(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: LockedAppEntity)

    @Delete
    suspend fun delete(app: LockedAppEntity)

    @Query("DELETE FROM locked_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE locked_apps SET isLocked = :isLocked WHERE packageName = :packageName")
    suspend fun updateLockStatus(packageName: String, isLocked: Boolean)

    @Query("DELETE FROM locked_apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM locked_apps WHERE isLocked = 1")
    fun getLockedAppCount(): Flow<Int>
}
