package com.saikalyan.jobmate.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.EmailTemplate
import com.saikalyan.jobmate.domain.repository.EmailTemplateRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

data class TemplateListUiState(val templates: List<EmailTemplate> = emptyList(), val loading: Boolean = true, val error: String? = null)

@HiltViewModel
class TemplateListViewModel @Inject constructor(
    private val repository: EmailTemplateRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTemplates().collect { result ->
                _uiState.value = when (result) {
                    is RepositoryResult.Success -> TemplateListUiState(result.value, loading = false)
                    is RepositoryResult.Failure -> TemplateListUiState(loading = false, error = "Could not load templates.")
                }
            }
        }
    }

    fun save(template: EmailTemplate?) = Unit

    fun save(id: String?, name: String, subject: String, body: String, builtIn: Boolean) {
        viewModelScope.launch {
            val template = EmailTemplate(
                id = id ?: UUID.randomUUID().toString(),
                name = name,
                subjectTemplate = subject,
                bodyTemplate = body,
                isBuiltIn = builtIn,
                updatedAt = clock.instant(),
            )
            when (repository.save(template)) {
                is RepositoryResult.Success -> Unit
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Template name, subject, and body are required.")
            }
        }
    }

    fun delete(templateId: String) {
        viewModelScope.launch {
            if (repository.delete(templateId) is RepositoryResult.Failure) {
                _uiState.value = _uiState.value.copy(error = "Built-in templates cannot be deleted.")
            }
        }
    }
}

