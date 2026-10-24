package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE state = :state ORDER BY queuedAt")
    fun observeByState(state: String): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SyncQueueEntity?

    @Query("SELECT * FROM sync_queue WHERE state = 'PENDING' ORDER BY queuedAt")
    suspend fun getPending(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE state = 'FAILED' ORDER BY queuedAt")
    suspend fun getFailed(): List<SyncQueueEntity>

    @Upsert
    suspend fun upsert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET state = :state, attemptCount = :attemptCount, lastErrorCode = :errorCode, remoteUpdatedAt = :remoteUpdatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, attemptCount: Int, errorCode: String?, remoteUpdatedAt: java.time.Instant?): Int

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
