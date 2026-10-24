package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sync_conflicts",
    indices = [Index(value = ["entityType", "entityId"])],
)
data class SyncConflictEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    /** Structured snapshots must contain only syncable fields and must never contain credentials. */
    val localSnapshot: String,
    val remoteSnapshot: String,
    val detectedAt: Instant,
    val resolution: String?,
)