package com.saikalyan.jobmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "email_templates")
data class EmailTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subjectTemplate: String,
    val bodyTemplate: String,
    val type: String,
    val updatedAt: Instant,
)