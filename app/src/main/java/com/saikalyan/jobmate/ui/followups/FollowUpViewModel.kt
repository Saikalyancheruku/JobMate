package com.saikalyan.jobmate.ui.followups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.worker.FollowUpScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class FollowUpUiState(val jobs: List<Job> = emptyList(), val message: String? = null)

@HiltViewModel
class FollowUpViewModel @Inject constructor(private val jobs: JobRepository, private val scheduler: FollowUpScheduler, private val clock: Clock) : ViewModel() {
    private val _state = MutableStateFlow(FollowUpUiState())
    val state = _state.asStateFlow()
    init { viewModelScope.launch { jobs.observeJobs().collect { result ->
        when (result) {
            is RepositoryResult.Success -> {
                val upcoming = result.value.filter { it.followUpAt != null && it.status.name != "CLOSED" }.sortedBy { it.followUpAt }
                upcoming.forEach(scheduler::schedule)
                _state.value = FollowUpUiState(upcoming, if (upcoming.isEmpty()) "No follow-ups scheduled. Set a follow-up date from a job's edit screen." else null)
            }
            is RepositoryResult.Failure -> _state.value = FollowUpUiState(message = "Could not load follow-ups.")
        }
    } } }
    fun dismiss(job: Job) { scheduler.cancel(job.id) }
}
