package com.saikalyan.jobmate.domain.repository

import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.model.ContactImportSummary
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(): Flow<RepositoryResult<List<Contact>>>

    fun searchContacts(query: String): Flow<RepositoryResult<List<Contact>>>

    suspend fun findByEmail(normalizedEmail: String): RepositoryResult<Contact?>

    suspend fun save(contact: Contact): RepositoryResult<Contact>

    suspend fun importContacts(contacts: List<Contact>): RepositoryResult<ContactImportSummary>

    suspend fun delete(id: String): RepositoryResult<Unit>
}