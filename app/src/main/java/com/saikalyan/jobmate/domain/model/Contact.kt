package com.saikalyan.jobmate.domain.model

import java.time.Instant

/** Platform-independent contact with an email address normalized by the domain boundary. */
data class Contact(
    val id: String,
    val fullName: String,
    val normalizedEmail: String,
    val companyName: String? = null,
    val domain: String? = null,
    val designation: String? = null,
    val notes: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)