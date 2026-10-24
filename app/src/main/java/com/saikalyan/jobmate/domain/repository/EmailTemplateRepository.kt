package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.EmailTemplate
import kotlinx.coroutines.flow.Flow

interface EmailTemplateRepository {
    fun observeTemplates(): Flow<RepositoryResult<List<EmailTemplate>>>
    suspend fun save(template: EmailTemplate): RepositoryResult<EmailTemplate>
    suspend fun delete(id: String): RepositoryResult<Unit>
}
