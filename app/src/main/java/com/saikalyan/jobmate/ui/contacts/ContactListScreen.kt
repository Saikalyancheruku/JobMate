package com.saikalyan.jobmate.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.domain.model.Contact

@Composable
fun ContactListRoute(
    onImport: () -> Unit,
    onMatch: (String) -> Unit,
    viewModel: ContactListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContactListScreen(
        state = state,
        onQuery = viewModel::setQuery,
        onRetry = viewModel::retry,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onImport = onImport,
        onMatch = onMatch,
    )
}

@Composable
fun ContactListScreen(
    state: ContactListUiState,
    onQuery: (String) -> Unit,
    onRetry: () -> Unit,
    onSave: (Contact?, String, String, String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    onImport: () -> Unit,
    onMatch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorContact by remember { mutableStateOf<Contact?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Contact?>(null) }
    if (adding || editorContact != null) {
        ContactEditorDialog(
            contact = editorContact,
            onDismiss = { adding = false; editorContact = null },
            onSave = { name, email, company, designation, notes ->
                onSave(editorContact, name, email, company, designation, notes)
                adding = false
                editorContact = null
            },
        )
    }
    deleteTarget?.let { contact ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete contact?") },
            text = { Text("${contact.fullName} will be removed. Existing outreach history is retained.") },
            confirmButton = {
                TextButton(onClick = { onDelete(contact.id); deleteTarget = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Contacts", style = MaterialTheme.typography.headlineMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onImport) { Text("Import") }
                Button(onClick = { adding = true }) { Text("Add") }
            }
        }
        OutlinedTextField(state.query, onQuery, label = { Text("Search contacts") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        state.message?.let { Text(it) }
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Column {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Retry") }
            }
            state.contacts.isEmpty() -> Text("No contacts yet. Add one or import a CSV/XLSX file.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.contacts, key = Contact::id) { contact ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(contact.fullName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable { editorContact = contact })
                            Text(contact.normalizedEmail)
                            contact.companyName?.let { Text(it) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editorContact = contact }) { Text("Edit") }
                                TextButton(onClick = { onMatch(contact.id) }) { Text("Match jobs") }
                                TextButton(onClick = { deleteTarget = contact }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactEditorDialog(
    contact: Contact?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    var name by remember(contact?.id) { mutableStateOf(contact?.fullName.orEmpty()) }
    var email by remember(contact?.id) { mutableStateOf(contact?.normalizedEmail.orEmpty()) }
    var company by remember(contact?.id) { mutableStateOf(contact?.companyName.orEmpty()) }
    var designation by remember(contact?.id) { mutableStateOf(contact?.designation.orEmpty()) }
    var notes by remember(contact?.id) { mutableStateOf(contact?.notes.orEmpty()) }
    var error by remember(contact?.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (contact == null) "Add contact" else "Edit contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name *") }, singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("Email *") }, singleLine = true)
                OutlinedTextField(company, { company = it }, label = { Text("Company") }, singleLine = true)
                OutlinedTextField(designation, { designation = it }, label = { Text("Designation") }, singleLine = true)
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() || !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
                    error = "Enter a name and valid email address."
                } else onSave(name, email, company, designation, notes)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}


