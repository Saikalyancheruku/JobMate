package com.saikalyan.jobmate.domain.usecase

import com.saikalyan.jobmate.domain.model.TemplateRenderResult
import javax.inject.Inject

/** Replaces only documented placeholders; template contents are never evaluated. */
class TemplateRenderer @Inject constructor() {
    fun render(subject: String, body: String, values: Map<String, String?>): TemplateRenderResult {
        val missing = linkedSetOf<String>()
        val unknown = linkedSetOf<String>()
        fun resolve(text: String): String = PLACEHOLDER.replace(text) { match ->
            val name = match.groupValues[1]
            if (name !in ALLOWED) {
                unknown += name
                match.value
            } else {
                val value = values[name]?.trim()
                if (value.isNullOrEmpty()) {
                    missing += name
                    match.value
                } else value
            }
        }
        val renderedSubject = resolve(subject)
        val renderedBody = resolve(body)
        return if (missing.isNotEmpty() || unknown.isNotEmpty()) {
            TemplateRenderResult.Blocked(missing, unknown)
        } else TemplateRenderResult.Rendered(renderedSubject, renderedBody)
    }

    fun availablePlaceholders(): Set<String> = ALLOWED

    private companion object {
        val PLACEHOLDER = Regex("\\{\\{([^{}]*)}}")
        val ALLOWED = setOf("firstName", "companyName", "jobTitle", "jobUrl", "senderName")
    }
}
