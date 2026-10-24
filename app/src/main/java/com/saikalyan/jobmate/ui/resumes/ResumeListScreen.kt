package com.saikalyan.jobmate.ui.resumes

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.domain.model.Resume
import kotlinx.coroutines.flow.collect

@Composable
fun ResumeListRoute(viewModel: ResumeListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::import) }
    LaunchedEffect(viewModel) {
        viewModel.previewUris.collect { uri ->
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try { context.startActivity(intent) }
            catch (_: ActivityNotFoundException) { Toast.makeText(context, "No PDF viewer is available.", Toast.LENGTH_SHORT).show() }
        }
    }
    ResumeListScreen(
        state = state,
        onImport = { picker.launch(arrayOf("application/pdf")) },
        onPreview = viewModel::preview,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
    )
}

@Composable
fun ResumeListScreen(
    state: ResumeListUiState,
    onImport: () -> Unit,
    onPreview: (Resume) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameTarget by remember { mutableStateOf<Resume?>(null) }
    var deleteTarget by remember { mutableStateOf<Resume?>(null) }
    renameTarget?.let { resume -> RenameResumeDialog(resume, { renameTarget = null }) { onRename(resume.id, it); renameTarget = null } }
    deleteTarget?.let { resume ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete resume?") },
            text = { Text("The private PDF will be removed. Outreach history will remain and its resume reference will be cleared.") },
            confirmButton = { TextButton(onClick = { onDelete(resume.id); deleteTarget = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Resumes", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onImport) { Text("Import PDF") }
        }
        state.message?.let { Text(it) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            state.isLoading -> CircularProgressIndicator()
            state.resumes.isEmpty() -> Text("No resumes saved. Import a PDF up to 10 MB.")
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.resumes, key = Resume::id) { resume ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(resume.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("${resume.originalFileName} · ${resume.sizeBytes / 1024} KB")
                            Text("SHA-256 ${resume.sha256.take(12)}…", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onPreview(resume) }) { Text("Preview") }
                                TextButton(onClick = { renameTarget = resume }) { Text("Rename") }
                                TextButton(onClick = { deleteTarget = resume }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameResumeDialog(resume: Resume, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember(resume.id) { mutableStateOf(resume.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename resume") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
