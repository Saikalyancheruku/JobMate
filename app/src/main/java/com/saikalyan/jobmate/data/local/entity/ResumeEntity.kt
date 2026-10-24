package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val originalFileName: String,
    /** Relative path within the app-private resume directory; never an arbitrary external path. */
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val importedAt: Instant,
)