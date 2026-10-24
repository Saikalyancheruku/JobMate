package com.saikalyan.jobmate.domain.model

sealed interface TemplateRenderResult {
    data class Rendered(val subject: String, val body: String) : TemplateRenderResult
    data class Blocked(val missingValues: Set<String>, val unknownPlaceholders: Set<String>) : TemplateRenderResult
}
