package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.SyncConflictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts WHERE resolution IS NULL ORDER BY detectedAt DESC")
    fun observeUnresolved(): Flow<List<SyncConflictEntity>>

    @Query("SELECT * FROM sync_conflicts WHERE resolution IS NULL ORDER BY detectedAt DESC")
    suspend fun getUnresolved(): List<SyncConflictEntity>

    @Query("SELECT * FROM sync_conflicts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SyncConflictEntity?

    @Upsert
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query("UPDATE sync_conflicts SET resolution = :resolution WHERE id = :id")
    suspend fun setResolution(id: String, resolution: String): Int
}
