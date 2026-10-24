package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.JobContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobContactDao {
    @Query("SELECT * FROM job_contacts WHERE jobId = :jobId ORDER BY createdAt")
    fun observeForJob(jobId: String): Flow<List<JobContactEntity>>

    @Query("SELECT * FROM job_contacts WHERE contactId = :contactId ORDER BY createdAt DESC")
    fun observeForContact(contactId: String): Flow<List<JobContactEntity>>

    @Upsert
    suspend fun upsert(link: JobContactEntity)

    @Query("DELETE FROM job_contacts WHERE jobId = :jobId AND contactId = :contactId")
    suspend fun delete(jobId: String, contactId: String): Int
}