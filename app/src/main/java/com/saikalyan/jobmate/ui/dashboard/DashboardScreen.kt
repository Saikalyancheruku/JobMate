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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 20.dp))
            Text("Your saved opportunities", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("Total jobs", summary.totalJobs, Modifier.weight(1f))
                SummaryCard("Applied", summary.count(JobStatus.APPLIED), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAddJob, modifier = Modifier.weight(1f)) { Text("Add job") }
                OutlinedButton(onClick = onViewJobs, modifier = Modifier.weight(1f)) { Text("View jobs") }
            }
            Spacer(Modifier.height(8.dp))
            Text("By status", style = MaterialTheme.typography.titleLarge)
        }
        items(JobStatus.entries, key = { it.storageValue }) { status ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(status.displayLabel)
                Text(summary.count(status).toString())
            }
        }
        item {
            Text("Recent jobs", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            if (summary.recentJobs.isEmpty()) {
                Text("No jobs saved yet. Add a job to see it here.", modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = onAddJob) { Text("Add your first job") }
            }
        }
        items(summary.recentJobs, key = Job::id) { job ->
            RecentJobCard(job = job, onClick = { onOpenJob(job.id) })
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentJobCard(job: Job, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text(job.companyName, style = MaterialTheme.typography.bodyMedium)
            Text("${job.status.displayLabel}${job.location?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
