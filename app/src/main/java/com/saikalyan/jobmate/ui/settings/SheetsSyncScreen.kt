package com.saikalyan.jobmate.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import com.saikalyan.jobmate.worker.GoogleSheetsSyncWorker
import com.saikalyan.jobmate.data.secure.SheetsSyncPreferences
import org.json.JSONObject

@Composable
fun SheetsSyncRoute(viewModel: SheetsSyncViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDisconnect by remember { mutableStateOf(false) }
    val authLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { authorization -> if (!authorization.accessToken.isNullOrBlank()) viewModel.onAuthorized() }
                .onFailure { viewModel.setError("Google authorization did not complete. Try connecting again.") }
        }
    }
    fun connect() {
        val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(GoogleSheetsSyncWorker.DRIVE_FILE_SCOPE))).build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { authorization ->
                if (authorization.hasResolution()) {
                    val pendingIntent = authorization.pendingIntent
                    if (pendingIntent != null) authLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    else viewModel.setError("Google did not return an authorization prompt.")
                } else if (!authorization.accessToken.isNullOrBlank()) viewModel.onAuthorized()
                else viewModel.setError("Google did not grant access to the sync spreadsheet.")
            }
            .addOnFailureListener { viewModel.setError("Google authorization could not start. Check Google Play services and try again.") }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Google Sheets sync")
        Text("JobMate creates a spreadsheet named JobMate Sync with Jobs and Contacts tabs. Sync includes structured job/contact fields only. App Passwords, outreach drafts, resumes, and screenshots are never uploaded. The app requests the narrow drive.file scope for files it creates.")
        Text("Editing a row in Sheets? Update its updatedAt cell to an ISO-8601 timestamp. Sync uses stable IDs and timestamps to detect concurrent edits.")
        if (!state.connection.enabled) Button(onClick = ::connect) { Text("Connect Google and create spreadsheet") }
        else {
            Text("Sync is enabled")
            state.connection.spreadsheetUrl?.let { url ->
                OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }) { Text("Open JobMate Sync") }
            }
            if (state.connection.authRequired) Button(onClick = ::connect) { Text("Reconnect Google account") }
            OutlinedButton(onClick = viewModel::syncNow) { Text("Sync now") }
            OutlinedButton(onClick = { pendingDisconnect = true }) { Text("Disconnect Google") }
        }
        state.connection.lastError?.let { Text(it) }
        Text("Sync conflicts (${state.conflicts.size})")
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.conflicts, key = { it.id }) { conflict ->
                val local = snapshotLabel(conflict.localSnapshot, conflict.entityType)
                val remote = snapshotLabel(conflict.remoteSnapshot, conflict.entityType)
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${conflict.entityType.lowercase().replaceFirstChar(Char::uppercase)} changed in both places")
                        Text("JobMate: $local")
                        Text("Google Sheet: $remote")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.resolve(conflict, false) }) { Text("Keep JobMate") }
                            OutlinedButton(onClick = { viewModel.resolve(conflict, true) }) { Text("Use Sheet version") }
                        }
                    }
                }
            }
        }
    }
    if (pendingDisconnect) AlertDialog(
        onDismissRequest = { pendingDisconnect = false },
        title = { Text("Disconnect Google Sheets?") },
        text = { Text("This stops future sync and revokes JobMate's Drive file access. The spreadsheet and its current rows remain in your Google Drive.") },
        confirmButton = { TextButton(onClick = {
            pendingDisconnect = false
            val revoke = RevokeAccessRequest.builder().setScopes(listOf(Scope(GoogleSheetsSyncWorker.DRIVE_FILE_SCOPE))).build()
            Identity.getAuthorizationClient(context).revokeAccess(revoke)
                .addOnCompleteListener { viewModel.disconnect() }
        }) { Text("Disconnect") } },
        dismissButton = { TextButton(onClick = { pendingDisconnect = false }) { Text("Cancel") } },
    )
}

private fun snapshotLabel(json: String, type: String): String = runCatching {
    val values = JSONObject(json).getJSONArray("values")
    "${values.optString(2)} · ${if (type == "JOB") values.optString(3) else values.optString(3)}"
}.getOrDefault("Could not read snapshot")
