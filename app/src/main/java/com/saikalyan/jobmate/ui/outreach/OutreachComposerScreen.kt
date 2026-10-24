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
        onTemplate = viewModel::selectTemplate,
        onResume = viewModel::selectResume,
        onSender = viewModel::setSenderName,
        onSubject = viewModel::setSubject,
        onBody = viewModel::setBody,
        onSaveDrafts = viewModel::saveDrafts,
        onSendDraft = viewModel::sendDraft,
        onClearUnknownRetry = viewModel::clearUnknownRetry,
    )
}

@Composable
fun OutreachComposerScreen(
    state: OutreachComposerUiState,
    onJob: (String?) -> Unit,
    onContact: (String) -> Unit,
    onTemplate: (String?) -> Unit,
    onResume: (String?) -> Unit,
    onSender: (String) -> Unit,
    onSubject: (String) -> Unit,
    onBody: (String) -> Unit,
    onSaveDrafts: () -> Unit,
    onSendDraft: (String, Boolean) -> Unit,
    onClearUnknownRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSave by remember { mutableStateOf(false) }
    var sendTarget by remember { mutableStateOf<OutreachDraft?>(null) }
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

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Outreach composer", style = MaterialTheme.typography.headlineMedium)
        Text("Preview each recipient separately. Saving creates local drafts; sending always requires an explicit confirmation.")
        ChoiceMenu("Choose job", state.jobs.map { it.id to "${it.title} · ${it.companyName}" }, state.jobId, onJob)
        Text("Recipients", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            state.contacts.forEach { contact ->
                FilterChip(
                    selected = contact.id in state.contactIds,
                    onClick = { onContact(contact.id) },
                    label = { Text(contact.fullName) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        ChoiceMenu("Choose template", state.templates.map { it.id to it.name }, state.templateId, onTemplate)
        ChoiceMenu("Optional resume", listOf("" to "No resume") + state.resumes.map { it.id to it.displayName }, state.resumeId.orEmpty()) {
            onResume(it?.ifBlank { null })
        }
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
            enabled = !state.isSaving && state.previews.isNotEmpty() && state.previews.all { it.result is TemplateRenderResult.Rendered },
        ) { Text(if (state.isSaving) "Saving…" else "Review and save drafts") }
        Text("Outreach history and drafts (${state.drafts.size})", style = MaterialTheme.typography.titleLarge)
        state.drafts.take(20).forEach { draft ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${draft.recipientName} · ${draft.jobTitle.orEmpty()} · ${draft.subject}")
                    Text("${draft.recipientEmail} · ${draft.status}", style = MaterialTheme.typography.bodySmall)
                    if (draft.status != "SENT") {
                        OutlinedButton(
                            onClick = { sendTarget = draft },
                            enabled = state.sendingDraftId == null,
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
