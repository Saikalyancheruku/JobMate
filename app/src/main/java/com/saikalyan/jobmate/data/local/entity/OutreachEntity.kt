package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "outreach",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = EmailTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ResumeEntity::class,
            parentColumns = ["id"],
            childColumns = ["resumeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["jobId"]),
        Index(value = ["contactId"]),
        Index(value = ["templateId"]),
        Index(value = ["resumeId"]),
        Index(value = ["status", "sentAt"]),
    ],
)
data class OutreachEntity(
    @PrimaryKey val id: String,
    val jobId: String?,
    val contactId: String?,
    val templateId: String?,
    val resumeId: String?,
    val recipientNameSnapshot: String?,
    val recipientEmailSnapshot: String?,
    val jobTitleSnapshot: String?,
    val companyNameSnapshot: String?,
    val templateNameSnapshot: String?,
    val resumeDisplayNameSnapshot: String?,
    val renderedSubject: String,
    val renderedBody: String,
    val status: String,
    val sentAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)