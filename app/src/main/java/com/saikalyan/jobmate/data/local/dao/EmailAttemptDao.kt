package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.EmailAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailAttemptDao {
    @Query("SELECT * FROM email_attempts WHERE outreachId = :outreachId ORDER BY attemptNumber")
    fun observeForOutreach(outreachId: String): Flow<List<EmailAttemptEntity>>

    @Query("SELECT * FROM email_attempts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EmailAttemptEntity?

    @Query("SELECT COALESCE(MAX(attemptNumber), 0) + 1 FROM email_attempts WHERE outreachId = :outreachId")
    suspend fun nextAttemptNumber(outreachId: String): Int

    @Upsert
    suspend fun upsert(attempt: EmailAttemptEntity)
}
