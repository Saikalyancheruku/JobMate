package com.saikalyan.jobmate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.remote.GmailSmtpClient
import com.saikalyan.jobmate.data.remote.SmtpFailure
import com.saikalyan.jobmate.data.remote.SmtpResult
import com.saikalyan.jobmate.data.secure.SecureCredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GmailSettingsUiState(
    val email: String = "",
    val passwordInput: String = "",
    val connectedEmail: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class GmailSettingsViewModel @Inject constructor(
    private val store: SecureCredentialStore,
    private val smtp: GmailSmtpClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GmailSettingsUiState())
    val uiState: StateFlow<GmailSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { store.configuredEmail.collect { email -> _uiState.value = _uiState.value.copy(connectedEmail = email) } }
    }

    fun setEmail(value: String) { _uiState.value = _uiState.value.copy(email = value, error = null) }
    fun setPassword(value: String) { _uiState.value = _uiState.value.copy(passwordInput = value, error = null) }

    fun save() {
        val current = _uiState.value
        if (!current.email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) || current.passwordInput.isBlank()) {
            _uiState.value = current.copy(error = "Enter a valid Gmail address and App Password.")
            return
        }
        _uiState.value = current.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                store.save(current.email, current.passwordInput)
                _uiState.value = _uiState.value.copy(passwordInput = "", busy = false, message = "App Password encrypted and saved on this device.")
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = "Could not secure these credentials. Try again.")
            }
        }
    }

    fun testConnection() {
        _uiState.value = _uiState.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            val result = try {
                val credential = store.read() ?: run {
                    _uiState.value = _uiState.value.copy(busy = false, error = "Save an App Password first.")
                    return@launch
                }
                smtp.testConnection(credential)
            } catch (_: Exception) {
                SmtpResult.Failed(SmtpFailure.AUTH_REJECTED)
            }
            _uiState.value = when (result) {
                SmtpResult.Connected -> _uiState.value.copy(busy = false, message = "Gmail SMTP connection and authentication succeeded. No email was sent.")
                is SmtpResult.Failed -> _uiState.value.copy(busy = false, error = result.reason.toSettingsMessage())
                is SmtpResult.Unknown -> _uiState.value.copy(busy = false, error = result.reason.toSettingsMessage())
                SmtpResult.Sent -> _uiState.value.copy(busy = false, error = "Unexpected send result.")
            }
        }
    }

    fun disconnect() {
        _uiState.value = _uiState.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                store.clear()
                _uiState.value = GmailSettingsUiState(message = "Gmail disconnected; local credentials cleared.")
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = "Could not clear stored credentials.")
            }
        }
    }

    private fun SmtpFailure.toSettingsMessage(): String = when (this) {
        SmtpFailure.AUTH_REJECTED -> "Gmail rejected the App Password. Confirm account eligibility and generate a new App Password."
        SmtpFailure.TLS -> "A secure STARTTLS connection could not be established."
        SmtpFailure.NETWORK -> "Could not reach Gmail SMTP. Check your network and try again."
        SmtpFailure.INVALID_RECIPIENT -> "The email address is invalid."
        SmtpFailure.ATTACHMENT -> "The selected PDF attachment is unavailable."
        SmtpFailure.UNKNOWN -> "Gmail connection failed for an unexpected reason."
    }
}
