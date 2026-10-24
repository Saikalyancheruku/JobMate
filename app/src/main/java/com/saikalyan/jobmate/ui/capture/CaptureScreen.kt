package com.saikalyan.jobmate.ui.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
    var overlayPermissionDenied by remember { mutableStateOf(false) }
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) CaptureForegroundService.start(context, result.resultCode, result.data!!)
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) {
            overlayPermissionDenied = false
            showConsentExplanation = true
        } else {
            overlayPermissionDenied = true
        }
    }
    fun continueCaptureSetup() {
        if (Settings.canDrawOverlays(context)) {
            showConsentExplanation = true
        } else {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionDenied = !granted
        continueCaptureSetup()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Capture a job listing")
        Text("Start screen capture once, then switch to Naukri or LinkedIn and scroll manually. Tap the floating CAP bubble on each job listing. JobMate reads that screen and saves a job automatically; continue scrolling and tap again for the next job.")
        Text("OCR can miss details. Captured text is saved with the job, and you can correct its fields later from Jobs. Some apps may block screenshots.")
        Text("A draggable CAP bubble stays visible over other apps. Android requires Display over other apps access for this helper.")
        Text("Capture status: ${when (val session = state.session) {
            CaptureSessionState.Idle -> "Not active"
            CaptureSessionState.Capturing -> "Ready to capture"
            CaptureSessionState.WaitingForFrame -> "Capturing screen"
            CaptureSessionState.Saving -> "Saving job"
            is CaptureSessionState.Saved -> "Saved: ${session.title} — ${session.company}"
            is CaptureSessionState.Error -> session.message
        }}")
        Text("This session: ${state.savedJobsInSession} jobs saved")
        if (notificationPermissionDenied) {
            Text("Notifications are disabled. The on-screen capture bubble will still work while the session is active.")
        }
        if (overlayPermissionDenied) {
            Text("Allow JobMate to display over other apps to use the capture bubble.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else continueCaptureSetup()
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
        text = { Text("Android will ask you to allow JobMate's capture bubble to display over other apps, then show a screen-sharing consent prompt. While viewing a job listing, tap the floating CAP bubble. JobMate captures the visible screen, shows OCR/save progress in the bubble, and saves the job automatically. Repeat on each listing. You can drag the bubble out of the way and edit saved details later from Jobs.") },
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
