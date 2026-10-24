package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.Resume
import kotlinx.coroutines.flow.Flow

interface ResumeRepository {
    fun observeResumes(): Flow<RepositoryResult<List<Resume>>>
    suspend fun importFromUri(uri: String, displayName: String): RepositoryResult<Resume>
    suspend fun get(id: String): RepositoryResult<Resume?>
    suspend fun rename(id: String, displayName: String): RepositoryResult<Unit>
    suspend fun delete(id: String): RepositoryResult<Unit>
}
