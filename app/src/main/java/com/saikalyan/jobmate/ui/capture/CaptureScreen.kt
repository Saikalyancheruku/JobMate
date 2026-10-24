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
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) CaptureForegroundService.start(context, result.resultCode, result.data!!)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || Build.VERSION.SDK_INT < 33) showConsentExplanation = true
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Capture a job listing")
        Text("JobMate reads only screen sections you capture after Android grants screen sharing. Switch to the job listing, scroll yourself, and tap Capture this section. Frames are discarded after on-device OCR.")
        Text("Some apps block screenshots, and OCR may be incomplete. Review every field before saving.")
        Text("Capture status: ${state.session}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else showConsentExplanation = true
            }) { Text("Start screen capture") }
            OutlinedButton(onClick = { CaptureForegroundService.captureCurrentSection(context) }) { Text("Capture this section") }
            OutlinedButton(onClick = { CaptureForegroundService.stop(context) }) { Text("Stop") }
        }
        state.message?.let { Text(it) }
        if (state.duplicate) Text("Possible duplicate: check the jobs list before saving another copy.")
        if (state.saved) Text("Saved")
        Text("Extracted fields · confirm and edit before saving")
        if (state.confidenceNotes.isNotBlank()) Text("Lower-confidence suggestions:\n${state.confidenceNotes}")
        CaptureField("Job title *", state.title) { viewModel.update { copy(title = it) } }
        CaptureField("Company *", state.company) { viewModel.update { copy(company = it) } }
        CaptureField("Location", state.location) { viewModel.update { copy(location = it) } }
        CaptureField("Job URL", state.sourceUrl) { viewModel.update { copy(sourceUrl = it) } }
        CaptureField("Skills (comma separated)", state.skills) { viewModel.update { copy(skills = it) } }
        CaptureField("Experience", state.experience) { viewModel.update { copy(experience = it) } }
        CaptureField("Salary", state.salary) { viewModel.update { copy(salary = it) } }
        CaptureField("Description and captured text", state.description, 5) { viewModel.update { copy(description = it) } }
        Button(onClick = viewModel::save, enabled = !state.saving && !state.saved) {
            if (state.saving) CircularProgressIndicator() else Text("Confirm and save job")
        }
    }
    if (showConsentExplanation) AlertDialog(
        onDismissRequest = { showConsentExplanation = false },
        title = { Text("Share your screen with JobMate?") },
        text = { Text("Android will show a system consent prompt. Capture runs only while the visible notification is active, and you can stop it at any time. JobMate will OCR only when you tap Capture this section.") },
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
