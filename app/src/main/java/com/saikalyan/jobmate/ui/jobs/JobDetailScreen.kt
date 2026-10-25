package com.saikalyan.jobmate.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), shape = MaterialTheme.shapes.small) {
                    Text(job.status.displayLabel, modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(job.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(job.companyName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                job.location?.let { Text("⌖  $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onEdit) { Text("Edit details") }
            if (job.status != JobStatus.CLOSED) OutlinedButton(onClick = onArchive) { Text("Archive") }
            OutlinedButton(onClick = onRequestDelete) { Text("Delete") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Update status", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                JobStatus.entries.forEach { status ->
                    FilterChip(selected = job.status == status, onClick = { onStatusChange(status) }, label = { Text(status.displayLabel) })
                }
            }
        }
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        job.source?.let { DetailField("Source", it) }
        job.salaryText?.let { DetailField("Salary", it) }
        job.employmentType?.let { DetailField("Employment type", it) }
        job.workArrangement?.let { DetailField("Work arrangement", it) }
        job.companyRating?.let { DetailField("Company rating", it) }
        job.companyReviewCount?.let { DetailField("Company reviews", it) }
        job.applicantCount?.let { DetailField("Applicants", it.toString()) }
        job.postedText?.let { DetailField("Posted", it) }
        job.vacancyCount?.let { DetailField("Vacancies", it.toString()) }
        job.experienceLevel?.let { DetailField("Experience level", it) }
        job.education?.let { DetailField("Education", it) }
        job.appliedAt?.let { DetailField("Applied", it.atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()) }
        job.sourceUrl?.let { DetailField("URL", it) }
        job.experienceMin?.let { DetailField("Experience minimum", "$it ${if (it == 1) "year" else "years"}") }
        job.experienceMax?.let { DetailField("Experience maximum", "$it ${if (it == 1) "year" else "years"}") }
        job.followUpAt?.let { DetailField("Follow-up", it.toString()) }
        job.skills.takeIf { it.isNotEmpty() }?.let { DetailField("Skills", it.joinToString(" · ")) }
        job.mandatorySkills.takeIf { it.isNotEmpty() }?.let { DetailField("Mandatory skills", it.joinToString(" · ")) }
        job.preferredSkills.takeIf { it.isNotEmpty() }?.let { DetailField("Preferred skills", it.joinToString(" · ")) }
        job.description?.let { DetailField("Description", it) }
        job.notes?.let { DetailField("Notes", it) }
        job.captureMethod?.let { DetailField("Capture method", it) }
        job.capturedAt?.let { DetailField("Captured at", it.toString()) }
        job.rawCapturedText?.let { DetailField("Raw captured text", it) }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 10.dp))
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}






