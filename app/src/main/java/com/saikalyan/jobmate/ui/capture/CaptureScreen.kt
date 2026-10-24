package com.saikalyan.jobmate.ui.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saikalyan.jobmate.service.CaptureForegroundService
import com.saikalyan.jobmate.service.CaptureSessionState

@Composable
fun CaptureRoute(sharedText: String? = null, viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(sharedText) { if (!sharedText.isNullOrBlank()) viewModel.acceptSharedText(sharedText) }
    var showConsentExplanation by remember { mutableStateOf(false) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) CaptureForegroundService.start(context, result.resultCode, result.data!!)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionDenied = !granted
        if (granted || Build.VERSION.SDK_INT < 33) showConsentExplanation = true
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Capture a job listing")
        Text("Start screen capture once, then switch to Naukri or LinkedIn and scroll manually. Pull down notifications and tap Capture screen on each job listing. JobMate reads that screen and saves a job automatically; continue scrolling and tap again for the next job.")
        Text("OCR can miss details. Captured text is saved with the job, and you can correct its fields later from Jobs. Some apps may block screenshots.")
        Text("Capture status: ${state.session}")
        Text("This session: ${state.savedJobsInSession} jobs saved")
        if (notificationPermissionDenied) {
            Text("Allow JobMate notifications to use the Capture screen action while another app is open.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else showConsentExplanation = true
            }) { Text("Start screen capture") }
            OutlinedButton(onClick = { CaptureForegroundService.captureCurrentSection(context) }) { Text("Capture and save this screen") }
            OutlinedButton(onClick = { CaptureForegroundService.stop(context) }) { Text("Stop") }
        }
        if (state.saving) CircularProgressIndicator()
        state.message?.let { Text(it) }
        if (state.saved && state.source != "Shared text") Text("Latest capture saved automatically: ${state.title} — ${state.company}")
        if (state.saved && state.source == "Shared text") Text("Shared job saved: ${state.title} — ${state.company}")
        if (state.source == "Shared text") {
            Text("Shared text · review and save")
            CaptureField("Job title *", state.title) { viewModel.update { copy(title = it) } }
            CaptureField("Company *", state.company) { viewModel.update { copy(company = it) } }
            CaptureField("Location", state.location) { viewModel.update { copy(location = it) } }
            CaptureField("Job URL", state.sourceUrl) { viewModel.update { copy(sourceUrl = it) } }
            CaptureField("Skills (comma separated)", state.skills) { viewModel.update { copy(skills = it) } }
            CaptureField("Experience", state.experience) { viewModel.update { copy(experience = it) } }
            CaptureField("Salary", state.salary) { viewModel.update { copy(salary = it) } }
            CaptureField("Shared text", state.description, 5) { viewModel.update { copy(description = it) } }
            Button(onClick = viewModel::save, enabled = !state.saving && !state.saved) { Text("Confirm and save shared job") }
        }
    }
    if (showConsentExplanation) AlertDialog(
        onDismissRequest = { showConsentExplanation = false },
        title = { Text("Share your screen with JobMate?") },
        text = { Text("Android will show a system consent prompt. Keep JobMate's notification visible. When you are viewing a job in another app, pull down notifications and tap Capture screen. JobMate waits briefly for the shade to close, OCRs the visible screen, and saves the job automatically. Repeat on each listing. You can edit saved details later from Jobs.") },
        confirmButton = { TextButton(onClick = {
            showConsentExplanation = false
            val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(manager.createScreenCaptureIntent())
        }) { Text("Continue to Android consent") } },
        dismissButton = { TextButton(onClick = { showConsentExplanation = false }) { Text("Cancel") } },
    )
}

@Composable
private fun CaptureField(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = minLines)
}
