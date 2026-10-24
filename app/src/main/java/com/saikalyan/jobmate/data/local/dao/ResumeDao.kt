package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.ResumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<ResumeEntity>>

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ResumeEntity?

    @Upsert
    suspend fun upsert(resume: ResumeEntity)

    @Query("DELETE FROM resumes WHERE id = :id")
    suspend fun deleteById(id: String): Int
}