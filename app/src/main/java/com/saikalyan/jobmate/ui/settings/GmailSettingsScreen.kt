package com.saikalyan.jobmate.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GmailSettingsRoute(viewModel: GmailSettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GmailSettingsScreen(state, viewModel::setEmail, viewModel::setPassword, viewModel::save, viewModel::testConnection, viewModel::disconnect)
}

@Composable
fun GmailSettingsScreen(
    state: GmailSettingsUiState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Gmail SMTP", style = MaterialTheme.typography.headlineMedium)
        Text("Use a Gmail App Password. A regular Google password is not accepted. App Password availability depends on account and Workspace policy.")
        Text("Status: ${state.connectedEmail ?: "Not connected"}")
        OutlinedTextField(state.email, onEmail, label = { Text("Gmail address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            state.passwordInput,
            onPassword,
            label = { Text("Gmail App Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Button(onClick = onSave, enabled = !state.busy) { Text("Save App Password") }
        OutlinedButton(onClick = onTest, enabled = !state.busy && state.connectedEmail != null) { Text("Test connection (does not send)") }
        if (state.connectedEmail != null) OutlinedButton(onClick = onDisconnect, enabled = !state.busy) { Text("Disconnect and clear credentials") }
        if (state.busy) CircularProgressIndicator()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
