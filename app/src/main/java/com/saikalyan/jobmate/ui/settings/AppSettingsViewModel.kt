package com.saikalyan.jobmate.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.storage.AppPreferences
import com.saikalyan.jobmate.data.storage.PrivacyDataManager
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.EmailTemplateRepository
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.OutreachDraftRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class AppSettingsState(
    val darkTheme: Boolean = false,
    val senderName: String = "",
    val linkedInProfileUrl: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val privacy: PrivacyDataManager,
    private val jobs: JobRepository,
    private val contacts: ContactRepository,
    private val templates: EmailTemplateRepository,
    private val drafts: OutreachDraftRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppSettingsState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch { preferences.darkTheme.collect { _state.value = _state.value.copy(darkTheme = it) } }
        viewModelScope.launch { preferences.senderName.collect { _state.value = _state.value.copy(senderName = it) } }
        viewModelScope.launch { preferences.linkedInProfileUrl.collect { _state.value = _state.value.copy(linkedInProfileUrl = it) } }
    }
    fun setDarkTheme(enabled: Boolean) { viewModelScope.launch { preferences.setDarkTheme(enabled) } }
    fun saveSenderName(name: String) {
        viewModelScope.launch {
            try {
                preferences.setSenderName(name)
                _state.value = _state.value.copy(message = "Sender name saved for outreach drafts.")
            } catch (_: Exception) {
                _state.value = _state.value.copy(message = "Could not save the sender name.")
            }
        }
    }
    fun saveLinkedInProfileUrl(url: String) {
        viewModelScope.launch {
            try {
                preferences.setLinkedInProfileUrl(url)
                _state.value = _state.value.copy(message = "LinkedIn profile URL saved. It will be added below your email signature.")
            } catch (_: Exception) {
                _state.value = _state.value.copy(message = "Could not save the LinkedIn profile URL.")
            }
        }
    }
    fun deleteAll() { _state.value = _state.value.copy(busy = true, message = null); viewModelScope.launch {
        try { privacy.deleteAll(); _state.value = AppSettingsState(message = "Local jobs, contacts, outreach, templates, resumes, sender name, LinkedIn profile URL, and saved provider credentials were deleted.") }
        catch (_: Exception) { _state.value = _state.value.copy(busy = false, message = "Could not delete all local data. Try again.") }
    } }
    fun export(context: Context, uri: Uri) { _state.value = _state.value.copy(busy = true, message = null); viewModelScope.launch {
        try {
            val jobsResult = jobs.observeJobs().first()
            val contactsResult = contacts.observeContacts().first()
            val templateResult = templates.observeTemplates().first()
            val draftResult = drafts.observeDrafts().first()
            val content = buildString {
                appendLine("type,id,primary,secondary,details")
                if (jobsResult !is RepositoryResult.Success || contactsResult !is RepositoryResult.Success || templateResult !is RepositoryResult.Success || draftResult !is RepositoryResult.Success) error("Could not read all local data")
                jobsResult.value.forEach { appendLine(row("job", it.id, it.title, it.companyName, "${it.status};${it.location.orEmpty()};${it.sourceUrl.orEmpty()};${it.notes.orEmpty()}")) }
                contactsResult.value.forEach { appendLine(row("contact", it.id, it.fullName, it.normalizedEmail, "${it.companyName.orEmpty()};${it.designation.orEmpty()};${it.notes.orEmpty()}")) }
                templateResult.value.forEach { appendLine(row("template", it.id, it.name, it.subjectTemplate, it.bodyTemplate)) }
                draftResult.value.forEach { appendLine(row("outreach", it.id, it.recipientName, it.recipientEmail, "${it.status};${it.subject};${it.body}")) }
            }
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) } ?: error("Could not open export destination") }
            _state.value = _state.value.copy(busy = false, message = "Export saved. The CSV contains jobs, contacts, templates, and outreach records; passwords and resume files are excluded.")
        } catch (_: Exception) { _state.value = _state.value.copy(busy = false, message = "Export failed. No local data was changed.") }
    } }
    private fun row(vararg values: String): String = values.joinToString(",") { "\"${it.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")}\"" }
}
