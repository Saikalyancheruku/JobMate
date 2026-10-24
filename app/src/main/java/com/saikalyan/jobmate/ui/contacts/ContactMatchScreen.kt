package com.saikalyan.jobmate.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.domain.matching.ContactMatchSuggestion

@Composable
fun ContactMatchRoute(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactMatchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(contactId, viewModel) { viewModel.load(contactId) }
    ContactMatchScreen(
        state = state,
        onBack = onBack,
        onConfirm = { viewModel.confirm(contactId, it) },
        onUnlink = { viewModel.unlink(contactId, it) },
    )
}

@Composable
fun ContactMatchScreen(
    state: ContactMatchUiState,
    onBack: () -> Unit,
    onConfirm: (ContactMatchSuggestion) -> Unit,
    onUnlink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Review company matches", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        state.contact?.let { Text("${it.fullName} · ${it.normalizedEmail}") }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.isLoading -> CircularProgressIndicator()
            state.suggestions.isEmpty() -> Text("No conservative company matches were found. No links were created.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.suggestions, key = { it.job.id }) { suggestion ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(suggestion.job.title, style = MaterialTheme.typography.titleMedium)
                            Text(suggestion.job.companyName)
                            Text("${(suggestion.confidence * 100).toInt()}% confidence · ${suggestion.explanation}")
                            if (suggestion.job.id in state.confirmedJobIds) {
                                OutlinedButton(onClick = { onUnlink(suggestion.job.id) }) { Text("Linked · remove") }
                            } else {
                                Button(onClick = { onConfirm(suggestion) }) { Text("Confirm link") }
                            }
                        }
                    }
                }
            }
        }
    }
}
