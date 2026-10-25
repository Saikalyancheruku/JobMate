package com.saikalyan.jobmate.ui.templates

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

@Composable
fun TemplateListRoute(viewModel: TemplateListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TemplateListScreen(state, viewModel::save, viewModel::delete)
}

@Composable
fun TemplateListScreen(
    state: TemplateListUiState,
    onSave: (String?, String, String, String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf<EmailTemplate?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<EmailTemplate?>(null) }
    if (adding || selected != null) {
        TemplateEditorDialog(selected, onDismiss = { adding = false; selected = null }) { name, subject, body ->
            onSave(selected?.id, name, subject, body, selected?.isBuiltIn == true)
            adding = false
            selected = null
        }
    }
    deleteTarget?.let { template ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete template?") },
            text = { Text(template.name) },
            confirmButton = { TextButton(onClick = { onDelete(template.id); deleteTarget = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Email templates", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = { adding = true }) { Text("New template") }
        }
        Text("Supported placeholders: {{firstName}}, {{contactName}}, {{contactEmail}}, {{contactCompany}}, {{companyName}}, {{jobTitle}}, {{jobUrl}}, {{senderName}}")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.loading -> CircularProgressIndicator()
            state.templates.isEmpty() -> Text("No templates available.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.templates, key = EmailTemplate::id) { template ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            Text(template.subjectTemplate, style = MaterialTheme.typography.bodyMedium)
                            Text(if (template.isBuiltIn) "Built-in · editable" else "Custom")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selected = template }) { Text("Edit") }
                                if (!template.isBuiltIn) TextButton(onClick = { deleteTarget = template }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateEditorDialog(
    template: EmailTemplate?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember(template?.id) { mutableStateOf(template?.name.orEmpty()) }
    var subject by remember(template?.id) { mutableStateOf(template?.subjectTemplate.orEmpty()) }
    var body by remember(template?.id) { mutableStateOf(template?.bodyTemplate.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "New template" else "Edit template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(subject, { subject = it }, label = { Text("Subject") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Body") }, minLines = 6)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, subject, body) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
