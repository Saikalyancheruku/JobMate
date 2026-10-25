package com.saikalyan.jobmate.ui.jobs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
fun JobListScreen(
    state: JobListUiState,
    onSearch: (String) -> Unit,
    onStatusFilter: (JobStatus?) -> Unit,
    onCaptureFilter: (CaptureFilter) -> Unit,
    onRetry: () -> Unit,
    onAddJob: () -> Unit,
    onOpenJob: (String) -> Unit,
    onArchive: (String) -> Unit,
    onCorrect: (String) -> Unit,
    onBeginSelection: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onBulkStatusChange: (JobStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBulkStatusMenu by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Find your next move", style = MaterialTheme.typography.headlineSmall)
                Text("${state.jobs.size} opportunities", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAddJob) { Text("＋ Add") }
        }
        if (state.selectionMode) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.selectedJobIds.size} selected", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = { showBulkStatusMenu = true }, enabled = state.selectedJobIds.isNotEmpty()) { Text("Change status") }
                    DropdownMenu(expanded = showBulkStatusMenu, onDismissRequest = { showBulkStatusMenu = false }) {
                        JobStatus.entries.forEach { status ->
                            DropdownMenuItem(text = { Text(status.displayLabel) }, onClick = {
                                showBulkStatusMenu = false
                                onBulkStatusChange(status)
                            })
                        }
                    }
                    TextButton(onClick = onClearSelection) { Text("Cancel") }
                }
            }
        }
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearch,
            label = { Text("Search title, company, or location") },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = state.statusFilter == null, onClick = { onStatusFilter(null) }, label = { Text("All") })
            JobStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { onStatusFilter(if (state.statusFilter == status) null else status) },
                    label = { Text(status.displayLabel) },
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(CaptureFilter.ALL to "All sources", CaptureFilter.SCRAPED to "Scraped", CaptureFilter.CORRECTED to "Corrected").forEach { (filter, label) ->
                FilterChip(selected = state.captureFilter == filter, onClick = { onCaptureFilter(filter) }, label = { Text(label) })
            }
        }
        if (state.captureFilter == CaptureFilter.CORRECTED) {
            Text("Jobs corrected with Gemini are shown here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!state.selectionMode) TextButton(onClick = onBeginSelection) { Text("Select jobs") }
        }
        state.actionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.errorMessage != null -> Column {
                Text(state.errorMessage)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
            }
            state.jobs.isEmpty() -> Text(
                if (state.searchQuery.isBlank() && state.statusFilter == null && state.captureFilter == CaptureFilter.ALL) "No jobs saved yet. Add a job to get started."
                else "No jobs match this search or filter.",
                modifier = Modifier.padding(vertical = 20.dp),
            )
            else -> LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.jobs, key = Job::id) { job ->
                    JobListCard(
                        job = job,
                        selectionMode = state.selectionMode,
                        selected = job.id in state.selectedJobIds,
                        onSelect = { onToggleSelection(job.id) },
                        onOpen = { onOpenJob(job.id) },
                        onArchive = { onArchive(job.id) },
                        onCorrect = { onCorrect(job.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JobListCard(job: Job, selectionMode: Boolean, selected: Boolean, onSelect: () -> Unit, onOpen: () -> Unit, onArchive: () -> Unit, onCorrect: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = if (selectionMode) onSelect else onOpen), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f)) {
                    if (selectionMode) Checkbox(checked = selected, onCheckedChange = { onSelect() })
                    Text(job.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(top = 12.dp))
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(job.status.displayLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(job.companyName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            job.location?.let { Text("⌖  $it", style = MaterialTheme.typography.bodyMedium) }
            if (!job.rawCapturedText.isNullOrBlank()) {
                Text(if (job.aiCorrected) "Scraped · AI corrected with Gemini" else "Scraped", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                OutlinedButton(onClick = onCorrect, modifier = Modifier.padding(top = 2.dp)) { Text(if (job.aiCorrected) "Correct again with AI" else "Correct details with AI") }
            }
            if (job.status != JobStatus.CLOSED) {
                OutlinedButton(onClick = onArchive, modifier = Modifier.padding(top = 2.dp)) { Text("Archive") }
            }
        }
    }
}
