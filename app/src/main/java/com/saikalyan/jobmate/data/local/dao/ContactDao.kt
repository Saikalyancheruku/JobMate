package com.saikalyan.jobmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY fullName COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("""
        SELECT * FROM contacts
        WHERE fullName LIKE '%' || :query || '%'
            OR normalizedEmail LIKE '%' || :query || '%'
            OR companyName LIKE '%' || :query || '%'
            OR domain LIKE '%' || :query || '%'
        ORDER BY fullName COLLATE NOCASE, id
    """)
    fun search(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY id")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE normalizedEmail = :email LIMIT 1")
    suspend fun findByNormalizedEmail(email: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE domain = :domain ORDER BY fullName COLLATE NOCASE, id")
    fun observeByDomain(domain: String): Flow<List<ContactEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM contacts WHERE normalizedEmail = :email)")
    suspend fun hasEmail(email: String): Boolean

    @Upsert
    suspend fun upsert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicate(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringDuplicates(contacts: List<ContactEntity>): List<Long>

    @Transaction
    suspend fun importAtomically(contacts: List<ContactEntity>): List<Long> =
        insertAllIgnoringDuplicates(contacts)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
