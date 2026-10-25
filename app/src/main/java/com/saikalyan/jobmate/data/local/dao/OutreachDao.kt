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

    @Transaction
    suspend fun prepareFollowUp(sourceId: String, draft: OutreachEntity, updatedAt: java.time.Instant): Boolean {
        if (clearFollowUpDue(sourceId, updatedAt) != 1) return false
        upsert(draft)
        return true
    }

    @Query("UPDATE outreach SET status = :status, sentAt = :sentAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, sentAt: java.time.Instant?, updatedAt: java.time.Instant): Int

    @Query("UPDATE outreach SET status = :status, sentAt = :sentAt, nextFollowUpAt = :nextFollowUpAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSendState(id: String, status: String, sentAt: java.time.Instant?, nextFollowUpAt: java.time.Instant?, updatedAt: java.time.Instant): Int

    @Query("UPDATE outreach SET nextFollowUpAt = NULL, updatedAt = :updatedAt WHERE id = :id AND status = 'SENT' AND nextFollowUpAt <= :updatedAt")
    suspend fun clearFollowUpDue(id: String, updatedAt: java.time.Instant): Int

    @Query("DELETE FROM outreach WHERE id = :id")
    suspend fun deleteById(id: String): Int
}


