package com.saikalyan.jobmate.domain.model

import java.time.Instant

data class Resume(
    val id: String,
    val displayName: String,
    val originalFileName: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val importedAt: Instant,
)
