package com.saikalyan.jobmate.data.repository

import com.saikalyan.jobmate.data.local.dao.EmailAttemptDao
import com.saikalyan.jobmate.data.local.dao.OutreachDao
import com.saikalyan.jobmate.data.local.dao.ResumeDao
import com.saikalyan.jobmate.data.local.entity.EmailAttemptEntity
import com.saikalyan.jobmate.data.remote.GmailSmtpClient
import com.saikalyan.jobmate.data.remote.SmtpResult
import com.saikalyan.jobmate.data.secure.SecureCredentialStore
import com.saikalyan.jobmate.data.storage.ResumeStorage
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.model.OutreachSendResult
import com.saikalyan.jobmate.domain.repository.OutreachSendRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.util.UUID
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutreachSendRepositoryImpl @Inject constructor(
    private val outreachDao: OutreachDao,
    private val attemptDao: EmailAttemptDao,
    private val resumeDao: ResumeDao,
    private val resumeStorage: ResumeStorage,
    private val credentials: SecureCredentialStore,
    private val smtp: GmailSmtpClient,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OutreachSendRepository {
    private val mutex = Mutex()

    override suspend fun sendDraft(outreachId: String, userConfirmedUnknownRetry: Boolean): RepositoryResult<OutreachSendResult> =
        withContext(ioDispatcher) {
            mutex.withLock {
                try {
                    val outreach = outreachDao.findById(outreachId)
                        ?: return@withLock RepositoryResult.Failure(RepositoryError.NotFound)
                    if (outreach.status == "SENT") return@withLock RepositoryResult.Failure(RepositoryError.Conflict)
                    val attempts = attemptDao.observeForOutreach(outreachId).first()
                    if (attempts.any { it.outcome == "UNKNOWN" } && !userConfirmedUnknownRetry) {
                        return@withLock RepositoryResult.Success(OutreachSendResult.UnknownRetryNeedsConfirmation)
                    }
                    val credential = credentials.read()
                        ?: return@withLock RepositoryResult.Failure(RepositoryError.InvalidInput("credential", "not_configured"))
                    val to = outreach.recipientEmailSnapshot
                        ?: return@withLock RepositoryResult.Failure(RepositoryError.InvalidInput("recipient", "missing_snapshot"))
                    val attachment = outreach.resumeId?.let { resumeId ->
                        val resume = resumeDao.findById(resumeId) ?: return@withLock RepositoryResult.Failure(RepositoryError.NotFound)
                        resumeStorage.file(resume.relativePath) to resume.displayName
                    }

                    val number = attemptDao.nextAttemptNumber(outreachId)
                    val now = clock.instant()
                    val attempt = EmailAttemptEntity(
                        id = UUID.randomUUID().toString(),
                        outreachId = outreachId,
                        attemptNumber = number,
                        startedAt = now,
                        endedAt = null,
                        outcome = "UNKNOWN",
                        sanitizedErrorCode = null,
                        messageId = null,
                        userConfirmedRetry = userConfirmedUnknownRetry,
                    )
                    attemptDao.upsert(attempt)
                    outreachDao.updateStatus(outreachId, "UNKNOWN", null, now)
                    when (val result = smtp.sendOne(
                        credential = credential,
                        recipient = to,
                        subject = outreach.renderedSubject,
                        body = outreach.renderedBody,
                        attachment = attachment?.first,
                        attachmentName = attachment?.second,
                    )) {
                        SmtpResult.Sent -> {
                            val ended = clock.instant()
                            attemptDao.upsert(attempt.copy(endedAt = ended, outcome = "SENT"))
                            val nextDue = ended.atZone(ZoneId.systemDefault()).plusDays(2).toInstant()
                            outreachDao.updateSendState(outreachId, "SENT", ended, nextDue, ended)
                            RepositoryResult.Success(OutreachSendResult.Sent)
                        }
                        is SmtpResult.Failed -> {
                            val ended = clock.instant()
                            attemptDao.upsert(attempt.copy(endedAt = ended, outcome = "FAILED", sanitizedErrorCode = result.reason.name))
                            outreachDao.updateStatus(outreachId, "FAILED", null, ended)
                            RepositoryResult.Success(OutreachSendResult.Failed(result.reason.name))
                        }
                        is SmtpResult.Unknown -> {
                            val ended = clock.instant()
                            attemptDao.upsert(attempt.copy(endedAt = ended, outcome = "UNKNOWN", sanitizedErrorCode = result.reason.name))
                            outreachDao.updateStatus(outreachId, "UNKNOWN", null, ended)
                            RepositoryResult.Success(OutreachSendResult.Unknown(result.reason.name))
                        }
                        SmtpResult.Connected -> {
                            val ended = clock.instant()
                            attemptDao.upsert(attempt.copy(endedAt = ended, outcome = "FAILED", sanitizedErrorCode = "UNEXPECTED_RESULT"))
                            outreachDao.updateStatus(outreachId, "FAILED", null, ended)
                            RepositoryResult.Success(OutreachSendResult.Failed("UNEXPECTED_RESULT"))
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    RepositoryResult.Failure(RepositoryError.Storage)
                }
            }
        }
}
