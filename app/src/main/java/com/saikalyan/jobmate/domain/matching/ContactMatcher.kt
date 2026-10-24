package com.saikalyan.jobmate.domain.matching

import com.saikalyan.jobmate.domain.model.Contact
import com.saikalyan.jobmate.domain.model.Job
import java.util.Locale
import javax.inject.Inject

data class ContactMatchSuggestion(
    val job: Job,
    val confidence: Double,
    val explanation: String,
    val requiresUserConfirmation: Boolean = true,
)

/** Conservative suggestions only. This class never creates a job/contact link. */
class ContactMatcher @Inject constructor() {
    fun suggest(contact: Contact, jobs: List<Job>): List<ContactMatchSuggestion> {
        val contactCompany = normalizeCompany(contact.companyName.orEmpty())
        val domain = (contact.domain ?: contact.normalizedEmail.substringAfter('@', ""))
            .trim().lowercase(Locale.ROOT)
        val companyDomainEvidence = domain.isNotBlank() && domain.substringBefore('.') !in GENERIC_DOMAINS
        return jobs.mapNotNull { job ->
            val jobCompany = normalizeCompany(job.companyName)
            when {
                contactCompany.isNotBlank() && contactCompany == jobCompany ->
                    ContactMatchSuggestion(job, 0.95, "The contact company exactly matches this job's company.")
                companyDomainEvidence && normalizeCompany(domain.substringBefore('.')) == jobCompany ->
                    ContactMatchSuggestion(job, 0.82, "The non-generic email domain matches this job's company name.")
                else -> null
            }
        }.sortedByDescending(ContactMatchSuggestion::confidence)
    }

    private fun normalizeCompany(value: String): String = value.lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() && it !in LEGAL_SUFFIXES }
        .joinToString(" ")

    private companion object {
        val GENERIC_DOMAINS = setOf("gmail", "googlemail", "outlook", "hotmail", "live", "yahoo", "icloud", "aol", "protonmail", "proton")
        val LEGAL_SUFFIXES = setOf("inc", "incorporated", "llc", "ltd", "limited", "corp", "corporation", "company", "co")
    }
}
