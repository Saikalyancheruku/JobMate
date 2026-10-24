package com.saikalyan.jobmate.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface JobDetailUiState {
    data object Loading : JobDetailUiState
    data class Content(val job: Job, val actionError: String? = null) : JobDetailUiState
    data class Error(val message: String) : JobDetailUiState
}

sealed interface JobDetailEvent {
    data object Exit : JobDetailEvent
}

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<JobDetailUiState>(JobDetailUiState.Loading)
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<JobDetailEvent>()
    val events = _events.asSharedFlow()

    fun load(jobId: String) {
        viewModelScope.launch {
            repository.observeJob(jobId).collect { result ->
                _uiState.value = when (result) {
                    is RepositoryResult.Success -> result.value?.let(JobDetailUiState::Content)
                        ?: JobDetailUiState.Error("This job no longer exists.")
                    is RepositoryResult.Failure -> JobDetailUiState.Error("Could not load this job.")
                }
            }
        }
    }

    fun archive(jobId: String) {
        viewModelScope.launch {
            when (repository.archive(jobId)) {
                is RepositoryResult.Success -> _events.emit(JobDetailEvent.Exit)
                is RepositoryResult.Failure -> showActionError("Could not archive this job.")
            }
        }
    }

    fun delete(jobId: String) {
        viewModelScope.launch {
            when (repository.delete(jobId)) {
                is RepositoryResult.Success -> _events.emit(JobDetailEvent.Exit)
                is RepositoryResult.Failure -> showActionError("Could not delete this job.")
            }
        }
    }

    fun updateStatus(jobId: String, status: JobStatus) {
        viewModelScope.launch {
            when (repository.updateStatus(jobId, status)) {
                is RepositoryResult.Success -> Unit
                is RepositoryResult.Failure -> showActionError("Could not update job status.")
            }
        }
    }

    private fun showActionError(message: String) {
        val current = _uiState.value
        if (current is JobDetailUiState.Content) _uiState.value = current.copy(actionError = message)
    }
}
