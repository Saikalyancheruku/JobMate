package com.saikalyan.jobmate.ui.outreach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.model.EmailTemplate
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.OutreachDraft
import com.saikalyan.jobmate.domain.model.Resume
import com.saikalyan.jobmate.domain.model.TemplateRenderResult
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.EmailTemplateRepository
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.OutreachDraftRepository
import com.saikalyan.jobmate.domain.repository.OutreachSendRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.repository.ResumeRepository
import com.saikalyan.jobmate.domain.usecase.TemplateRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

data class RecipientPreview(val contact: Contact, val result: TemplateRenderResult)

data class OutreachComposerUiState(
    val jobs: List<Job> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val templates: List<EmailTemplate> = emptyList(),
    val resumes: List<Resume> = emptyList(),
    val drafts: List<OutreachDraft> = emptyList(),
    val jobId: String? = null,
    val contactIds: Set<String> = emptySet(),
    val templateId: String? = null,
    val resumeId: String? = null,
    val senderName: String = "",
    val subject: String = "",
    val body: String = "",
    val previews: List<RecipientPreview> = emptyList(),
    val isSaving: Boolean = false,
    val sendingDraftId: String? = null,
    val unknownRetryDraftId: String? = null,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class OutreachViewModel @Inject constructor(
    jobs: JobRepository,
    contacts: ContactRepository,
    templates: EmailTemplateRepository,
    resumes: ResumeRepository,
    private val drafts: OutreachDraftRepository,
    private val sender: OutreachSendRepository,
    private val renderer: TemplateRenderer,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OutreachComposerUiState())
    val uiState: StateFlow<OutreachComposerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { jobs.observeJobs().collect { result -> when (result) {
            is RepositoryResult.Success -> update { it.copy(jobs = result.value) }
            is RepositoryResult.Failure -> update { it.copy(error = "Could not load jobs for outreach.") }
        } } }
        viewModelScope.launch { contacts.observeContacts().collect { result -> when (result) {
            is RepositoryResult.Success -> update { it.copy(contacts = result.value) }
            is RepositoryResult.Failure -> update { it.copy(error = "Could not load contacts for outreach.") }
        } } }
        viewModelScope.launch { templates.observeTemplates().collect { result -> when (result) {
            is RepositoryResult.Success -> update { it.copy(templates = result.value) }
            is RepositoryResult.Failure -> update { it.copy(error = "Could not load templates.") }
        } } }
        viewModelScope.launch { resumes.observeResumes().collect { result -> when (result) {
            is RepositoryResult.Success -> update { it.copy(resumes = result.value) }
            is RepositoryResult.Failure -> update { it.copy(error = "Could not load resumes.") }
        } } }
        viewModelScope.launch { drafts.observeDrafts().collect { result -> when (result) {
            is RepositoryResult.Success -> update { it.copy(drafts = result.value) }
            is RepositoryResult.Failure -> update { it.copy(error = "Could not load drafts.") }
        } } }
    }

    fun selectJob(id: String?) = update { it.copy(jobId = id).repreview() }
    fun toggleContact(id: String) = update { state -> state.copy(contactIds = if (id in state.contactIds) state.contactIds - id else state.contactIds + id).repreview() }
    fun selectTemplate(id: String?) = update { state ->
        val template = state.templates.firstOrNull { it.id == id }
        state.copy(templateId = id, subject = template?.subjectTemplate.orEmpty(), body = template?.bodyTemplate.orEmpty()).repreview()
    }
    fun selectResume(id: String?) = update { it.copy(resumeId = id) }
    fun setSenderName(value: String) = update { it.copy(senderName = value).repreview() }
    fun setSubject(value: String) = update { it.copy(subject = value).repreview() }
    fun setBody(value: String) = update { it.copy(body = value).repreview() }

    fun saveDrafts() {
        val state = _uiState.value
        val job = state.jobs.firstOrNull { it.id == state.jobId }
        val template = state.templates.firstOrNull { it.id == state.templateId }
        val recipients = state.contacts.filter { it.id in state.contactIds }
        if (job == null || template == null || recipients.isEmpty()) {
            update { it.copy(error = "Choose a job, template, and at least one contact.") }
            return
        }
        val previews = recipients.map { recipient -> recipient to render(state, job, template, recipient) }
        val blocked = previews.filter { it.second !is TemplateRenderResult.Rendered }
        if (blocked.isNotEmpty()) {
            update { it.copy(error = "Complete required template values before saving drafts.") }
            return
        }
        val now = clock.instant()
        val resume = state.resumes.firstOrNull { it.id == state.resumeId }
        val draftRows = previews.map { (recipient, render) ->
            val rendered = render as TemplateRenderResult.Rendered
            OutreachDraft(
                id = UUID.randomUUID().toString(),
                jobId = job.id,
                contactId = recipient.id,
                templateId = template.id,
                resumeId = resume?.id,
                recipientName = recipient.fullName,
                recipientEmail = recipient.normalizedEmail,
                jobTitle = job.title,
                companyName = job.companyName,
                templateName = template.name,
                resumeDisplayName = resume?.displayName,
                subject = rendered.subject,
                body = rendered.body,
                createdAt = now,
                updatedAt = now,
            )
        }
        update { it.copy(isSaving = true, error = null, message = null) }
        viewModelScope.launch {
            when (drafts.saveDrafts(draftRows)) {
                is RepositoryResult.Success -> update { it.copy(isSaving = false, message = "Saved ${draftRows.size} separate draft(s). Nothing was sent.") }
                is RepositoryResult.Failure -> update { it.copy(isSaving = false, error = "Could not save outreach drafts.") }
            }
        }
    }


    fun sendDraft(outreachId: String, confirmUnknownRetry: Boolean = false) {
        update { it.copy(sendingDraftId = outreachId, error = null, message = null, unknownRetryDraftId = null) }
        viewModelScope.launch {
            when (val result = sender.sendDraft(outreachId, confirmUnknownRetry)) {
                is RepositoryResult.Failure -> update { it.copy(sendingDraftId = null, error = "Could not start sending. Check Gmail settings and review the draft.") }
                is RepositoryResult.Success -> when (result.value) {
                    com.saikalyan.jobmate.domain.model.OutreachSendResult.Sent -> update { it.copy(sendingDraftId = null, message = "Message sent to one recipient.") }
                    is com.saikalyan.jobmate.domain.model.OutreachSendResult.Failed -> update { it.copy(sendingDraftId = null, error = "Send failed (${result.value.reasonCode}). You can review and retry this failed attempt.") }
                    is com.saikalyan.jobmate.domain.model.OutreachSendResult.Unknown -> update { it.copy(sendingDraftId = null, error = "Delivery is uncertain. Check Gmail Sent before deciding what to do.") }
                    com.saikalyan.jobmate.domain.model.OutreachSendResult.UnknownRetryNeedsConfirmation -> update { it.copy(sendingDraftId = null, unknownRetryDraftId = outreachId) }
                }
            }
        }
    }

    fun clearUnknownRetry() { update { it.copy(unknownRetryDraftId = null) } }
    private fun render(state: OutreachComposerUiState, job: Job, template: EmailTemplate, contact: Contact) = renderer.render(
        state.subject,
        state.body,
        mapOf(
            "firstName" to contact.fullName.trim().substringBefore(' ').takeIf(String::isNotBlank),
            "companyName" to (job.companyName.ifBlank { contact.companyName.orEmpty() }),
            "jobTitle" to job.title,
            "jobUrl" to job.sourceUrl,
            "senderName" to state.senderName,
        ),
    )

    private fun OutreachComposerUiState.repreview(): OutreachComposerUiState {
        val job = jobs.firstOrNull { it.id == jobId }
        val template = templates.firstOrNull { it.id == templateId }
        val selectedContacts = contacts.filter { it.id in contactIds }
        val rendered = if (job == null || template == null) emptyList() else selectedContacts.map { contact ->
            RecipientPreview(contact, render(this, job, template, contact))
        }
        return copy(previews = rendered)
    }

    private fun update(transform: (OutreachComposerUiState) -> OutreachComposerUiState) {
        _uiState.value = transform(_uiState.value)
    }
}


