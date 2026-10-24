package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.OutreachDraft
import kotlinx.coroutines.flow.Flow

interface OutreachDraftRepository {
    fun observeDrafts(): Flow<RepositoryResult<List<OutreachDraft>>>
    suspend fun saveDrafts(drafts: List<OutreachDraft>): RepositoryResult<Unit>
}
