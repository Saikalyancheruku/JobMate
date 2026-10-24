package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.EmailTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailTemplateDao {
    @Query("SELECT * FROM email_templates ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<EmailTemplateEntity>>

    @Query("SELECT * FROM email_templates WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): EmailTemplateEntity?

    @Upsert
    suspend fun upsert(template: EmailTemplateEntity)

    @Query("DELETE FROM email_templates WHERE id = :id")
    suspend fun deleteById(id: String): Int
}