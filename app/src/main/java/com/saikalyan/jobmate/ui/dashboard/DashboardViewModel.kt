package com.saikalyan.jobmate.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.usecase.ObserveJobDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardLoadState {
    data object Loading : DashboardLoadState
    data class Content(val summary: JobDashboardSummary) : DashboardLoadState
    data class Error(val message: String) : DashboardLoadState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboard: ObserveJobDashboardUseCase,
) : ViewModel() {
    private val refreshRequests = MutableStateFlow(0)
    private val _uiState = MutableStateFlow<DashboardLoadState>(DashboardLoadState.Loading)
    val uiState: StateFlow<DashboardLoadState> = _uiState.asStateFlow()

    init {
        observe()
    }

    fun retry() {
        refreshRequests.value += 1
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe() {
        viewModelScope.launch {
            refreshRequests.flatMapLatest {
                observeDashboard().onStart { _uiState.value = DashboardLoadState.Loading }
            }.collect { result ->
                _uiState.value = when (result) {
                    is RepositoryResult.Success -> DashboardLoadState.Content(result.value)
                    is RepositoryResult.Failure -> DashboardLoadState.Error(result.error.toUserMessage())
                }
            }
        }
    }

    private fun com.saikalyan.jobmate.domain.repository.RepositoryError.toUserMessage(): String =
        when (this) {
            is com.saikalyan.jobmate.domain.repository.RepositoryError.InvalidInput -> "Dashboard data is invalid."
            com.saikalyan.jobmate.domain.repository.RepositoryError.NotFound -> "Dashboard data is unavailable."
            com.saikalyan.jobmate.domain.repository.RepositoryError.Conflict -> "Dashboard data could not be loaded."
            is com.saikalyan.jobmate.domain.repository.RepositoryError.CorruptData -> "Some saved job data is invalid."
            com.saikalyan.jobmate.domain.repository.RepositoryError.Storage,
            com.saikalyan.jobmate.domain.repository.RepositoryError.Unexpected -> "Could not load your saved jobs. Try again."
        }
}
