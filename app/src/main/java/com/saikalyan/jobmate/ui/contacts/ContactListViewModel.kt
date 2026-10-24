package com.saikalyan.jobmate.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.repository.ContactRepository
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
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ContactListUiState(
    val contacts: List<Contact> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val repository: ContactRepository,
    private val clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val refresh = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init { observe() }

    fun setQuery(value: String) {
        query.value = value
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun save(existing: Contact?, fullName: String, email: String, company: String, designation: String, notes: String) {
        val now = clock.instant()
        viewModelScope.launch {
            val result = repository.save(
                Contact(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    fullName = fullName,
                    normalizedEmail = email.lowercase(Locale.ROOT),
                    companyName = company,
                    designation = designation,
                    notes = notes,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            _uiState.value = _uiState.value.copy(message = if (result is RepositoryResult.Success) "Contact saved." else "Could not save contact.")
        }
    }

    fun delete(contactId: String) {
        viewModelScope.launch {
            val result = repository.delete(contactId)
            _uiState.value = _uiState.value.copy(message = if (result is RepositoryResult.Success) "Contact deleted." else "Could not delete contact.")
        }
    }

    fun retry() { refresh.value += 1 }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observe() {
        viewModelScope.launch {
            combine(query.debounce(250), refresh) { value, _ -> value }
                .flatMapLatest { value ->
                    val contacts = if (value.isBlank()) repository.observeContacts() else repository.searchContacts(value)
                    contacts.onStart { _uiState.value = _uiState.value.copy(isLoading = true, error = null) }
                }
                .collect { result ->
                    _uiState.value = when (result) {
                        is RepositoryResult.Success -> _uiState.value.copy(contacts = result.value, isLoading = false, error = null)
                        is RepositoryResult.Failure -> _uiState.value.copy(isLoading = false, error = "Could not load contacts.")
                    }
                }
        }
    }
}
