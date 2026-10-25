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
    val description: String = "", val sourceUrl: String = "", val source: String = "Screen capture",
    val skills: String = "", val experience: String = "", val salary: String = "",
    val confidenceNotes: String = "",
    val capturedSectionsInSession: Int = 0, val savedJobsInSession: Int = 0,
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
    private var activeCaptureJob: Job? = null
    private val _state = MutableStateFlow(CaptureUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { controller.state.collect { _state.value = _state.value.copy(session = it) } }
        viewModelScope.launch {
            controller.newJobRequests.collect {
                activeCaptureJob = null
                _state.value = _state.value.nextCaptureDraft().copy(message = "New job started. Capture its pages; tap NEXT before moving to another listing.")
            }
        }
        viewModelScope.launch {
            controller.sections.collect { section ->
                val parsed = parser.parse(section.screen)
                val current = _state.value
                val draft = if (activeCaptureJob == null) current.nextCaptureDraft() else current
                _state.value = draft.copy(
                    title = parsed.title?.value ?: draft.title,
                    company = parsed.company?.value ?: draft.company,
                    location = parsed.location?.value ?: draft.location,
                    description = listOf(draft.description, parsed.description).filter(String::isNotBlank).joinToString("\n\n"),
                    sourceUrl = parsed.sourceUrl?.value ?: draft.sourceUrl,
                    source = parsed.source?.value ?: "Screen capture",
                    skills = parsed.skills?.value ?: draft.skills,
                    experience = parsed.experience?.value ?: draft.experience,
                    salary = parsed.salary?.value ?: draft.salary,
                    capturedSectionsInSession = draft.capturedSectionsInSession + 1,
                    confidenceNotes = listOfNotNull(parsed.title, parsed.company, parsed.location, parsed.skills, parsed.experience, parsed.salary, parsed.sourceUrl)
                        .filter { it.confidence < 0.75f }.joinToString("\n") { "Review carefully (${(it.confidence * 100).toInt()}% confidence): ${it.evidence}" },
                    saving = true,
                    message = "Capture received. Saving this job automatically…",
                )
                saveCapturedSection(parsed, parsed.description, section.screen.text, section.capturedAt)
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
        if (s.title.isBlank() || s.company.isBlank()) {
            _state.value = s.copy(message = "Enter both a job title and company before saving.")
            return
        }
        val normalized = s.sourceUrl.trim().lowercase().ifBlank {
            "${s.title.trim().lowercase()}|${s.company.trim().lowercase()}|${s.location.trim().lowercase()}"
        }
        val fingerprint = "capture:" + MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray()).joinToString("") { "%02x".format(it) }
        _state.value = s.copy(saving = true, message = null)
        viewModelScope.launch {
            when (val existing = jobs.findByFingerprint(fingerprint)) {
                is RepositoryResult.Success -> if (existing.value != null) {
                    _state.value = _state.value.copy(saving = false, duplicate = true, message = "A matching job is already saved.")
                    return@launch
                }
                is RepositoryResult.Failure -> {
                    _state.value = _state.value.copy(saving = false, message = "Could not check for an existing job.")
                    return@launch
                }
            }
            val now = clock.instant()
            val (experienceMin, experienceMax) = parseExperienceRange(s.experience)
            val job = Job(
                id = UUID.randomUUID().toString(), title = s.title.trim(), companyName = s.company.trim(),
                location = s.location.trim().ifBlank { null }, description = s.description.trim().ifBlank { null },
                skills = s.skills.split(',').map(String::trim).filter(String::isNotBlank),
                experienceMin = experienceMin,
                experienceMax = experienceMax,
                salaryText = s.salary.trim().ifBlank { null }, source = s.source,
                sourceUrl = s.sourceUrl.trim().ifBlank { null }, status = JobStatus.SAVED,
                captureMethod = "USER_CONSENTED_SCREEN_OCR", fingerprint = fingerprint, createdAt = now, updatedAt = now,
            )
            when (jobs.save(job)) {
                is RepositoryResult.Success -> _state.value = _state.value.copy(saving = false, saved = true, message = "Job saved.")
                is RepositoryResult.Failure -> _state.value = _state.value.copy(saving = false, message = "Could not save this job. Please try again.")
            }
        }
    }

    private suspend fun saveCapturedSection(
        parsed: com.saikalyan.jobmate.domain.usecase.ParsedJobFields,
        capturedText: String,
        rawCapturedText: String,
        capturedAt: java.time.Instant,
    ) {
        val now = clock.instant()
        val active = activeCaptureJob
        var isNewJob = active == null
        val baseJob = active ?: run {
            val title = parsed.title?.value?.trim().takeUnless { it.isNullOrBlank() }
                ?: "Captured job ${now.toString().take(16).replace('T', ' ')}"
            val company = parsed.company?.value?.trim().takeUnless { it.isNullOrBlank() }
                ?: "Company not detected"
            val normalized = parsed.sourceUrl?.value?.trim()?.lowercase().takeUnless { it.isNullOrBlank() }
                ?: capturedText.lineSequence().map(String::trim).filter(String::isNotBlank).joinToString(" ").lowercase()
                    .ifBlank { "${title.lowercase()}|${company.lowercase()}|${parsed.location?.value.orEmpty().trim().lowercase()}" }
            val fingerprint = "capture:" + MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray()).joinToString("") { "%02x".format(it) }
            when (val existing = jobs.findByFingerprint(fingerprint)) {
                is RepositoryResult.Success -> existing.value?.also { isNewJob = false } ?: Job(
                    id = UUID.randomUUID().toString(), title = title, companyName = company,
                    status = JobStatus.SAVED, source = parsed.source?.value ?: "Screen capture",
                    sourceUrl = parsed.sourceUrl?.value?.trim()?.ifBlank { null },
                    captureMethod = "USER_CONSENTED_SCREEN_OCR", fingerprint = fingerprint,
                    createdAt = now, updatedAt = now,
                )
                is RepositoryResult.Failure -> {
                    _state.value = _state.value.copy(saving = false, message = "Could not check whether this captured job already exists.")
                    controller.failed("Could not check whether this captured job already exists.")
                    return
                }
            }
        }
        val pageText = capturedText.trim()
        val previousDescription = baseJob.description.orEmpty()
        val mergedDescription = when {
            pageText.isBlank() -> baseJob.description
            previousDescription.isBlank() -> pageText
            normalizeCapturedText(previousDescription).contains(normalizeCapturedText(pageText)) -> previousDescription
            else -> "$previousDescription\n\n$pageText"
        }
        val parsedTitle = parsed.title?.value?.trim().orEmpty()
        val parsedCompany = parsed.company?.value?.trim().orEmpty()
        val mergedSkills = (baseJob.skills + parsed.skills?.value.orEmpty().split(',').map(String::trim))
            .filter(String::isNotBlank).distinctBy { it.lowercase() }
        val mergedRawText = listOf(baseJob.rawCapturedText.orEmpty(), rawCapturedText).filter(String::isNotBlank).joinToString("\n\n")
        val job = baseJob.copy(
            title = if (baseJob.title.startsWith("Captured job ") && parsedTitle.isNotBlank()) parsedTitle else baseJob.title,
            companyName = if (baseJob.companyName == "Company not detected" && parsedCompany.isNotBlank()) parsedCompany else baseJob.companyName,
            location = baseJob.location ?: parsed.location?.value?.trim()?.ifBlank { null },
            workArrangement = baseJob.workArrangement ?: parsed.workArrangement?.value?.trim()?.ifBlank { null },
            description = mergedDescription,
            skills = mergedSkills,
            mandatorySkills = (baseJob.mandatorySkills + parsed.mandatorySkills?.value.orEmpty().split(',').map(String::trim)).filter(String::isNotBlank).distinctBy { it.lowercase() },
            preferredSkills = (baseJob.preferredSkills + parsed.preferredSkills?.value.orEmpty().split(',').map(String::trim)).filter(String::isNotBlank).distinctBy { it.lowercase() },
            employmentType = baseJob.employmentType ?: parsed.employmentType?.value?.trim()?.ifBlank { null },
            companyRating = baseJob.companyRating ?: parsed.companyRating?.value?.trim()?.ifBlank { null },
            companyReviewCount = baseJob.companyReviewCount ?: parsed.companyReviewCount?.value?.trim()?.ifBlank { null },
            applicantCount = baseJob.applicantCount ?: parsed.applicantCount?.value?.toIntOrNull(),
            postedText = baseJob.postedText ?: parsed.postedText?.value?.trim()?.ifBlank { null },
            vacancyCount = baseJob.vacancyCount ?: parsed.vacancyCount?.value?.toIntOrNull(),
            experienceLevel = baseJob.experienceLevel ?: parsed.experienceLevel?.value?.trim()?.ifBlank { null },
            education = baseJob.education ?: parsed.education?.value?.trim()?.ifBlank { null },
            rawCapturedText = mergedRawText,
            capturedAt = baseJob.capturedAt ?: capturedAt,
            experienceMin = baseJob.experienceMin ?: parsed.experienceMin,
            experienceMax = baseJob.experienceMax ?: parsed.experienceMax,
            salaryText = baseJob.salaryText ?: parsed.salary?.value?.trim()?.ifBlank { null },
            source = if (baseJob.source == "Screen capture") parsed.source?.value ?: baseJob.source else baseJob.source,
            sourceUrl = baseJob.sourceUrl ?: parsed.sourceUrl?.value?.trim()?.ifBlank { null },
            updatedAt = now,
        )
        activeCaptureJob = job
        when (jobs.save(job)) {
            is RepositoryResult.Success -> {
                val latest = _state.value
                _state.value = latest.copy(
                    saving = false,
                    saved = true,
                    savedJobsInSession = latest.savedJobsInSession + (if (isNewJob) 1 else 0),
                    message = if (isNewJob) "Job saved automatically. Capture more pages, or tap NEXT for another job."
                    else "Page added to ${job.title}; the saved job now includes this screen. Tap NEXT before another listing.",
                )
                controller.saved(job.title, job.companyName)
            }
            is RepositoryResult.Failure -> {
                _state.value = _state.value.copy(saving = false, message = "Could not save this capture. Open JobMate to try capturing it again.")
                controller.failed("Could not save this capture. Try capturing it again.")
            }
        }
    }

    private fun normalizeCapturedText(value: String) = value.lineSequence().map(String::trim).filter(String::isNotBlank).joinToString(" ").lowercase()

    private fun parseExperienceRange(value: String): Pair<Int?, Int?> {
        val range = Regex("(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:years?|yrs?)\\b").find(value)
        if (range != null) return range.groupValues[1].toDoubleOrNull()?.toInt() to range.groupValues[2].toDoubleOrNull()?.toInt()
        val single = Regex("(?i)\\b(\\d+(?:\\+)?)(?:\\s*)(?:years?|yrs?)\\b").find(value)
        val years = single?.groupValues?.get(1)?.removeSuffix("+")?.toDoubleOrNull()?.toInt()
        return years to null
    }

    private fun CaptureUiState.nextCaptureDraft() = CaptureUiState(
        session = session,
        source = "Screen capture",
        capturedSectionsInSession = capturedSectionsInSession,
        savedJobsInSession = savedJobsInSession,
    )
}
