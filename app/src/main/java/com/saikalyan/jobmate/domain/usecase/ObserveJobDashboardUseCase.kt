package com.saikalyan.jobmate.domain.usecase

import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveJobDashboardUseCase @Inject constructor(
    private val repository: JobRepository,
) {
    operator fun invoke(): Flow<RepositoryResult<JobDashboardSummary>> = repository.observeDashboard()
}
