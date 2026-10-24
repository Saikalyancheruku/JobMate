package com.saikalyan.jobmate.domain.repository

import kotlinx.coroutines.flow.Flow

interface JobContactLinkRepository {
    fun observeForContact(contactId: String): Flow<RepositoryResult<Set<String>>>
    suspend fun confirmLink(jobId: String, contactId: String, confidence: Double): RepositoryResult<Unit>
    suspend fun removeLink(jobId: String, contactId: String): RepositoryResult<Unit>
}
