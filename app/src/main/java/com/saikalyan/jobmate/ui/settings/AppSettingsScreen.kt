package com.saikalyan.jobmate.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@Composable
fun AppSettingsRoute(viewModel: AppSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var senderName by remember(state.senderName) { mutableStateOf(state.senderName) }
    var linkedInProfileUrl by remember(state.linkedInProfileUrl) { mutableStateOf(state.linkedInProfileUrl) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? -> if (uri != null) viewModel.export(context, uri) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("App settings")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text("Dark theme"); Switch(checked = state.darkTheme, onCheckedChange = viewModel::setDarkTheme) }
        OutlinedTextField(
            value = senderName,
            onValueChange = { senderName = it },
            label = { Text("Your name for email signatures") },
            placeholder = { Text("Your full name") },
            supportingText = { Text("Used in initial and follow-up outreach drafts.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = { viewModel.saveSenderName(senderName) }, enabled = !state.busy) { Text("Save sender name") }
        OutlinedTextField(
            value = linkedInProfileUrl,
            onValueChange = { linkedInProfileUrl = it },
            label = { Text("LinkedIn profile URL") },
            placeholder = { Text("https://www.linkedin.com/in/your-profile") },
            supportingText = { Text("Added below your name in initial and follow-up emails. Leave blank to omit it.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = { viewModel.saveLinkedInProfileUrl(linkedInProfileUrl) }, enabled = !state.busy) { Text("Save LinkedIn URL") }
        Text("Export or delete local JobMate data. Exports are created only when you choose a destination.")
        Button(onClick = { exportLauncher.launch("jobmate-export.csv") }, enabled = !state.busy) { Text("Export structured data (CSV)") }
        Button(onClick = { confirmDelete = true }, enabled = !state.busy) { Text("Delete all local data") }
        state.message?.let { Text(it) }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Delete all JobMate data?") },
        text = { Text("This permanently removes local jobs, contacts, outreach records, templates, resume PDFs, your saved sender name and LinkedIn profile URL, sync settings, the saved Gmail App Password, and any saved AI provider key. Google Sheets data already in Drive remains there.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteAll() }) { Text("Delete everything") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}
