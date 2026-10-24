package com.saikalyan.jobmate.data.importing

import com.saikalyan.jobmate.domain.model.Contact
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

 data class ContactColumnMapping(
    val name: Int? = null,
    val email: Int? = null,
    val company: Int? = null,
    val designation: Int? = null,
    val notes: Int? = null,
)

data class ContactImportCandidate(
    val rowNumber: Int,
    val contact: Contact?,
    val issue: String? = null,
    val duplicate: Boolean = false,
)

data class ContactMappedPreview(
    val file: ContactFilePreview,
    val mapping: ContactColumnMapping,
    val candidates: List<ContactImportCandidate>,
) {
    val eligibleContacts: List<Contact> get() = candidates.mapNotNull { if (it.issue == null && !it.duplicate) it.contact else null }
}

class ContactImportMapper @Inject constructor(private val clock: Clock) {
    fun map(
        file: ContactFilePreview,
        mapping: ContactColumnMapping,
        existingEmails: Set<String> = emptySet(),
    ): ContactMappedPreview {
        val seen = mutableSetOf<String>()
        val candidates = file.rows.map { row ->
            val name = row.value(mapping.name).trim()
            val email = row.value(mapping.email).trim().lowercase(Locale.ROOT)
            val company = row.value(mapping.company).trim().ifEmpty { null }
            val designation = row.value(mapping.designation).trim().ifEmpty { null }
            val notes = row.value(mapping.notes).trim().ifEmpty { null }
            val issue = when {
                mapping.name == null || mapping.email == null -> "Choose name and email columns."
                name.isBlank() -> "Name is required."
                email.isBlank() -> "Email is required."
                !EMAIL_REGEX.matches(email) -> "Email address is invalid."
                else -> null
            }
            val duplicate = issue == null && (email in existingEmails || !seen.add(email))
            val contact = if (issue == null && !duplicate) {
                val now = clock.instant()
                Contact(
                    id = UUID.randomUUID().toString(),
                    fullName = name,
                    normalizedEmail = email,
                    companyName = company,
                    domain = email.substringAfter('@'),
                    designation = designation,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now,
                )
            } else null
            ContactImportCandidate(
                rowNumber = row.rowNumber,
                contact = contact,
                issue = issue ?: if (duplicate) "Email already exists in contacts or this file." else null,
                duplicate = duplicate,
            )
        }
        return ContactMappedPreview(file, mapping, candidates)
    }

    private fun RawContactRow.value(index: Int?): String = index?.let(cells::getOrNull).orEmpty()

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
