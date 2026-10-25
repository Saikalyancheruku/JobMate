package com.saikalyan.jobmate.service

import com.saikalyan.jobmate.domain.usecase.RecognizedScreen
import com.saikalyan.jobmate.domain.model.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CaptureSessionState {
    data object Idle : CaptureSessionState
    data object Capturing : CaptureSessionState
    data object WaitingForFrame : CaptureSessionState
    data object Saving : CaptureSessionState
    data class Saved(val title: String, val company: String) : CaptureSessionState
    data class Error(val message: String) : CaptureSessionState
}

data class CapturedTextSection(val screen: RecognizedScreen, val capturedAt: Instant)

@Singleton
class CaptureSessionController @Inject constructor() {
    private val _state = MutableStateFlow<CaptureSessionState>(CaptureSessionState.Idle)
    val state = _state.asStateFlow()
    private val _sections = MutableSharedFlow<CapturedTextSection>(extraBufferCapacity = 8)
    val sections = _sections.asSharedFlow()
    private val _jobGeneration = MutableStateFlow(0L)
    val jobGeneration = _jobGeneration.asStateFlow()
    @Volatile private var currentJob: Job? = null

    /** Keeps the active listing across CaptureViewModel recreation while the capture service is alive. */
    fun activeJob(): Job? = currentJob
    fun setActiveJob(job: Job?) { currentJob = job }

    fun started() { _state.value = CaptureSessionState.Capturing }
    fun waitingForFrame() { _state.value = CaptureSessionState.WaitingForFrame }
    fun publish(screen: RecognizedScreen, capturedAt: Instant = Instant.now()) {
        if (screen.text.isBlank()) _state.value = CaptureSessionState.Error("No readable text was found in this screen section.")
        else {
            _state.value = CaptureSessionState.Saving
            if (!_sections.tryEmit(CapturedTextSection(screen, capturedAt))) {
                _state.value = CaptureSessionState.Error("Capture queue is busy. Wait briefly, then try again.")
            }
        }
    }
    fun saved(title: String, company: String) { _state.value = CaptureSessionState.Saved(title, company) }
    fun finishCurrentJob() {
        currentJob = null
        _jobGeneration.value = _jobGeneration.value + 1
    }
    fun failed(message: String) { _state.value = CaptureSessionState.Error(message) }
    fun stopped() { _state.value = CaptureSessionState.Idle }
}

