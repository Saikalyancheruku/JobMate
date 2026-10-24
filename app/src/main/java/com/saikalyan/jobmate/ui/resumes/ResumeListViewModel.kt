package com.saikalyan.jobmate.ui.resumes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.data.storage.ResumeStorage
import com.saikalyan.jobmate.domain.model.Resume
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.repository.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResumeListUiState(
    val resumes: List<Resume> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class ResumeListViewModel @Inject constructor(
    private val repository: ResumeRepository,
    private val storage: ResumeStorage,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResumeListUiState())
    val uiState: StateFlow<ResumeListUiState> = _uiState.asStateFlow()
    private val _previewUris = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val previewUris = _previewUris.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeResumes().catch { _uiState.value = _uiState.value.copy(isLoading = false, error = "Could not load resumes.") }
                .collect { result ->
                    _uiState.value = when (result) {
                        is RepositoryResult.Success -> _uiState.value.copy(resumes = result.value, isLoading = false, error = null)
                        is RepositoryResult.Failure -> _uiState.value.copy(isLoading = false, error = "Could not load resumes.")
                    }
                }
        }
    }

    fun import(uri: Uri) {
        val fileName = queryDisplayName(uri)
        viewModelScope.launch {
            when (repository.importFromUri(uri.toString(), fileName)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(message = "PDF imported into private storage.")
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Could not import PDF. Confirm it is a valid file no larger than 10 MB.")
            }
        }
    }

    fun preview(resume: Resume) {
        viewModelScope.launch {
            try {
                val file = storage.file(resume.relativePath)
                _previewUris.emit(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(error = "The stored PDF is missing or cannot be opened.")
            }
        }
    }

    fun rename(resumeId: String, displayName: String) {
        viewModelScope.launch {
            when (repository.rename(resumeId, displayName)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(message = "Resume renamed.")
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Enter a display name and try again.")
            }
        }
    }

    fun delete(resumeId: String) {
        viewModelScope.launch {
            when (repository.delete(resumeId)) {
                is RepositoryResult.Success -> _uiState.value = _uiState.value.copy(message = "Resume deleted.")
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(error = "Could not delete the private resume file.")
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Resume.pdf"
    } catch (_: Exception) { "Resume.pdf" }
}


