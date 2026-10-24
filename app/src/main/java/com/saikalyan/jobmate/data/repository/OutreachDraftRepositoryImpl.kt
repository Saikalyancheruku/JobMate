package com.saikalyan.jobmate.data.repository

import com.saikalyan.jobmate.data.local.dao.OutreachDao
import com.saikalyan.jobmate.data.local.entity.OutreachEntity
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.model.OutreachDraft
import com.saikalyan.jobmate.domain.repository.OutreachDraftRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OutreachDraftRepositoryImpl @Inject constructor(
    private val dao: OutreachDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OutreachDraftRepository {
    override fun observeDrafts(): Flow<RepositoryResult<List<OutreachDraft>>> = dao.observeAll()
        .map { rows -> RepositoryResult.Success(rows.map(OutreachEntity::toDomain)) }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            emit(RepositoryResult.Failure(RepositoryError.Storage))
        }
        .flowOn(ioDispatcher)

    override suspend fun saveDrafts(drafts: List<OutreachDraft>): RepositoryResult<Unit> = try {
        if (drafts.isEmpty()) return RepositoryResult.Failure(RepositoryError.InvalidInput("drafts", "empty"))
        dao.saveDraftsAtomically(drafts.map { draft ->
            OutreachEntity(
                id = draft.id,
                jobId = draft.jobId,
                contactId = draft.contactId,
                templateId = draft.templateId,
                resumeId = draft.resumeId,
                recipientNameSnapshot = draft.recipientName,
                recipientEmailSnapshot = draft.recipientEmail,
                jobTitleSnapshot = draft.jobTitle,
                companyNameSnapshot = draft.companyName,
                templateNameSnapshot = draft.templateName,
                resumeDisplayNameSnapshot = draft.resumeDisplayName,
                renderedSubject = draft.subject,
                renderedBody = draft.body,
                status = "DRAFT",
                sentAt = null,
                createdAt = draft.createdAt,
                updatedAt = draft.updatedAt,
            )
        })
        RepositoryResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        RepositoryResult.Failure(RepositoryError.Storage)
    }

    private fun OutreachEntity.toDomain() = OutreachDraft(
        id = id,
        jobId = jobId,
        contactId = contactId.orEmpty(),
        templateId = templateId,
        resumeId = resumeId,
        recipientName = recipientNameSnapshot.orEmpty(),
        recipientEmail = recipientEmailSnapshot.orEmpty(),
        jobTitle = jobTitleSnapshot,
        companyName = companyNameSnapshot,
        templateName = templateNameSnapshot,
        resumeDisplayName = resumeDisplayNameSnapshot,
        subject = renderedSubject,
        body = renderedBody,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
