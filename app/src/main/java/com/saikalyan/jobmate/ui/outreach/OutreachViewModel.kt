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
import com.saikalyan.jobmate.data.remote.GeminiReferralDraftClient
import com.saikalyan.jobmate.data.secure.AiApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    val contactSearch: String = "",
    val companyMatchesOnly: Boolean = false,
    val isGenerating: Boolean = false,
    val isSendingBatch: Boolean = false,
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
    private val aiStore: AiApiKeyStore,
    private val referralClient: GeminiReferralDraftClient,
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
    fun setContactSearch(value: String) = update { it.copy(contactSearch = value).repreview() }
    fun showCompanyMatches(enabled: Boolean) = update { it.copy(companyMatchesOnly = enabled) }
    fun toggleContact(id: String) = update { state -> state.copy(contactIds = if (id in state.contactIds) state.contactIds - id else state.contactIds + id).repreview() }
    fun selectTemplate(id: String?) = update { state ->
        val template = state.templates.firstOrNull { it.id == id }
        state.copy(templateId = id, subject = template?.subjectTemplate.orEmpty(), body = template?.bodyTemplate.orEmpty()).repreview()
    }
    fun selectResume(id: String?) = update { it.copy(resumeId = id) }
    fun setSenderName(value: String) = update { it.copy(senderName = value).repreview() }
    fun setSubject(value: String) = update { it.copy(subject = value).repreview() }
    fun setBody(value: String) = update { it.copy(body = value).repreview() }

    fun generateAiDraft() {
        val current = _uiState.value
        val job = current.jobs.firstOrNull { it.id == current.jobId }
        if (job == null) { update { it.copy(error = "Choose a job before generating an email.") }; return }
        if (current.isGenerating) return
        update { it.copy(isGenerating = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (aiStore.provider.first() != "Gemini") throw IllegalStateException("Save a Gemini API key in AI settings first.")
                val key = aiStore.readKey() ?: throw IllegalStateException("Save a Gemini API key in AI settings first.")
                val profile = aiStore.readProfile()?.takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Add and save your profile in AI settings first.")
                val generated = referralClient.generate(job, profile, key)
                update { it.copy(isGenerating = false, templateId = null, subject = generated.subject, body = generated.body,
                    message = "AI draft created from this job and your saved profile. Review it before use.").repreview() }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { update { it.copy(isGenerating = false, error = failure.message ?: "Could not generate an AI email draft.") } }
        }
    }

    fun saveDrafts() {
        val state = _uiState.value
        val job = state.jobs.firstOrNull { it.id == state.jobId }
        val template = state.templates.firstOrNull { it.id == state.templateId }
        val recipients = state.contacts.filter { it.id in state.contactIds }
        if (job == null || recipients.isEmpty() || state.subject.isBlank() || state.body.isBlank()) {
            update { it.copy(error = "Choose a job, complete the message, and select at least one contact.") }
            return
        }
        val previews = recipients.map { recipient -> recipient to render(state, job, recipient) }
        val blocked = previews.filter { it.second !is TemplateRenderResult.Rendered }
        if (blocked.isNotEmpty()) {
            update { it.copy(error = "Complete required template values before saving drafts.") }
            return
        }
        val now = clock.instant()
        val resume = state.resumes.firstOrNull { it.id == state.resumeId }
        if (state.resumeId != null && resume == null) {
            update { it.copy(error = "The selected resume is no longer available. Choose it again before saving.") }
            return
        }
        val draftRows = previews.map { (recipient, render) ->
            val rendered = render as TemplateRenderResult.Rendered
            OutreachDraft(
                id = UUID.randomUUID().toString(),
                jobId = job.id,
                contactId = recipient.id,
                templateId = template?.id,
                resumeId = resume?.id,
                recipientName = recipient.fullName,
                recipientEmail = recipient.normalizedEmail,
                jobTitle = job.title,
                companyName = job.companyName,
                templateName = template?.name ?: "AI referral draft",
                resumeDisplayName = resume?.displayName,
                subject = rendered.subject,
                body = rendered.body,
                messageKind = "INITIAL",
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
        if (_uiState.value.isSendingBatch) return
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

    /** Sends ready drafts for one selected job as separate messages, never as a multi-recipient email. */
    fun sendAllDraftsForJob(jobId: String) {
        val current = _uiState.value
        if (current.isSendingBatch || current.sendingDraftId != null || current.jobId != jobId) return
        val ids = current.drafts.filter { it.jobId == jobId && it.status == "DRAFT" && it.recipientEmail.isNotBlank() }
            .map { it.id }
        if (ids.isEmpty()) {
            update { it.copy(error = "There are no ready drafts for this job.") }
            return
        }
        update { it.copy(isSendingBatch = true, error = null, message = null) }
        viewModelScope.launch {
            var sent = 0
            var failed = 0
            var uncertain = 0
            var stopped = false
            for (id in ids) {
                when (val result = sender.sendDraft(id, userConfirmedUnknownRetry = false)) {
                    is RepositoryResult.Success -> when (result.value) {
                        com.saikalyan.jobmate.domain.model.OutreachSendResult.Sent -> sent++
                        is com.saikalyan.jobmate.domain.model.OutreachSendResult.Failed -> failed++
                        is com.saikalyan.jobmate.domain.model.OutreachSendResult.Unknown -> uncertain++
                        com.saikalyan.jobmate.domain.model.OutreachSendResult.UnknownRetryNeedsConfirmation -> uncertain++
                    }
                    is RepositoryResult.Failure -> {
                        stopped = true
                        break
                    }
                }
            }
            val summary = buildString {
                append("Batch finished: $sent sent")
                if (failed > 0) append(", $failed failed")
                if (uncertain > 0) append(", $uncertain with uncertain delivery—check Gmail Sent before retrying")
                if (stopped) append(". Remaining drafts were not sent because sending could not continue.")
                else append(". Each message was sent separately.")
            }
            update { it.copy(isSendingBatch = false, message = summary) }
        }
    }

    fun prepareFollowUp(sourceId: String) {
        val original = _uiState.value.drafts.firstOrNull { it.id == sourceId } ?: return
        if (original.status != "SENT" || original.nextFollowUpAt == null) return
        val contact = _uiState.value.contacts.firstOrNull { it.id == original.contactId }
        val name = contact?.fullName?.trim()?.substringBefore(' ')?.takeIf(String::isNotBlank) ?: "there"
        val now = clock.instant()
        val next = OutreachDraft(
            id = UUID.randomUUID().toString(), jobId = original.jobId, contactId = original.contactId,
            templateId = null, resumeId = null, recipientName = original.recipientName,
            recipientEmail = original.recipientEmail, jobTitle = original.jobTitle,
            companyName = original.companyName, templateName = "Follow-up ${original.followUpCount + 1}",
            resumeDisplayName = null,
            subject = if (original.subject.startsWith("Re:", true)) original.subject else "Re: ${original.subject}",
            body = "Hi $name,\n\nI wanted to follow up on my earlier note about ${original.jobTitle ?: "the role"} at ${original.companyName ?: "your company"}. If you have a moment, I would be grateful for any guidance or referral consideration.\n\nThank you,\n${_uiState.value.senderName}",
            messageKind = "FOLLOW_UP", parentOutreachId = original.id,
            followUpCount = original.followUpCount + 1, createdAt = now, updatedAt = now,
        )
        viewModelScope.launch {
            when (drafts.prepareFollowUp(sourceId, next)) {
                is RepositoryResult.Success -> update { it.copy(message = "Follow-up draft prepared. Review and send it manually.") }
                is RepositoryResult.Failure -> update { it.copy(error = "Could not prepare the follow-up draft.") }
            }
        }
    }

    fun clearUnknownRetry() { update { it.copy(unknownRetryDraftId = null) } }
    private fun render(state: OutreachComposerUiState, job: Job, contact: Contact): TemplateRenderResult {
        val result = renderer.render(
            state.subject,
            state.body,
            mapOf(
            "firstName" to contact.fullName.trim().substringBefore(' ').takeIf(String::isNotBlank),
            "contactName" to contact.fullName,
            "contactEmail" to contact.normalizedEmail,
            "contactCompany" to contact.companyName,
            "companyName" to (job.companyName.ifBlank { contact.companyName.orEmpty() }),
            "jobTitle" to job.title,
            "jobUrl" to job.sourceUrl,
            "senderName" to state.senderName,
            ),
        )
        if (result !is TemplateRenderResult.Rendered || state.resumeId == null) return result
        return result.copy(body = includeResumeAttachmentNotice(result.body))
    }

    private fun includeResumeAttachmentNotice(body: String): String {
        val alreadyMentioned = body.contains("resume", ignoreCase = true) || body.contains("cv", ignoreCase = true)
        val attachmentMentioned = body.contains("attach", ignoreCase = true) || body.contains("enclos", ignoreCase = true)
        if (alreadyMentioned && attachmentMentioned) return body
        val notice = "I've attached my resume for your reference."
        val signOff = Regex("(?im)^(thank you|thanks|regards|best regards|kind regards|sincerely|cheers)[,! ]*$")
        val lastSignOff = signOff.findAll(body).lastOrNull()
        if (lastSignOff == null) return "${body.trimEnd()}\n\n$notice"
        return body.substring(0, lastSignOff.range.first).trimEnd() + "\n\n$notice\n\n" + body.substring(lastSignOff.range.first)
    }

    private fun OutreachComposerUiState.repreview(): OutreachComposerUiState {
        val job = jobs.firstOrNull { it.id == jobId }
        val selectedContacts = contacts.filter { it.id in contactIds }
        val rendered = if (job == null) emptyList() else selectedContacts.map { contact ->
            RecipientPreview(contact, render(this, job, contact))
        }
        return copy(previews = rendered)
    }

    private fun update(transform: (OutreachComposerUiState) -> OutreachComposerUiState) {
        _uiState.value = transform(_uiState.value)
    }

    fun visibleContacts(): List<Contact> {
        val state = _uiState.value
        val jobCompany = normalizeCompany(state.jobs.firstOrNull { it.id == state.jobId }?.companyName)
        val query = state.contactSearch.trim().lowercase()
        return state.contacts
            .sortedWith(compareByDescending<Contact> { normalizeCompany(it.companyName) == jobCompany }.thenBy { it.fullName })
            .filter { query.isBlank() || it.fullName.lowercase().contains(query) ||
                it.companyName.orEmpty().lowercase().contains(query) || it.normalizedEmail.lowercase().contains(query) }
            .filter { !state.companyMatchesOnly || (jobCompany.isNotBlank() && normalizeCompany(it.companyName) == jobCompany) }
            .take(60)
    }

    private fun normalizeCompany(value: String?): String = value.orEmpty().lowercase()
        .replace(Regex("\\b(incorporated|corporation|limited|private|pvt|ltd|llp|inc)\\b"), "")
        .replace(Regex("[^a-z0-9]"), "")
}


