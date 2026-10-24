package com.saikalyan.jobmate.ui.jobs

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobStatus

@Composable
fun JobDetailScreen(
    state: JobDetailUiState,
    onEdit: () -> Unit,
    onStatusChange: (JobStatus) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete job?") },
            text = { Text("This removes the job from your local list. Existing outreach history is preserved.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    when (state) {
        JobDetailUiState.Loading -> CircularProgressIndicator(modifier.padding(24.dp))
        is JobDetailUiState.Error -> Column(modifier.fillMaxSize().padding(24.dp)) {
            Text(state.message)
            OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
        }
        is JobDetailUiState.Content -> JobDetails(
            job = state.job,
            actionError = state.actionError,
            onEdit = onEdit,
            onStatusChange = onStatusChange,
            onArchive = onArchive,
            onRequestDelete = { confirmDelete = true },
            modifier = modifier,
        )
    }
}

@Composable
private fun JobDetails(
    job: Job,
    actionError: String?,
    onEdit: () -> Unit,
    onStatusChange: (JobStatus) -> Unit,
    onArchive: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(job.title, style = MaterialTheme.typography.headlineMedium)
        Text(job.companyName, style = MaterialTheme.typography.titleLarge)
        job.location?.let { Text(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit) { Text("Edit") }
            if (job.status != JobStatus.CLOSED) OutlinedButton(onClick = onArchive) { Text("Archive") }
            OutlinedButton(onClick = onRequestDelete) { Text("Delete") }
        }
        Text("Status", style = MaterialTheme.typography.titleMedium)
        Column {
            JobStatus.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { status ->
                        FilterChip(
                            selected = job.status == status,
                            onClick = { onStatusChange(status) },
                            label = { Text(status.displayLabel) },
                        )
                    }
                }
            }
        }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        job.source?.let { DetailField("Source", it) }
        job.salaryText?.let { DetailField("Salary", it) }
        job.employmentType?.let { DetailField("Employment type", it) }
        job.appliedAt?.let { DetailField("Applied", it.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()) }
        job.sourceUrl?.let { DetailField("URL", it) }
        job.experienceMin?.let { DetailField("Experience minimum", "$it years") }
        job.experienceMax?.let { DetailField("Experience maximum", "$it years") }
        job.followUpAt?.let { DetailField("Follow-up", it.toString()) }
        job.description?.let { DetailField("Description", it) }
        job.notes?.let { DetailField("Notes", it) }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}






