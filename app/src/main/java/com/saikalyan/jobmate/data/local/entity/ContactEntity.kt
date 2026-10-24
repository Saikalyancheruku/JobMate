package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["normalizedEmail"], unique = true),
        Index(value = ["domain"]),
    ],
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val normalizedEmail: String,
    val companyName: String?,
    val domain: String?,
    val designation: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)