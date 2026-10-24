package com.saikalyan.jobmate.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.matching.ContactMatchSuggestion
import com.saikalyan.jobmate.domain.matching.ContactMatcher
import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.JobContactLinkRepository
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactMatchUiState(
    val contact: Contact? = null,
    val suggestions: List<ContactMatchSuggestion> = emptyList(),
    val confirmedJobIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ContactMatchViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val jobRepository: JobRepository,
    private val linkRepository: JobContactLinkRepository,
    private val matcher: ContactMatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactMatchUiState())
    val uiState: StateFlow<ContactMatchUiState> = _uiState.asStateFlow()

    fun load(contactId: String) {
        viewModelScope.launch {
            val contacts = contactRepository.observeContacts().first()
            val contact = (contacts as? RepositoryResult.Success)?.value?.firstOrNull { it.id == contactId }
            val jobs = (jobRepository.observeJobs().first() as? RepositoryResult.Success)?.value
            if (contact == null || jobs == null) {
                _uiState.value = ContactMatchUiState(isLoading = false, error = "Could not load contact or job data.")
                return@launch
            }
            val linked = (linkRepository.observeForContact(contactId).first() as? RepositoryResult.Success)?.value.orEmpty()
            _uiState.value = ContactMatchUiState(
                contact = contact,
                suggestions = matcher.suggest(contact, jobs),
                confirmedJobIds = linked,
                isLoading = false,
            )
        }
    }

    fun confirm(contactId: String, suggestion: ContactMatchSuggestion) {
        viewModelScope.launch {
            when (linkRepository.confirmLink(suggestion.job.id, contactId, suggestion.confidence)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(
                    confirmedJobIds = _uiState.value.confirmedJobIds + suggestion.job.id,
                    message = "Contact linked to ${suggestion.job.companyName}.",
                )
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Could not save this link.")
            }
        }
    }

    fun unlink(contactId: String, jobId: String) {
        viewModelScope.launch {
            when (linkRepository.removeLink(jobId, contactId)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(
                    confirmedJobIds = _uiState.value.confirmedJobIds - jobId,
                    message = "Contact link removed.",
                )
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Could not remove this link.")
            }
        }
    }
}
