package com.saikalyan.jobmate.ui.contacts

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.importing.ContactColumnMapping
import com.saikalyan.jobmate.data.importing.ContactFilePreview
import com.saikalyan.jobmate.data.importing.ContactImportMapper
import com.saikalyan.jobmate.data.importing.ContactMappedPreview
import com.saikalyan.jobmate.data.importing.ContactImportParser
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactImportUiState(
    val file: ContactFilePreview? = null,
    val preview: ContactMappedPreview? = null,
    val isReading: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class ContactImportViewModel @Inject constructor(
    private val parser: ContactImportParser,
    private val mapper: ContactImportMapper,
    private val repository: ContactRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContactImportUiState())
    val uiState: StateFlow<ContactImportUiState> = _uiState.asStateFlow()
    private var mappingJob: Job? = null

    fun read(uri: Uri) {
        _uiState.value = ContactImportUiState(isReading = true)
        viewModelScope.launch {
            try {
                val file = parser.preview(uri)
                _uiState.value = ContactImportUiState(file = file)
                updateMapping(autoMapping(file.headers))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _uiState.value = ContactImportUiState(error = failure.message ?: "Could not read the selected file.")
            }
        }
    }

    fun updateMapping(mapping: ContactColumnMapping) {
        val file = _uiState.value.file ?: return
        mappingJob?.cancel()
        mappingJob = viewModelScope.launch {
            val existing = when (val result = repository.observeContacts().first()) {
                is RepositoryResult.Success -> result.value.map { it.normalizedEmail }.toSet()
                is RepositoryResult.Failure -> {
                    _uiState.value = _uiState.value.copy(error = "Could not check existing contacts for duplicates.")
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(preview = mapper.map(file, mapping, existing), error = null)
        }
    }

    fun importEligibleContacts() {
        val candidates = _uiState.value.preview?.eligibleContacts.orEmpty()
        if (candidates.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "There are no eligible contacts to import.")
            return
        }
        _uiState.value = _uiState.value.copy(isImporting = true, error = null, message = null)
        viewModelScope.launch {
            when (val result = repository.importContacts(candidates)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    message = "Imported ${result.value.importedCount}; skipped ${result.value.duplicateCount} duplicates.",
                )
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Could not import contacts. Review the file and try again.",
                )
            }
        }
    }

    private fun autoMapping(headers: List<String>): ContactColumnMapping {
        fun find(vararg tokens: String): Int? = headers.indexOfFirst { header ->
            val value = header.trim().lowercase(java.util.Locale.ROOT)
            tokens.any { value.contains(it) }
        }.takeIf { it >= 0 }
        return ContactColumnMapping(
            name = find("full name", "contact name", "name"),
            email = find("email", "e-mail"),
            company = find("company", "organization", "organisation"),
            designation = find("title", "designation", "role"),
            notes = find("note", "comment"),
        )
    }
}

