package com.saikalyan.jobmate.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.remote.GeminiJobCorrectionClient
import com.saikalyan.jobmate.data.secure.AiApiKeyStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

sealed interface JobDetailUiState {
    data object Loading : JobDetailUiState
    data class Content(
        val job: Job,
        val actionError: String? = null,
        val isCorrecting: Boolean = false,
        val correctionMessage: String? = null,
    ) : JobDetailUiState
    data class Error(val message: String) : JobDetailUiState
}

sealed interface JobDetailEvent {
    data object Exit : JobDetailEvent
}

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository,
    private val apiKeyStore: AiApiKeyStore,
    private val correctionClient: GeminiJobCorrectionClient,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow<JobDetailUiState>(JobDetailUiState.Loading)
    val uiState: StateFlow<JobDetailUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<JobDetailEvent>()
    val events = _events.asSharedFlow()

    fun load(jobId: String) {
        viewModelScope.launch {
            repository.observeJob(jobId).collect { result ->
                val transient = _uiState.value as? JobDetailUiState.Content
                _uiState.value = when (result) {
                    is RepositoryResult.Success -> result.value?.let { job ->
                        JobDetailUiState.Content(
                            job = job,
                            actionError = transient?.actionError,
                            isCorrecting = transient?.isCorrecting ?: false,
                            correctionMessage = transient?.correctionMessage,
                        )
                    } ?: JobDetailUiState.Error("This job no longer exists.")
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

    fun correctDetails(jobId: String) {
        val current = _uiState.value as? JobDetailUiState.Content ?: return
        if (current.isCorrecting) return
        if (current.job.rawCapturedText.isNullOrBlank()) {
            _uiState.value = current.copy(actionError = "This job has no saved OCR text to send for correction.")
            return
        }
        _uiState.value = current.copy(isCorrecting = true, actionError = null, correctionMessage = null)
        viewModelScope.launch {
            try {
                val provider = apiKeyStore.provider.first()
                if (provider != "Gemini") throw IllegalStateException("Save a Gemini API key in AI correction settings first.")
                val key = apiKeyStore.readKey() ?: throw IllegalStateException("Save a Gemini API key in AI correction settings first.")
                val fields = correctionClient.correct(current.job, key)
                val updated = fields.applyTo(current.job).copy(aiCorrected = true, updatedAt = clock.instant())
                when (repository.save(updated)) {
                    is RepositoryResult.Success -> {
                        val latest = _uiState.value as? JobDetailUiState.Content
                        if (latest != null) _uiState.value = latest.copy(
                            job = updated,
                            isCorrecting = false,
                            correctionMessage = "Job fields corrected with Gemini. Application status and dates were left unchanged.",
                        )
                    }
                    is RepositoryResult.Failure -> showCorrectionError("Could not save the corrected fields. Your existing job data is unchanged.")
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showCorrectionError(failure.message ?: "Gemini correction failed. Your existing job data is unchanged.")
            }
        }
    }

    private fun showCorrectionError(message: String) {
        val latest = _uiState.value as? JobDetailUiState.Content ?: return
        _uiState.value = latest.copy(isCorrecting = false, actionError = message)
    }

    private fun showActionError(message: String) {
        val current = _uiState.value
        if (current is JobDetailUiState.Content) _uiState.value = current.copy(actionError = message)
    }
}
