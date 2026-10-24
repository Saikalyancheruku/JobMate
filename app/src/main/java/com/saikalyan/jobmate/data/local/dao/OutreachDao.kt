package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.OutreachEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutreachDao {
    @Query("SELECT * FROM outreach ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OutreachEntity>>

    @Query("SELECT * FROM outreach WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OutreachEntity?

    @Upsert
    suspend fun upsert(outreach: OutreachEntity)

    @Upsert
    suspend fun upsertAll(outreach: List<OutreachEntity>)

    @Transaction
    suspend fun saveDraftsAtomically(outreach: List<OutreachEntity>) = upsertAll(outreach)

    @Query("UPDATE outreach SET status = :status, sentAt = :sentAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, sentAt: java.time.Instant?, updatedAt: java.time.Instant): Int

    @Query("DELETE FROM outreach WHERE id = :id")
    suspend fun deleteById(id: String): Int
}


