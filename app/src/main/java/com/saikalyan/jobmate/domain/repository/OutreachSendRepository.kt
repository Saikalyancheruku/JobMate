package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.OutreachSendResult

interface OutreachSendRepository {
    suspend fun sendDraft(outreachId: String, userConfirmedUnknownRetry: Boolean = false): RepositoryResult<OutreachSendResult>
}
