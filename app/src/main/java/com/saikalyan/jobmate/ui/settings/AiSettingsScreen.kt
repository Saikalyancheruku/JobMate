package com.saikalyan.jobmate.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    modifier: Modifier = Modifier,
) {
    val provider = "Gemini"
    var apiKey by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("AI field correction", style = MaterialTheme.typography.headlineMedium)
        Text("Save your Gemini API key once. JobMate will use it only when you request correction for a captured job.")
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
        Text("Your key is stored encrypted on this device. When you tap Correct details, only the saved raw OCR text for that job is sent to Google over HTTPS; screen images are not uploaded. Google says free-tier Gemini API content may be used to improve its products, so avoid sending text you consider sensitive.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AiSettingsRoute(viewModel: AiSettingsViewModel = hiltViewModel()) {
    val provider by viewModel.provider.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    AiSettingsScreen(provider, message, busy, viewModel::save, viewModel::clear)
}
