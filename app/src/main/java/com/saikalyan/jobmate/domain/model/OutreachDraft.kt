package com.saikalyan.jobmate.domain.model

import java.time.Instant

data class OutreachDraft(
    val id: String,
    val jobId: String?,
    val contactId: String,
    val templateId: String?,
    val resumeId: String?,
    val recipientName: String,
    val recipientEmail: String,
    val jobTitle: String?,
    val companyName: String?,
    val templateName: String?,
    val resumeDisplayName: String?,
    val subject: String,
    val body: String,
    val status: String = "DRAFT",
    val createdAt: Instant,
    val updatedAt: Instant,
)

