package com.saikalyan.jobmate.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobListUiState(
    val jobs: List<Job> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: JobStatus? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val captureFilter: CaptureFilter = CaptureFilter.ALL,
)

enum class CaptureFilter { ALL, SCRAPED, CORRECTED }

@HiltViewModel
class JobListViewModel @Inject constructor(
    private val repository: JobRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val status = MutableStateFlow<JobStatus?>(null)
    private val refresh = MutableStateFlow(0)
    private val captureFilter = MutableStateFlow(CaptureFilter.ALL)
    private val _uiState = MutableStateFlow(JobListUiState())
    val uiState: StateFlow<JobListUiState> = _uiState.asStateFlow()

    init { observeJobs() }

    fun setSearchQuery(value: String) {
        query.value = value
        _uiState.value = _uiState.value.copy(searchQuery = value)
    }

    fun setStatusFilter(value: JobStatus?) {
        status.value = value
        _uiState.value = _uiState.value.copy(statusFilter = value)
    }

    fun setCaptureFilter(value: CaptureFilter) {
        captureFilter.value = value
        _uiState.value = _uiState.value.copy(captureFilter = value)
    }

    fun retry() { refresh.value += 1 }

    fun archive(jobId: String) {
        viewModelScope.launch {
            val result = repository.archive(jobId)
            _uiState.value = _uiState.value.copy(
                actionMessage = if (result is RepositoryResult.Success) "Job archived." else "Could not archive this job.",
            )
        }
    }

    fun delete(jobId: String) {
        viewModelScope.launch {
            val result = repository.delete(jobId)
            _uiState.value = _uiState.value.copy(
                actionMessage = if (result is RepositoryResult.Success) "Job deleted." else "Could not delete this job.",
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeJobs() {
        viewModelScope.launch {
            combine(query.debounce(300), status, captureFilter, refresh) { text, selectedStatus, capture, _ -> Triple(text, selectedStatus, capture) }
                .flatMapLatest { (text, selectedStatus, capture) ->
                    repository.observeJobs(selectedStatus)
                        .onStart { _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null) }
                        .map { result -> Triple(result, text, capture) }
                }
                .collect { (result, text, capture) ->
                    when (result) {
                        is RepositoryResult.Success -> {
                            val needle = text.trim()
                            val searched = if (needle.isEmpty()) result.value else result.value.filter { job ->
                                job.title.contains(needle, true) ||
                                    job.companyName.contains(needle, true) ||
                                    job.location.orEmpty().contains(needle, true)
                            }
                            val filtered = when (capture) {
                                CaptureFilter.ALL -> searched
                                CaptureFilter.SCRAPED -> searched.filter { !it.rawCapturedText.isNullOrBlank() }
                                CaptureFilter.CORRECTED -> searched.filter(Job::aiCorrected)
                            }
                            _uiState.value = _uiState.value.copy(
                                jobs = filtered,
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                        is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Could not load jobs. Check local storage and retry.",
                        )
                    }
                }
        }
    }
}


