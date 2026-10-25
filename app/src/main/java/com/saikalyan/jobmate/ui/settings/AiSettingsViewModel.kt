package com.saikalyan.jobmate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.secure.AiApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSettingsViewModel @Inject constructor(private val store: AiApiKeyStore) : ViewModel() {
    private val _provider = MutableStateFlow<String?>(null)
    val provider = _provider.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    init { viewModelScope.launch { store.provider.collect { _provider.value = it } } }

    fun save(provider: String, key: String) {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            try {
                store.save(provider, key)
                _message.value = "Gemini API key encrypted and saved on this device."
            } catch (_: Exception) {
                _message.value = "Could not save the API key securely. Check the value and try again."
            } finally { _busy.value = false }
        }
    }

    fun clear() {
        viewModelScope.launch {
            try { store.clear(); _message.value = "Saved AI key cleared." }
            catch (_: Exception) { _message.value = "Could not clear the saved AI key." }
        }
    }
}
