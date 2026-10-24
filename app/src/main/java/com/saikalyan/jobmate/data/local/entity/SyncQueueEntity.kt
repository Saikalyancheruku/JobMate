package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["state", "queuedAt"])],
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadVersion: Int,
    val queuedAt: Instant,
    val attemptCount: Int,
    val lastErrorCode: String?,
    val state: String,
    val remoteUpdatedAt: Instant?,
)