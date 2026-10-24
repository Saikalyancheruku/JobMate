package com.saikalyan.jobmate.ui.jobs

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.saikalyan.jobmate.domain.model.JobStatus

@Composable
fun JobEditorScreen(
    state: JobEditorUiState,
    onFormChange: (JobEditorForm) -> Unit,
    onSave: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestLeave = { if (state.isDirty && !state.saved) showDiscardDialog = true else onLeave() }
    BackHandler { requestLeave() }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved job changes will be lost.") },
            confirmButton = { TextButton(onClick = onLeave) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Keep editing") } },
        )
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (state.baseline == JobEditorForm()) "Add job" else "Edit job", style = MaterialTheme.typography.headlineMedium)
        if (state.isLoading) CircularProgressIndicator()
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        EditorField("Job title *", state.form.title) { onFormChange(state.form.copy(title = it)) }
        EditorField("Company *", state.form.companyName) { onFormChange(state.form.copy(companyName = it)) }
        EditorField("Location", state.form.location) { onFormChange(state.form.copy(location = it)) }
        EditorField("Source", state.form.source) { onFormChange(state.form.copy(source = it)) }
        EditorField("Job URL", state.form.sourceUrl) { onFormChange(state.form.copy(sourceUrl = it)) }
        Text("Status", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            JobStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.form.status == status,
                    onClick = { onFormChange(state.form.copy(status = status)) },
                    label = { Text(status.displayLabel) },
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorField("Experience min (years)", state.form.experienceMin, Modifier.weight(1f)) {
                onFormChange(state.form.copy(experienceMin = it))
            }
            EditorField("Experience max (years)", state.form.experienceMax, Modifier.weight(1f)) {
                onFormChange(state.form.copy(experienceMax = it))
            }
        }
        EditorField("Applied date (YYYY-MM-DD)", state.form.appliedDate) { onFormChange(state.form.copy(appliedDate = it)) }
        EditorField("Follow-up date (YYYY-MM-DD)", state.form.followUpDate) {
            onFormChange(state.form.copy(followUpDate = it))
        }
        EditorField("Salary", state.form.salaryText) { onFormChange(state.form.copy(salaryText = it)) }
        EditorField("Employment type", state.form.employmentType) { onFormChange(state.form.copy(employmentType = it)) }
        EditorField("Description", state.form.description, minLines = 3) {
            onFormChange(state.form.copy(description = it))
        }
        EditorField("Notes", state.form.notes, minLines = 3) { onFormChange(state.form.copy(notes = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave, enabled = state.canSave) {
                if (state.isSaving) CircularProgressIndicator()
                else Text("Save")
            }
            OutlinedButton(onClick = requestLeave, enabled = !state.isSaving) { Text("Cancel") }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        minLines = minLines,
    )
}



