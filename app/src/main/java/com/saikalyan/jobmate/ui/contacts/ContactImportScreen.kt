package com.saikalyan.jobmate.ui.contacts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.data.importing.ContactColumnMapping
import com.saikalyan.jobmate.data.importing.ContactImportCandidate

@Composable
fun ContactImportRoute(
    onBack: () -> Unit,
    viewModel: ContactImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::read) }
    ContactImportScreen(
        state = state,
        onPickFile = { picker.launch(arrayOf("text/*", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
        onMappingChange = viewModel::updateMapping,
        onImport = viewModel::importEligibleContacts,
        onBack = onBack,
    )
}

@Composable
fun ContactImportScreen(
    state: ContactImportUiState,
    onPickFile: () -> Unit,
    onMappingChange: (ContactColumnMapping) -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Import contacts", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        Text("Select a CSV or XLSX file. Contacts are reviewed before they are saved; no email is sent.")
        Button(onClick = onPickFile) { Text("Choose file") }
        if (state.isReading || state.isImporting) CircularProgressIndicator()
        state.file?.let { file ->
            Text("${file.fileName} · ${file.rows.size} rows", style = MaterialTheme.typography.titleMedium)
            state.preview?.let { preview ->
                MappingSelector("Name column *", file.headers, preview.mapping.name) { onMappingChange(preview.mapping.copy(name = it)) }
                MappingSelector("Email column *", file.headers, preview.mapping.email) { onMappingChange(preview.mapping.copy(email = it)) }
                MappingSelector("Company column", file.headers, preview.mapping.company) { onMappingChange(preview.mapping.copy(company = it)) }
                MappingSelector("Designation column", file.headers, preview.mapping.designation) { onMappingChange(preview.mapping.copy(designation = it)) }
                MappingSelector("Notes column", file.headers, preview.mapping.notes) { onMappingChange(preview.mapping.copy(notes = it)) }
                Text("${preview.eligibleContacts.size} eligible · ${preview.candidates.count { it.duplicate }} duplicates · ${preview.candidates.count { it.issue != null && !it.duplicate }} invalid")
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(preview.candidates, key = ContactImportCandidate::rowNumber) { candidate ->
                        ContactImportRow(candidate)
                    }
                }
                Button(onClick = onImport, enabled = preview.eligibleContacts.isNotEmpty() && !state.isImporting) {
                    Text("Import eligible contacts")
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun MappingSelector(
    label: String,
    headers: List<String>,
    selected: Int?,
    onSelected: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${selected?.let { headers.getOrNull(it) ?: "Column ${it + 1}" } ?: "Not selected"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Not selected") }, onClick = { onSelected(null); expanded = false })
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text(header.ifBlank { "Column ${index + 1}" }) },
                    onClick = { onSelected(index); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ContactImportRow(candidate: ContactImportCandidate) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val title = candidate.contact?.fullName ?: "Row ${candidate.rowNumber}"
        val email = candidate.contact?.normalizedEmail.orEmpty()
        Text("Row ${candidate.rowNumber}: $title${if (email.isBlank()) "" else " · $email"}")
        candidate.issue?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (candidate.issue == null) Text("Ready to import", style = MaterialTheme.typography.bodySmall)
    }
}
