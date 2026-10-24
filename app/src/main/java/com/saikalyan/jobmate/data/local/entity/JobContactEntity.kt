package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "job_contacts",
    primaryKeys = ["jobId", "contactId"],
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["contactId"])],
)
data class JobContactEntity(
    val jobId: String,
    val contactId: String,
    val matchMethod: String,
    val confidence: Double?,
    val isConfirmed: Boolean,
    val createdAt: Instant,
)