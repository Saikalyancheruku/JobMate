package com.saikalyan.jobmate.domain.usecase

import com.saikalyan.jobmate.domain.model.TemplateRenderResult
import javax.inject.Inject

/** Replaces only documented placeholders; template contents are never evaluated. */
class TemplateRenderer @Inject constructor() {
    fun render(subject: String, body: String, values: Map<String, String?>): TemplateRenderResult {
        val missing = linkedSetOf<String>()
        val unknown = linkedSetOf<String>()
        fun resolve(text: String): String = text.lineSequence().mapNotNull { line ->
            var optionalFieldMissing = false
            val resolved = PLACEHOLDER.replace(line) { match ->
                val name = match.groupValues[1]
                if (name !in ALLOWED) {
                    unknown += name
                    match.value
                } else {
                    val value = values[name]?.trim()
                    if (value.isNullOrEmpty() && name in OPTIONAL) {
                        optionalFieldMissing = true
                        ""
                    } else if (value.isNullOrEmpty()) {
                        missing += name
                        match.value
                    } else value
                }
            }
            // Avoid leaving orphaned labels such as "Job URL:" when an optional URL is unavailable.
            if (optionalFieldMissing && resolved.trim().matches(ORPHANED_OPTIONAL_LABEL)) null else resolved
        }.joinToString("\n")
        val renderedSubject = resolve(subject)
        val renderedBody = resolve(body)
        return if (missing.isNotEmpty() || unknown.isNotEmpty()) {
            TemplateRenderResult.Blocked(missing, unknown)
        } else TemplateRenderResult.Rendered(renderedSubject, renderedBody)
    }

    fun availablePlaceholders(): Set<String> = ALLOWED

    private companion object {
        val PLACEHOLDER = Regex("\\{\\{([^{}]*)\\}\\}")
        val ALLOWED = setOf("firstName", "contactName", "contactEmail", "contactCompany", "companyName", "jobTitle", "jobUrl", "senderName", "linkedInUrl")
        val OPTIONAL = setOf("jobUrl", "linkedInUrl")
        val ORPHANED_OPTIONAL_LABEL = Regex("(?i)(?:job(?:\\s+(?:url|link))?|url|link)\\s*[:：-]?")
    }
}
