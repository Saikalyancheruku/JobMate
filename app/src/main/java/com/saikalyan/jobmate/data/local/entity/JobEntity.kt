package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["companyName"]),
        Index(value = ["status"]),
        Index(value = ["fingerprint"]),
    ],
)
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val companyName: String,
    val location: String?,
    val description: String?,
    val skills: List<String>,
    val experienceMin: Int?,
    val experienceMax: Int?,
    val salaryText: String?,
    val employmentType: String?,
    val postedAt: Instant?,
    val source: String?,
    val sourceUrl: String?,
    val status: String,
    val appliedAt: Instant?,
    val followUpAt: Instant?,
    val notes: String?,
    val captureMethod: String?,
    val fingerprint: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val workArrangement: String? = null,
    val mandatorySkills: List<String> = emptyList(),
    val preferredSkills: List<String> = emptyList(),
    val companyRating: String? = null,
    val companyReviewCount: String? = null,
    val applicantCount: Int? = null,
    val postedText: String? = null,
    val vacancyCount: Int? = null,
    val experienceLevel: String? = null,
    val education: String? = null,
    val rawCapturedText: String? = null,
    val capturedAt: Instant? = null,
)
