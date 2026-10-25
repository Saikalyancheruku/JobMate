package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE (:status IS NULL OR status = :status) ORDER BY updatedAt DESC")
    fun observeJobs(status: String?): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    fun observeJob(id: String): Flow<JobEntity?>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): JobEntity?

    @Query("SELECT * FROM jobs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE fingerprint = :fingerprint ORDER BY updatedAt DESC LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): JobEntity?

    @Query("""
        SELECT * FROM jobs
        WHERE :query = ''
            OR title LIKE '%' || :query || '%'
            OR companyName LIKE '%' || :query || '%'
            OR location LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    suspend fun search(query: String): List<JobEntity>

    @Query("SELECT * FROM jobs ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<JobEntity>>

    @Query("SELECT status, COUNT(*) AS count FROM jobs GROUP BY status")
    fun observeStatusCounts(): Flow<List<JobStatusCountRow>>

    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM jobs WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Upsert
    suspend fun upsert(job: JobEntity)

    @Upsert
    suspend fun upsertAll(jobs: List<JobEntity>)

    @Query("UPDATE jobs SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: java.time.Instant): Int

    @Query("UPDATE jobs SET status = :status, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateStatuses(ids: List<String>, status: String, updatedAt: java.time.Instant): Int

    @Delete
    suspend fun delete(job: JobEntity): Int

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Transaction
    suspend fun saveAllAtomically(jobs: List<JobEntity>) {
        upsertAll(jobs)
    }
}

data class JobStatusCountRow(
    val status: String,
    val count: Int,
)
