package com.saikalyan.jobmate.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.domain.usecase.JobFieldParser
import com.saikalyan.jobmate.service.CaptureSessionController
import com.saikalyan.jobmate.service.CaptureSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

data class CaptureUiState(
    val session: CaptureSessionState = CaptureSessionState.Idle,
    val title: String = "", val company: String = "", val location: String = "",
    val description: String = "", val sourceUrl: String = "", val source: String = "Shared text",
    val skills: String = "", val experience: String = "", val salary: String = "",
    val confidenceNotes: String = "",
    val duplicate: Boolean = false, val saving: Boolean = false, val saved: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val controller: CaptureSessionController,
    private val jobs: JobRepository,
    private val clock: Clock,
) : ViewModel() {
    private val parser = JobFieldParser()
    private val _state = MutableStateFlow(CaptureUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { controller.state.collect { _state.value = _state.value.copy(session = it) } }
        viewModelScope.launch {
            controller.sections.collect { section ->
                val parsed = parser.parse(section.text)
                _state.value = _state.value.copy(
                    title = parsed.title?.value ?: _state.value.title,
                    company = parsed.company?.value ?: _state.value.company,
                    location = parsed.location?.value ?: _state.value.location,
                    description = listOf(_state.value.description, parsed.description).filter(String::isNotBlank).joinToString("\n\n"),
                    sourceUrl = parsed.sourceUrl?.value ?: _state.value.sourceUrl,
                    skills = parsed.skills?.value ?: _state.value.skills,
                    experience = parsed.experience?.value ?: _state.value.experience,
                    salary = parsed.salary?.value ?: _state.value.salary,
                    confidenceNotes = listOfNotNull(parsed.title, parsed.company, parsed.location, parsed.skills, parsed.experience, parsed.salary, parsed.sourceUrl)
                        .filter { it.confidence < 0.75f }.joinToString("\n") { "Review carefully (${(it.confidence * 100).toInt()}% confidence): ${it.evidence}" },
                    message = "Review the extracted fields and correct anything inaccurate.",
                )
            }
        }
    }

    fun update(change: CaptureUiState.() -> CaptureUiState) { _state.value = _state.value.change().copy(duplicate = false, saved = false) }

    fun acceptSharedText(text: String) {
        val parsed = parser.parse(text)
        _state.value = _state.value.copy(
            title = parsed.title?.value.orEmpty(), company = parsed.company?.value.orEmpty(),
            location = parsed.location?.value.orEmpty(), description = parsed.description,
            sourceUrl = parsed.sourceUrl?.value.orEmpty(), source = "Shared text",
            skills = parsed.skills?.value.orEmpty(), experience = parsed.experience?.value.orEmpty(), salary = parsed.salary?.value.orEmpty(),
            confidenceNotes = listOfNotNull(parsed.title, parsed.company, parsed.location, parsed.skills, parsed.experience, parsed.salary, parsed.sourceUrl)
                .filter { it.confidence < 0.75f }.joinToString("\n") { "Review carefully (${(it.confidence * 100).toInt()}% confidence): ${it.evidence}" },
            message = "Shared text is ready. Review and edit the fields before saving.",
        )
    }

    fun save() {
        val s = _state.value
        if (s.title.isBlank() || s.company.isBlank()) { _state.value = s.copy(message = "Enter both a job title and company before saving."); return }
        val normalized = s.sourceUrl.trim().lowercase().ifBlank {
            "${s.title.trim().lowercase()}|${s.company.trim().lowercase()}|${s.location.trim().lowercase()}"
        }
        val fingerprint = "capture:" + MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray()).joinToString("") { "%02x".format(it) }
        _state.value = s.copy(saving = true, message = null)
        viewModelScope.launch {
            when (val existing = jobs.findByFingerprint(fingerprint)) {
                is RepositoryResult.Success -> if (existing.value != null) {
                    _state.value = _state.value.copy(saving = false, duplicate = true, message = "A matching captured job already exists. Edit the URL or review your saved jobs.")
                    return@launch
                }
                is RepositoryResult.Failure -> { _state.value = _state.value.copy(saving = false, message = "Could not check for an existing job."); return@launch }
            }
            val now = clock.instant()
            val job = Job(UUID.randomUUID().toString(), s.title.trim(), s.company.trim(), s.location.trim().ifBlank { null },
                s.description.trim().ifBlank { null }, skills = s.skills.split(',').map(String::trim).filter(String::isNotBlank),
                experienceMin = Regex("\\d+").find(s.experience)?.value?.toIntOrNull(), salaryText = s.salary.trim().ifBlank { null },
                source = s.source, sourceUrl = s.sourceUrl.trim().ifBlank { null }, status = JobStatus.SAVED,
                captureMethod = "USER_CONSENTED_SCREEN_OCR", fingerprint = fingerprint, createdAt = now, updatedAt = now)
            when (val result = jobs.save(job)) {
                is RepositoryResult.Success -> _state.value = _state.value.copy(saving = false, saved = true, message = "Job saved after your confirmation.")
                is RepositoryResult.Failure -> _state.value = _state.value.copy(saving = false, message = "Could not save this job. Please try again.")
            }
        }
    }
}
