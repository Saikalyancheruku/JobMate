package com.saikalyan.jobmate.ui.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikalyan.jobmate.domain.model.Job
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.domain.repository.JobRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import com.saikalyan.jobmate.worker.FollowUpScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject

data class JobEditorForm(
    val title: String = "",
    val companyName: String = "",
    val location: String = "",
    val description: String = "",
    val source: String = "Manual",
    val sourceUrl: String = "",
    val status: JobStatus = JobStatus.SAVED,
    val experienceMin: String = "",
    val experienceMax: String = "",
    val appliedDate: String = "",
    val followUpDate: String = "",
    val salaryText: String = "",
    val employmentType: String = "",
    val workArrangement: String = "",
    val mandatorySkills: String = "",
    val preferredSkills: String = "",
    val companyRating: String = "",
    val companyReviewCount: String = "",
    val applicantCount: String = "",
    val postedText: String = "",
    val vacancyCount: String = "",
    val experienceLevel: String = "",
    val education: String = "",
    val captureMethod: String? = null,
    val rawCapturedText: String? = null,
    val capturedAt: Instant? = null,
    val notes: String = "",
)

data class JobEditorUiState(
    val form: JobEditorForm = JobEditorForm(),
    val baseline: JobEditorForm? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationMessage: String? = null,
    val saved: Boolean = false,
) {
    val isDirty: Boolean get() = baseline != null && form != baseline
    val canSave: Boolean get() = baseline != null && !isLoading && !isSaving && !saved
}

@HiltViewModel
class JobEditorViewModel @Inject constructor(
    private val repository: JobRepository,
    private val clock: Clock,
    private val followUpScheduler: FollowUpScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(JobEditorUiState(baseline = JobEditorForm()))
    val uiState: StateFlow<JobEditorUiState> = _uiState.asStateFlow()
    private var original: Job? = null

    fun load(jobId: String?) {
        if (jobId == null) {
            original = null
            _uiState.value = JobEditorUiState(baseline = JobEditorForm())
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.observeJob(jobId).first()
            _uiState.value = when (result) {
                is RepositoryResult.Success -> {
                    val job = result.value
                    if (job == null) JobEditorUiState(isLoading = false, errorMessage = "This job no longer exists.")
                    else {
                        original = job
                        val form = job.toForm()
                        JobEditorUiState(form = form, baseline = form)
                    }
                }
                is RepositoryResult.Failure -> JobEditorUiState(isLoading = false, errorMessage = "Could not load this job.")
            }
        }
    }

    fun updateForm(form: JobEditorForm) {
        _uiState.value = _uiState.value.copy(form = form, validationMessage = null, errorMessage = null)
    }

    fun save() {
        val state = _uiState.value
        val form = state.form
        val title = form.title.trim()
        val company = form.companyName.trim()
        if (title.isEmpty() || company.isEmpty()) {
            _uiState.value = state.copy(validationMessage = "Title and company are required.")
            return
        }
        val min = form.experienceMin.toOptionalInt()
        val max = form.experienceMax.toOptionalInt()
        if ((form.experienceMin.isNotBlank() && min == null) || (form.experienceMax.isNotBlank() && max == null)) {
            _uiState.value = state.copy(validationMessage = "Experience must be a whole number of years.")
            return
        }
        val applicantCount = form.applicantCount.toOptionalInt()
        val vacancyCount = form.vacancyCount.toOptionalInt()
        if ((form.applicantCount.isNotBlank() && applicantCount == null) || (form.vacancyCount.isNotBlank() && vacancyCount == null)) {
            _uiState.value = state.copy(validationMessage = "Applicant and vacancy counts must be whole numbers.")
            return
        }
        if ((min != null && min < 0) || (max != null && max < 0) || (min != null && max != null && min > max)) {
            _uiState.value = state.copy(validationMessage = "Experience range is invalid.")
            return
        }
        val appliedAt = parseDate(form.appliedDate)
        if (form.appliedDate.isNotBlank() && appliedAt == null) {
            _uiState.value = state.copy(validationMessage = "Applied date must use YYYY-MM-DD.")
            return
        }
        val followUp = parseDate(form.followUpDate)
        if (form.followUpDate.isNotBlank() && followUp == null) {
            _uiState.value = state.copy(validationMessage = "Follow-up date must use YYYY-MM-DD.")
            return
        }
        val now = clock.instant()
        val previous = original
        val job = Job(
            id = previous?.id ?: UUID.randomUUID().toString(),
            title = title,
            companyName = company,
            location = form.location.trim().ifEmpty { null },
            description = form.description.trim().ifEmpty { null },
            experienceMin = min,
            experienceMax = max,
            salaryText = form.salaryText.trim().ifEmpty { null },
            employmentType = form.employmentType.trim().ifEmpty { null },
            workArrangement = form.workArrangement.trim().ifEmpty { null },
            mandatorySkills = form.mandatorySkills.split(',').map(String::trim).filter(String::isNotBlank),
            preferredSkills = form.preferredSkills.split(',').map(String::trim).filter(String::isNotBlank),
            skills = (previous?.skills.orEmpty() + form.mandatorySkills.split(',').map(String::trim) + form.preferredSkills.split(',').map(String::trim)).filter(String::isNotBlank).distinctBy { it.lowercase() },
            postedAt = previous?.postedAt,
            companyRating = form.companyRating.trim().ifEmpty { null },
            companyReviewCount = form.companyReviewCount.trim().ifEmpty { null },
            applicantCount = applicantCount,
            postedText = form.postedText.trim().ifEmpty { null },
            vacancyCount = vacancyCount,
            experienceLevel = form.experienceLevel.trim().ifEmpty { null },
            education = form.education.trim().ifEmpty { null },
            rawCapturedText = form.rawCapturedText,
            capturedAt = form.capturedAt,
            aiCorrected = previous?.aiCorrected ?: false,
            source = form.source.trim().ifEmpty { null },
            sourceUrl = form.sourceUrl.trim().ifEmpty { null },
            status = form.status,
            appliedAt = appliedAt ?: previous?.appliedAt,
            followUpAt = followUp,
            notes = form.notes.trim().ifEmpty { null },
            captureMethod = form.captureMethod,
            fingerprint = previous?.fingerprint ?: "manual:${UUID.randomUUID()}",
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
        )
        _uiState.value = state.copy(isSaving = true, validationMessage = null, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.save(job)) {
                is RepositoryResult.Success -> {
                    original = result.value
                    followUpScheduler.schedule(result.value)
                    val savedForm = result.value.toForm()
                    _uiState.value = JobEditorUiState(form = savedForm, baseline = savedForm, saved = true)
                }
                is RepositoryResult.Failure -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = result.error.toEditorMessage(),
                )
            }
        }
    }

    private fun String.toOptionalInt(): Int? = if (isBlank()) null else toIntOrNull()

    private fun parseDate(value: String): Instant? = try {
        value.trim().takeIf(String::isNotEmpty)?.let { LocalDate.parse(it).atStartOfDay().toInstant(ZoneOffset.UTC) }
    } catch (_: Exception) {
        null
    }

    private fun Job.toForm() = JobEditorForm(
        title = title,
        companyName = companyName,
        location = location.orEmpty(),
        description = description.orEmpty(),
        source = source.orEmpty(),
        sourceUrl = sourceUrl.orEmpty(),
        status = status,
        experienceMin = experienceMin?.toString().orEmpty(),
        experienceMax = experienceMax?.toString().orEmpty(),
        appliedDate = appliedAt?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty(),
        followUpDate = followUpAt?.atZone(ZoneOffset.UTC)?.toLocalDate()?.toString().orEmpty(),
        salaryText = salaryText.orEmpty(),
        employmentType = employmentType.orEmpty(),
        workArrangement = workArrangement.orEmpty(),
        mandatorySkills = mandatorySkills.joinToString(", "),
        preferredSkills = preferredSkills.joinToString(", "),
        companyRating = companyRating.orEmpty(),
        companyReviewCount = companyReviewCount.orEmpty(),
        applicantCount = applicantCount?.toString().orEmpty(),
        postedText = postedText.orEmpty(),
        vacancyCount = vacancyCount?.toString().orEmpty(),
        experienceLevel = experienceLevel.orEmpty(),
        education = education.orEmpty(),
        captureMethod = captureMethod,
        rawCapturedText = rawCapturedText,
        capturedAt = capturedAt,
        notes = notes.orEmpty(),
    )

    private fun RepositoryError.toEditorMessage(): String = when (this) {
        is RepositoryError.InvalidInput -> "Please review the job details and try again."
        RepositoryError.NotFound -> "This job no longer exists."
        RepositoryError.Conflict -> "This job conflicts with saved data."
        is RepositoryError.CorruptData -> "Saved job data is invalid."
        RepositoryError.Storage, RepositoryError.Unexpected -> "Could not save the job. Try again."
    }
}
