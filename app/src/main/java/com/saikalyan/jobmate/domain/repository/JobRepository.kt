package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow

/** Domain contract for observing and managing locally persisted jobs. */
interface JobRepository {
    fun observeJobs(status: JobStatus? = null): Flow<RepositoryResult<List<Job>>>

    /** Emits Success(null) when the requested job is not present. */
    fun observeJob(id: String): Flow<RepositoryResult<Job?>>

    fun observeDashboard(): Flow<RepositoryResult<JobDashboardSummary>>

    suspend fun save(job: Job): RepositoryResult<Job>

    suspend fun updateStatus(id: String, status: JobStatus): RepositoryResult<Unit>

    /** Archiving maps to the persisted [JobStatus.CLOSED] state. */
    suspend fun archive(id: String): RepositoryResult<Unit>

    suspend fun delete(id: String): RepositoryResult<Unit>

    suspend fun searchJobs(query: String): RepositoryResult<List<Job>>

    suspend fun findByFingerprint(fingerprint: String): RepositoryResult<Job?>
}