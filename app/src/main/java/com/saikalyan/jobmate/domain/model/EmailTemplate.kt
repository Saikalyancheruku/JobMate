package com.saikalyan.jobmate.domain.model

import java.time.Instant

data class EmailTemplate(
    val id: String,
    val name: String,
    val subjectTemplate: String,
    val bodyTemplate: String,
    val isBuiltIn: Boolean,
    val updatedAt: Instant,
)
