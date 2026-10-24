package com.saikalyan.jobmate.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.saikalyan.jobmate.data.local.dao.ContactDao
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import com.saikalyan.jobmate.di.IoDispatcher
import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.model.ContactImportSummary
import com.saikalyan.jobmate.domain.repository.ContactRepository
import com.saikalyan.jobmate.domain.repository.RepositoryError
import com.saikalyan.jobmate.domain.repository.RepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val syncQueue: SyncQueueManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContactRepository {
    override fun observeContacts(): Flow<RepositoryResult<List<Contact>>> =
        contactDao.observeAll()
            .map(::mapContacts)
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(storageError(cause)))
            }
            .flowOn(ioDispatcher)

    override fun searchContacts(query: String): Flow<RepositoryResult<List<Contact>>> =
        contactDao.search(query.trim())
            .map(::mapContacts)
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(RepositoryResult.Failure(storageError(cause)))
            }
            .flowOn(ioDispatcher)

    override suspend fun findByEmail(normalizedEmail: String): RepositoryResult<Contact?> {
        val email = normalizeEmail(normalizedEmail)
        if (email.isBlank()) {
            return RepositoryResult.Failure(RepositoryError.InvalidInput("normalizedEmail", "blank"))
        }
        return databaseResult {
            val entity = contactDao.findByNormalizedEmail(email)
            if (entity == null) RepositoryResult.Success(null)
            else entity.toDomain()?.let { RepositoryResult.Success<Contact?>(it) }
                ?: RepositoryResult.Failure(RepositoryError.CorruptData("contact.normalizedEmail"))
        }
    }

    override suspend fun save(contact: Contact): RepositoryResult<Contact> {
        val normalized = normalize(contact)
            ?: return RepositoryResult.Failure(RepositoryError.InvalidInput("contact", "required_field_blank"))
        return databaseResult {
            contactDao.upsert(normalized.toEntity())
            syncQueue.enqueue("CONTACT", normalized.id)
            RepositoryResult.Success(normalized)
        }
    }

    override suspend fun importContacts(contacts: List<Contact>): RepositoryResult<ContactImportSummary> {
        if (contacts.isEmpty()) return RepositoryResult.Success(ContactImportSummary(0, 0))
        val normalized = contacts.map(::normalize)
        if (normalized.any { it == null }) {
            return RepositoryResult.Failure(RepositoryError.InvalidInput("contacts", "required_field_blank"))
        }
        val entities = normalized.filterNotNull().map { it.toEntity() }
        return databaseResult {
            val insertResults = contactDao.importAtomically(entities)
            val imported = insertResults.count { it != -1L }
            insertResults.forEachIndexed { index, result -> if (result != -1L) syncQueue.enqueue("CONTACT", entities[index].id) }
            RepositoryResult.Success(
                ContactImportSummary(
                    importedCount = imported,
                    duplicateCount = insertResults.size - imported,
                ),
            )
        }
    }

    override suspend fun delete(id: String): RepositoryResult<Unit> =
        databaseResult {
            if (contactDao.deleteById(id) == 0) RepositoryResult.Failure(RepositoryError.NotFound)
            else { syncQueue.enqueue("CONTACT", id, SyncQueueManager.DELETE); RepositoryResult.Success(Unit) }
        }

    private fun normalize(contact: Contact): Contact? {
        val name = contact.fullName.trim()
        val email = normalizeEmail(contact.normalizedEmail)
        if (contact.id.isBlank() || name.isBlank() || email.isBlank()) return null
        val domain = contact.domain?.trim()?.lowercase(Locale.ROOT)?.ifEmpty { null }
            ?: email.substringAfter('@', missingDelimiterValue = "").ifEmpty { null }
        return contact.copy(
            fullName = name,
            normalizedEmail = email,
            companyName = contact.companyName?.trim()?.ifEmpty { null },
            domain = domain,
            designation = contact.designation?.trim()?.ifEmpty { null },
            notes = contact.notes?.trim()?.ifEmpty { null },
        )
    }

    private fun normalizeEmail(value: String): String = value.trim().lowercase(Locale.ROOT)

    private fun mapContacts(entities: List<ContactEntity>): RepositoryResult<List<Contact>> {
        val contacts = mutableListOf<Contact>()
        for (entity in entities) {
            contacts += entity.toDomain()
                ?: return RepositoryResult.Failure(RepositoryError.CorruptData("contact.normalizedEmail"))
        }
        return RepositoryResult.Success(contacts)
    }

    private fun ContactEntity.toDomain(): Contact? {
        if (id.isBlank() || fullName.isBlank() || normalizedEmail.isBlank()) return null
        return Contact(id, fullName, normalizedEmail, companyName, domain, designation, notes, createdAt, updatedAt)
    }

    private fun Contact.toEntity() = ContactEntity(
        id = id,
        fullName = fullName,
        normalizedEmail = normalizedEmail,
        companyName = companyName,
        domain = domain,
        designation = designation,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private suspend fun <T> databaseResult(
        operation: suspend () -> RepositoryResult<T>,
    ): RepositoryResult<T> = try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SQLiteConstraintException) {
        RepositoryResult.Failure(RepositoryError.Conflict)
    } catch (failure: Exception) {
        RepositoryResult.Failure(storageError(failure))
    }

    private fun storageError(cause: Throwable): RepositoryError =
        if (cause is SQLiteConstraintException) RepositoryError.Conflict else RepositoryError.Storage
}
