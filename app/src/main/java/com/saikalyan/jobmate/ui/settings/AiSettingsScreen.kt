package com.saikalyan.jobmate.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Gemini API-key settings. */
@Composable
fun AiSettingsScreen(
    configuredProvider: String?,
    message: String?,
    busy: Boolean,
    onSave: (provider: String, apiKey: String) -> Unit,
    onClear: () -> Unit,
    profile: String,
    onProfileChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val provider = "Gemini"
    var apiKey by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Save your Gemini API key once. JobMate uses it only when you request captured-job correction or an outreach draft.")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Provider: Google Gemini", style = MaterialTheme.typography.titleMedium)
                Text("Model: Gemini 3.5 Flash-Lite", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; notice = null },
                    label = { Text("API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    if (apiKey.isBlank()) notice = "Enter an API key to save."
                    else { onSave(provider, apiKey.trim()); apiKey = ""; notice = null }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save API key") }
                if (busy) CircularProgressIndicator()
                if (configuredProvider != null) {
                    Text("Saved provider: $configuredProvider", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { onClear(); notice = "Saved AI key cleared." }) { Text("Remove saved key") }
                }
                notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your profile for referral emails", style = MaterialTheme.typography.titleMedium)
                Text("Add your experience, skills, and a short professional summary. Gemini uses this with the selected job to draft reusable email text; recipient names are filled in separately on this device.")
                OutlinedTextField(
                    value = profile,
                    onValueChange = onProfileChange,
                    label = { Text("Professional summary and skills") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
                    supportingText = { Text("${profile.length}/8000 characters") },
                )
                Button(onClick = onSaveProfile, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            }
        }
        Text("Your API key and profile are encrypted on this device. When requested, JobMate sends the selected job details and saved profile to Google over HTTPS for a referral email draft; for OCR correction it sends the job's raw OCR text. Screen images and contact data are not sent to Gemini. Google may process submitted text under the terms for your API plan, so do not include information you do not want to share.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AiSettingsRoute(viewModel: AiSettingsViewModel = hiltViewModel()) {
    val provider by viewModel.provider.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    AiSettingsScreen(provider, message, busy, viewModel::save, viewModel::clear, profile, viewModel::setProfile, viewModel::saveProfile)
}
