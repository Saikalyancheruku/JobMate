package com.saikalyan.jobmate.data.repository

import com.saikalyan.jobmate.data.local.dao.JobContactDao
import com.saikalyan.jobmate.data.local.entity.JobContactEntity
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.repository.JobContactLinkRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class JobContactLinkRepositoryImpl @Inject constructor(
    private val dao: JobContactDao,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : JobContactLinkRepository {
    override fun observeForContact(contactId: String): Flow<RepositoryResult<Set<String>>> =
        dao.observeForContact(contactId).map { links ->
            val result: RepositoryResult<Set<String>> = RepositoryResult.Success(links.map { it.jobId }.toSet())
            result
        }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(RepositoryError.Storage))
            }.flowOn(ioDispatcher)

    override suspend fun confirmLink(jobId: String, contactId: String, confidence: Double): RepositoryResult<Unit> = try {
        dao.upsert(
            JobContactEntity(
                jobId = jobId,
                contactId = contactId,
                matchMethod = "USER_CONFIRMED",
                confidence = confidence.coerceIn(0.0, 1.0),
                isConfirmed = true,
                createdAt = clock.instant(),
            ),
        )
        RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        RepositoryResult.Failure(RepositoryError.Storage)
    }

    override suspend fun removeLink(jobId: String, contactId: String): RepositoryResult<Unit> = try {
        dao.delete(jobId, contactId)
        RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        RepositoryResult.Failure(RepositoryError.Storage)
    }
}
