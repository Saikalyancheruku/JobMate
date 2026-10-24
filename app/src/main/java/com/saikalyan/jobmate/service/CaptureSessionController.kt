package com.saikalyan.jobmate.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class CapturedTextSection(val text: String)

@Singleton
class CaptureSessionController @Inject constructor() {
    private val _state = MutableStateFlow<CaptureSessionState>(CaptureSessionState.Idle)
    val state = _state.asStateFlow()
    private val _sections = MutableSharedFlow<CapturedTextSection>(extraBufferCapacity = 8)
    val sections = _sections.asSharedFlow()

    fun started() { _state.value = CaptureSessionState.Capturing }
    fun waitingForFrame() { _state.value = CaptureSessionState.WaitingForFrame }
    fun publish(text: String) {
        if (text.isBlank()) _state.value = CaptureSessionState.Error("No readable text was found in this screen section.")
        else {
            _state.value = CaptureSessionState.Saving
            if (!_sections.tryEmit(CapturedTextSection(text))) {
                _state.value = CaptureSessionState.Error("Capture queue is busy. Wait briefly, then try again.")
            }
        }
    }
    fun saved(title: String, company: String) { _state.value = CaptureSessionState.Saved(title, company) }
    fun failed(message: String) { _state.value = CaptureSessionState.Error(message) }
    fun stopped() { _state.value = CaptureSessionState.Idle }
}

