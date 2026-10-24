package com.saikalyan.jobmate.ui.dashboard

import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.usecase.ObserveJobDashboardUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeJobRepository()

    @Before
    fun setUp() = kotlinx.coroutines.Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = kotlinx.coroutines.Dispatchers.resetMain()

    @Test
    fun roomSummaryIsExposedAndEmptyDatabaseRemainsEmpty() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(ObserveJobDashboardUseCase(repository))
        advanceUntilIdle()
        repository.results.emit(RepositoryResult.Success(JobDashboardSummary()))
        advanceUntilIdle()

        assertEquals(DashboardLoadState.Content(JobDashboardSummary()), viewModel.uiState.value)
    }

    @Test
    fun repositoryFailureIsPresentedAsAnErrorState() = runTest(dispatcher) {
        val viewModel = DashboardViewModel(ObserveJobDashboardUseCase(repository))
        advanceUntilIdle()
        repository.results.emit(RepositoryResult.Failure(RepositoryError.Storage))
        advanceUntilIdle()

        assertEquals(
            DashboardLoadState.Error("Could not load your saved jobs. Try again."),
            viewModel.uiState.value,
        )
    }

    private class FakeJobRepository : JobRepository {
        val results = MutableSharedFlow<RepositoryResult<JobDashboardSummary>>(extraBufferCapacity = 1)
        override fun observeDashboard() = results
        override fun observeJobs(status: com.saikalyan.jobmate.domain.model.JobStatus?) = error("unused")
        override fun observeJob(id: String) = error("unused")
        override suspend fun save(job: com.saikalyan.jobmate.domain.model.Job) = error("unused")
        override suspend fun updateStatus(id: String, status: com.saikalyan.jobmate.domain.model.JobStatus) = error("unused")
        override suspend fun archive(id: String) = error("unused")
        override suspend fun delete(id: String) = error("unused")
        override suspend fun searchJobs(query: String) = error("unused")
        override suspend fun findByFingerprint(fingerprint: String) = error("unused")
    }
}
