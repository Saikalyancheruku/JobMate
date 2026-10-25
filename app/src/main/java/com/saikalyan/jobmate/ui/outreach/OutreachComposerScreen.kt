package com.saikalyan.jobmate.ui.outreach

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.domain.model.EmailTemplate
import com.saikalyan.jobmate.domain.model.OutreachDraft
import com.saikalyan.jobmate.domain.model.TemplateRenderResult

@Composable
fun OutreachComposerRoute(viewModel: OutreachViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OutreachComposerScreen(
        state = state,
        onJob = viewModel::selectJob,
        onContact = viewModel::toggleContact,
        visibleContacts = viewModel.visibleContacts(),
        onCompanyMatchesOnly = viewModel::showCompanyMatches,
        onContactSearch = viewModel::setContactSearch,
        onTemplate = viewModel::selectTemplate,
        onResume = viewModel::selectResume,
        onSender = viewModel::setSenderName,
        onSubject = viewModel::setSubject,
        onBody = viewModel::setBody,
        onSaveDrafts = viewModel::saveDrafts,
        onGenerateAi = viewModel::generateAiDraft,
        onSendAllDrafts = viewModel::sendAllDraftsForJob,
        onPrepareFollowUp = viewModel::prepareFollowUp,
        onSendDraft = viewModel::sendDraft,
        onClearUnknownRetry = viewModel::clearUnknownRetry,
    )
}

@Composable
fun OutreachComposerScreen(
    state: OutreachComposerUiState,
    onJob: (String?) -> Unit,
    onContact: (String) -> Unit,
    visibleContacts: List<com.saikalyan.jobmate.domain.model.Contact>,
    onCompanyMatchesOnly: (Boolean) -> Unit,
    onContactSearch: (String) -> Unit,
    onTemplate: (String?) -> Unit,
    onResume: (String?) -> Unit,
    onSender: (String) -> Unit,
    onSubject: (String) -> Unit,
    onBody: (String) -> Unit,
    onSaveDrafts: () -> Unit,
    onGenerateAi: () -> Unit,
    onSendAllDrafts: (String) -> Unit,
    onPrepareFollowUp: (String) -> Unit,
    onSendDraft: (String, Boolean) -> Unit,
    onClearUnknownRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSave by remember { mutableStateOf(false) }
    var sendTarget by remember { mutableStateOf<OutreachDraft?>(null) }
    var batchJobTarget by remember { mutableStateOf<String?>(null) }
    if (confirmSave) AlertDialog(
        onDismissRequest = { confirmSave = false },
        title = { Text("Save outreach drafts?") },
        text = { Text("This creates ${state.contactIds.size} separate local draft(s), one per recipient. No email will be sent.") },
        confirmButton = { TextButton(onClick = { confirmSave = false; onSaveDrafts() }) { Text("Save drafts") } },
        dismissButton = { TextButton(onClick = { confirmSave = false }) { Text("Cancel") } },
    )
    sendTarget?.let { draft -> AlertDialog(
        onDismissRequest = { sendTarget = null },
        title = { Text("Send one email?") },
        text = { Text("To: ${draft.recipientEmail}\nSubject: ${draft.subject}\n\nThis sends one separate email only to this recipient.") },
        confirmButton = { TextButton(onClick = { sendTarget = null; onSendDraft(draft.id, false) }) { Text("Send now") } },
        dismissButton = { TextButton(onClick = { sendTarget = null }) { Text("Cancel") } },
    ) }
    val unknownDraft = state.unknownRetryDraftId?.let { id -> state.drafts.firstOrNull { it.id == id } }
    if (unknownDraft != null) AlertDialog(
        onDismissRequest = onClearUnknownRetry,
        title = { Text("Verify before retrying") },
        text = { Text("The previous result is uncertain. Check the Gmail Sent folder. Retry only if you confirmed it was not sent to ${unknownDraft.recipientEmail}.") },
        confirmButton = { TextButton(onClick = { onClearUnknownRetry(); onSendDraft(unknownDraft.id, true) }) { Text("I verified; retry") } },
        dismissButton = { TextButton(onClick = onClearUnknownRetry) { Text("Cancel") } },
    )
    val batchDrafts = batchJobTarget?.let { jobId ->
        state.drafts.filter { it.jobId == jobId && it.status == "DRAFT" && it.recipientEmail.isNotBlank() }
    }.orEmpty()
    if (batchJobTarget != null) AlertDialog(
        onDismissRequest = { batchJobTarget = null },
        title = { Text("Send ${batchDrafts.size} separate emails?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Review the recipient-specific previews below. Confirming sends one separate message to each listed contact. Failed or uncertain deliveries will be reported; uncertain messages will not be retried automatically.")
                batchDrafts.take(12).forEach { draft ->
                    Text("${draft.recipientName} <${draft.recipientEmail}> · ${draft.templateName ?: draft.messageKind}", style = MaterialTheme.typography.bodySmall)
                    Text(draft.subject, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (batchDrafts.size > 12) Text("And ${batchDrafts.size - 12} more ready drafts.")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val jobId = batchJobTarget
                batchJobTarget = null
                if (jobId != null) onSendAllDrafts(jobId)
            }, enabled = batchDrafts.isNotEmpty() && !state.isSendingBatch) { Text("Send these emails") }
        },
        dismissButton = { TextButton(onClick = { batchJobTarget = null }) { Text("Cancel") } },
    )

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Outreach composer", style = MaterialTheme.typography.headlineMedium)
        Text("Preview each recipient separately. Saving creates local drafts; sending always requires an explicit confirmation.")
        ChoiceMenu("Choose job", state.jobs.map { it.id to "${it.title} · ${it.companyName}" }, state.jobId, onJob)
        Text("Recipients", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onCompanyMatchesOnly(!state.companyMatchesOnly) }, enabled = state.jobId != null) {
            Text(if (state.companyMatchesOnly) "Show all contacts" else "Show matching company contacts")
        }
        OutlinedTextField(state.contactSearch, onContactSearch, label = { Text("Search contacts by name, company, or email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Company matches are shown first. Search narrows the list (up to 60 displayed). Select recipients manually; no message is sent automatically.", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            visibleContacts.forEach { contact ->
                FilterChip(
                    selected = contact.id in state.contactIds,
                    onClick = { onContact(contact.id) },
                    label = { Text(contact.fullName) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        ChoiceMenu("Choose template", state.templates.map { it.id to it.name }, state.templateId, onTemplate)
        OutlinedButton(onClick = onGenerateAi, enabled = state.jobId != null && !state.isGenerating, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isGenerating) "Generating AI draft…" else "Draft referral email with AI")
        }
        Text("Gemini receives the selected job and your saved profile. Contact-specific values are inserted locally in each preview.", style = MaterialTheme.typography.bodySmall)
        ChoiceMenu("Optional resume", listOf("" to "No resume") + state.resumes.map { it.id to it.displayName }, state.resumeId.orEmpty()) {
            onResume(it?.ifBlank { null })
        }
        if (state.resumeId != null) Text("The selected PDF will be attached to each email, and the draft will say it is attached.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(state.senderName, onSender, label = { Text("Your name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(state.subject, onSubject, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(state.body, onBody, label = { Text("Message body") }, modifier = Modifier.fillMaxWidth(), minLines = 5)
        Text("Per-recipient preview", style = MaterialTheme.typography.titleLarge)
        if (state.previews.isEmpty()) Text("Choose a job, template and at least one contact to preview.")
        state.previews.forEach { preview ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("To: ${preview.contact.fullName} <${preview.contact.normalizedEmail}>", style = MaterialTheme.typography.titleSmall)
                    when (val result = preview.result) {
                        is TemplateRenderResult.Rendered -> {
                            Text(result.subject, style = MaterialTheme.typography.titleMedium)
                            Text(result.body)
                        }
                        is TemplateRenderResult.Blocked -> Text(
                            "Cannot save: missing ${result.missingValues.joinToString().ifBlank { "none" }}; unknown ${result.unknownPlaceholders.joinToString().ifBlank { "none" }}",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(
            onClick = { confirmSave = true },
            enabled = !state.isSaving && state.previews.isNotEmpty() && state.subject.isNotBlank() && state.body.isNotBlank() && state.previews.all { it.result is TemplateRenderResult.Rendered },
        ) { Text(if (state.isSaving) "Saving…" else "Review and save drafts") }
        Text("Outreach history and drafts (${state.drafts.size})", style = MaterialTheme.typography.titleLarge)
        val activeJobId = state.jobId
        val readyForJob = state.drafts.count { it.jobId == activeJobId && it.status == "DRAFT" && it.recipientEmail.isNotBlank() }
        if (activeJobId != null && readyForJob > 0) {
            OutlinedButton(
                onClick = { batchJobTarget = activeJobId },
                enabled = !state.isSendingBatch && state.sendingDraftId == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (state.isSendingBatch) "Sending separate emails…" else "Send all ready emails for this job ($readyForJob)") }
        }
        state.drafts.take(100).forEach { draft ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${draft.recipientName} · ${draft.jobTitle.orEmpty()} · ${draft.subject}")
                    Text("${draft.recipientEmail} · ${draft.status}", style = MaterialTheme.typography.bodySmall)
                    if (draft.status == "DRAFT") Text(draft.body, maxLines = 4, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    if (draft.status == "SENT" && draft.nextFollowUpAt != null && draft.nextFollowUpAt <= java.time.Instant.now()) {
                        Text("Follow-up due", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { onPrepareFollowUp(draft.id) }) { Text("Prepare follow-up") }
                    }
                    if (draft.messageKind == "FOLLOW_UP") Text("Follow-up ${draft.followUpCount}", style = MaterialTheme.typography.labelSmall)
                    if (draft.status != "SENT") {
                        OutlinedButton(
                            onClick = { sendTarget = draft },
                            enabled = state.sendingDraftId == null && !state.isSendingBatch,
                        ) { Text(if (state.sendingDraftId == draft.id) "Sending…" else if (draft.status == "UNKNOWN") "Review and retry" else "Send this recipient") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceMenu(
    label: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${current ?: "Not selected"}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Not selected") }, onClick = { onSelected(null); expanded = false })
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
            }
        }
    }
}
