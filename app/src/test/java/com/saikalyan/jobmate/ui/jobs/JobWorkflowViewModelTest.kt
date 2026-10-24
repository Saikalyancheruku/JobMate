package com.saikalyan.jobmate.ui.jobs

import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class JobWorkflowViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeJobRepository()

    @Before fun setUp() = kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    @After fun tearDown() = kotlinx.coroutines.Dispatchers.resetMain()

    @Test
    fun searchIsDebouncedAndMatchesTitleCompanyOrLocation() = runTest(dispatcher) {
        repository.jobs = listOf(
            job("1", "Android Engineer", "Northwind", "Pune"),
            job("2", "Backend Engineer", "Contoso", "Remote"),
        )
        val viewModel = JobListViewModel(repository)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(2, viewModel.uiState.value.jobs.size)

        viewModel.setSearchQuery("remote")
        advanceTimeBy(299)
        runCurrent()
        assertEquals(2, viewModel.uiState.value.jobs.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("2"), viewModel.uiState.value.jobs.map(Job::id))
    }

    @Test
    fun editorRejectsInvertedExperienceRange() {
        val viewModel = JobEditorViewModel(repository, Clock.systemUTC())
        viewModel.updateForm(JobEditorForm(title = "Engineer", companyName = "Northwind", experienceMin = "5", experienceMax = "2"))
        viewModel.save()
        assertEquals("Experience range is invalid.", viewModel.uiState.value.validationMessage)
        assertEquals(false, repository.saveCalled)
    }

    private fun job(id: String, title: String, company: String, location: String) = Job(
        id = id,
        title = title,
        companyName = company,
        location = location,
        fingerprint = "manual:$id",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private class FakeJobRepository : JobRepository {
        var jobs: List<Job> = emptyList()
        var saveCalled = false
        override fun observeJobs(status: JobStatus?): Flow<RepositoryResult<List<Job>>> = flowOf(
            RepositoryResult.Success(jobs.filter { status == null || it.status == status }),
        )
        override fun observeJob(id: String): Flow<RepositoryResult<Job?>> = flowOf(RepositoryResult.Success(jobs.find { it.id == id }))
        override fun observeDashboard(): Flow<RepositoryResult<JobDashboardSummary>> = flowOf(RepositoryResult.Success(JobDashboardSummary()))
        override suspend fun save(job: Job): RepositoryResult<Job> {
            saveCalled = true
            return RepositoryResult.Success(job)
        }
        override suspend fun updateStatus(id: String, status: JobStatus) = RepositoryResult.Success(Unit)
        override suspend fun archive(id: String) = RepositoryResult.Success(Unit)
        override suspend fun delete(id: String) = RepositoryResult.Success(Unit)
        override suspend fun searchJobs(query: String): RepositoryResult<List<Job>> = RepositoryResult.Success(jobs)
        override suspend fun findByFingerprint(fingerprint: String): RepositoryResult<Job?> = RepositoryResult.Success(null)
    }
}
