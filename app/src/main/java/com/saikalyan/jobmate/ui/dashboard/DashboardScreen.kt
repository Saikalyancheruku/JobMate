package com.saikalyan.jobmate.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobDashboardSummary
import com.saikalyan.jobmate.domain.model.JobStatus

@Composable
fun DashboardScreen(
    state: DashboardLoadState,
    onRetry: () -> Unit,
    onAddJob: () -> Unit,
    onViewJobs: () -> Unit,
    onOpenJob: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DashboardLoadState.Loading -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading dashboard" })
            Text("Loading your dashboard", modifier = Modifier.padding(top = 12.dp))
        }
        is DashboardLoadState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
        }
        is DashboardLoadState.Content -> DashboardContent(
            summary = state.summary,
            onAddJob = onAddJob,
            onViewJobs = onViewJobs,
            onOpenJob = onOpenJob,
            modifier = modifier,
        )
    }
}

@Composable
private fun DashboardContent(
    summary: JobDashboardSummary,
    onAddJob: () -> Unit,
    onViewJobs: () -> Unit,
    onOpenJob: (String) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your job search", style = MaterialTheme.typography.headlineMedium)
                Text("A clear view of every opportunity.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep your momentum", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Save a role and keep its details, contacts, and follow-ups together.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onAddJob) { Text("Add a job") }
                        OutlinedButton(onClick = onViewJobs) { Text("Browse jobs") }
                    }
                }
            }
            Text("At a glance", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("All jobs", summary.totalJobs, Modifier.weight(1f))
                SummaryCard("Applied", summary.count(JobStatus.APPLIED), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Interviews", summary.count(JobStatus.INTERVIEW), Modifier.weight(1f))
                SummaryCard("Offers", summary.count(JobStatus.OFFER), Modifier.weight(1f))
            }
            Text("By status", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    JobStatus.entries.forEachIndexed { index, status ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(status.displayLabel, style = MaterialTheme.typography.bodyLarge)
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                Text(summary.count(status).toString(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (index < JobStatus.entries.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
        item {
            Text("Recently saved", style = MaterialTheme.typography.titleLarge)
            if (summary.recentJobs.isEmpty()) {
                Card {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your list is ready when you are", style = MaterialTheme.typography.titleMedium)
                        Text("Add your first opportunity to start tracking your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onAddJob) { Text("Add your first job") }
                    }
                }
            }
        }
        items(summary.recentJobs, key = Job::id) { job ->
            RecentJobCard(job = job, onClick = { onOpenJob(job.id) })
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentJobCard(job: Job, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text(job.companyName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(listOfNotNull(job.location, job.status.displayLabel).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
