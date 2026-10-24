package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "email_attempts",
    foreignKeys = [
        ForeignKey(
            entity = OutreachEntity::class,
            parentColumns = ["id"],
            childColumns = ["outreachId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["outreachId", "attemptNumber"], unique = true)],
)
data class EmailAttemptEntity(
    @PrimaryKey val id: String,
    val outreachId: String,
    val attemptNumber: Int,
    val startedAt: Instant,
    val endedAt: Instant?,
    /** SENT, FAILED, or UNKNOWN. */
    val outcome: String,
    val sanitizedErrorCode: String?,
    val messageId: String?,
    val userConfirmedRetry: Boolean,
)