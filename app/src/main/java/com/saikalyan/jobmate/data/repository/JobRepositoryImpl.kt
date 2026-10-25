package com.saikalyan.jobmate.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.saikalyan.jobmate.data.local.dao.JobDao
import com.saikalyan.jobmate.data.local.entity.JobEntity
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import com.saikalyan.jobmate.di.IoDispatcher

class JobRepositoryImpl @Inject constructor(
    private val jobDao: JobDao,
    private val syncQueue: SyncQueueManager,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : JobRepository {
    override fun observeJobs(status: JobStatus?): Flow<RepositoryResult<List<Job>>> =
        jobDao.observeJobs(status?.storageValue)
            .map(::mapJobs)
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(storageError(cause)))
            }
            .flowOn(ioDispatcher)

    override fun observeJob(id: String): Flow<RepositoryResult<Job?>> =
        jobDao.observeJob(id)
            .map { entity ->
                if (entity == null) {
                    RepositoryResult.Success(null)
                } else {
                    entity.toDomain()?.let { RepositoryResult.Success<Job?>(it) }
                        ?: RepositoryResult.Failure(RepositoryError.CorruptData("job.status"))
                }
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(storageError(cause)))
            }
            .flowOn(ioDispatcher)

    override fun observeDashboard(): Flow<RepositoryResult<JobDashboardSummary>> =
        combine(jobDao.observeStatusCounts(), jobDao.observeRecent(DASHBOARD_RECENT_LIMIT)) { counts, recent ->
            val statusCounts = mutableMapOf<JobStatus, Int>()
            for (row in counts) {
                val status = JobStatus.fromStorage(row.status)
                    ?: return@combine RepositoryResult.Failure(RepositoryError.CorruptData("job.status"))
                statusCounts[status] = row.count
            }
            val recentJobs = recent.mapJobsOrNull()
                ?: return@combine RepositoryResult.Failure(RepositoryError.CorruptData("job.status"))
            RepositoryResult.Success(JobDashboardSummary(statusCounts.toMap(), recentJobs))
        }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(storageError(cause)))
            }
            .flowOn(ioDispatcher)

    override suspend fun save(job: Job): RepositoryResult<Job> {
        validate(job)?.let { return RepositoryResult.Failure(it) }
        return databaseResult {
            jobDao.upsert(job.toEntity())
            syncQueue.enqueue("JOB", job.id)
            RepositoryResult.Success(job)
        }
    }

    override suspend fun updateStatus(id: String, status: JobStatus): RepositoryResult<Unit> =
        databaseResult {
            val changed = jobDao.updateStatus(id, status.storageValue, clock.instant())
            if (changed == 0) RepositoryResult.Failure(RepositoryError.NotFound)
            else { syncQueue.enqueue("JOB", id); RepositoryResult.Success(Unit) }
        }

    override suspend fun updateStatuses(ids: Set<String>, status: JobStatus): RepositoryResult<Unit> {
        val jobIds = ids.filter(String::isNotBlank).distinct()
        if (jobIds.isEmpty()) return RepositoryResult.Failure(RepositoryError.InvalidInput("jobIds", "empty"))
        return databaseResult {
            val changed = jobDao.updateStatuses(jobIds, status.storageValue, clock.instant())
            if (changed == 0) RepositoryResult.Failure(RepositoryError.NotFound)
            else {
                jobIds.forEach { syncQueue.enqueue("JOB", it, scheduleWork = false) }
                syncQueue.schedule()
                RepositoryResult.Success(Unit)
            }
        }
    }

    override suspend fun archive(id: String): RepositoryResult<Unit> =
        updateStatus(id, JobStatus.CLOSED)

    override suspend fun delete(id: String): RepositoryResult<Unit> =
        databaseResult {
            if (jobDao.deleteById(id) == 0) RepositoryResult.Failure(RepositoryError.NotFound)
            else { syncQueue.enqueue("JOB", id, SyncQueueManager.DELETE); RepositoryResult.Success(Unit) }
        }

    override suspend fun searchJobs(query: String): RepositoryResult<List<Job>> =
        databaseResult {
            mapJobs(jobDao.search(query.trim()))
        }

    override suspend fun findByFingerprint(fingerprint: String): RepositoryResult<Job?> {
        if (fingerprint.isBlank()) {
            return RepositoryResult.Failure(RepositoryError.InvalidInput("fingerprint", "blank"))
        }
        return databaseResult {
            val entity = jobDao.findByFingerprint(fingerprint)
            if (entity == null) RepositoryResult.Success(null)
            else entity.toDomain()?.let { RepositoryResult.Success<Job?>(it) }
                ?: RepositoryResult.Failure(RepositoryError.CorruptData("job.status"))
        }
    }

    private fun validate(job: Job): RepositoryError.InvalidInput? = when {
        job.id.isBlank() -> RepositoryError.InvalidInput("id", "blank")
        job.title.isBlank() -> RepositoryError.InvalidInput("title", "blank")
        job.companyName.isBlank() -> RepositoryError.InvalidInput("companyName", "blank")
        job.fingerprint.isBlank() -> RepositoryError.InvalidInput("fingerprint", "blank")
        job.experienceMin != null && job.experienceMin < 0 ->
            RepositoryError.InvalidInput("experienceMin", "negative")
        job.experienceMax != null && job.experienceMax < 0 ->
            RepositoryError.InvalidInput("experienceMax", "negative")
        job.experienceMin != null && job.experienceMax != null && job.experienceMin > job.experienceMax ->
            RepositoryError.InvalidInput("experience", "minimum_exceeds_maximum")
        else -> null
    }

    private suspend fun <T> databaseResult(
        operation: suspend () -> RepositoryResult<T>,
    ): RepositoryResult<T> = try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SQLiteConstraintException) {
        RepositoryResult.Failure(RepositoryError.Conflict)
    } catch (failure: Exception) {
        RepositoryResult.Failure(storageError(failure))
    }

    private fun storageError(cause: Throwable): RepositoryError =
        if (cause is SQLiteConstraintException) RepositoryError.Conflict else RepositoryError.Storage

    private fun mapJobs(entities: List<JobEntity>): RepositoryResult<List<Job>> {
        val jobs = entities.mapJobsOrNull()
            ?: return RepositoryResult.Failure(RepositoryError.CorruptData("job.status"))
        return RepositoryResult.Success(jobs)
    }

    private fun List<JobEntity>.mapJobsOrNull(): List<Job>? {
        val jobs = ArrayList<Job>(size)
        for (entity in this) jobs += entity.toDomain() ?: return null
        return jobs
    }

    private fun JobEntity.toDomain(): Job? {
        val jobStatus = JobStatus.fromStorage(status) ?: return null
        return Job(
            id = id,
            title = title,
            companyName = companyName,
            location = location,
            description = description,
            skills = skills,
            experienceMin = experienceMin,
            experienceMax = experienceMax,
            salaryText = salaryText,
            employmentType = employmentType,
            postedAt = postedAt,
            source = source,
            sourceUrl = sourceUrl,
            status = jobStatus,
            appliedAt = appliedAt,
            followUpAt = followUpAt,
            notes = notes,
            captureMethod = captureMethod,
            fingerprint = fingerprint,
            createdAt = createdAt,
            updatedAt = updatedAt,
            workArrangement = workArrangement,
            mandatorySkills = mandatorySkills,
            preferredSkills = preferredSkills,
            companyRating = companyRating,
            companyReviewCount = companyReviewCount,
            applicantCount = applicantCount,
            postedText = postedText,
            vacancyCount = vacancyCount,
            experienceLevel = experienceLevel,
            education = education,
            rawCapturedText = rawCapturedText,
            capturedAt = capturedAt,
            aiCorrected = aiCorrected,
        )
    }

    private fun Job.toEntity(): JobEntity = JobEntity(
        id = id,
        title = title.trim(),
        companyName = companyName.trim(),
        location = location,
        description = description,
        skills = skills,
        experienceMin = experienceMin,
        experienceMax = experienceMax,
        salaryText = salaryText,
        employmentType = employmentType,
        postedAt = postedAt,
        source = source,
        sourceUrl = sourceUrl,
        status = status.storageValue,
        appliedAt = appliedAt,
        followUpAt = followUpAt,
        notes = notes,
        captureMethod = captureMethod,
        fingerprint = fingerprint,
        createdAt = createdAt,
        updatedAt = updatedAt,
        workArrangement = workArrangement,
        mandatorySkills = mandatorySkills,
        preferredSkills = preferredSkills,
        companyRating = companyRating,
        companyReviewCount = companyReviewCount,
        applicantCount = applicantCount,
        postedText = postedText,
        vacancyCount = vacancyCount,
        experienceLevel = experienceLevel,
        education = education,
        rawCapturedText = rawCapturedText,
        capturedAt = capturedAt,
        aiCorrected = aiCorrected,
    )

    private companion object {
        const val DASHBOARD_RECENT_LIMIT = 10
    }
}
