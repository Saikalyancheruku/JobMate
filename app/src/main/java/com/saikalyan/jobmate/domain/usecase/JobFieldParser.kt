package com.saikalyan.jobmate.domain.usecase

/** Parsed values retain a simple confidence score and source evidence for human review. */
data class FieldCandidate(val value: String, val confidence: Float, val evidence: String)

data class ParsedJobFields(
    val title: FieldCandidate? = null,
    val company: FieldCandidate? = null,
    val location: FieldCandidate? = null,
    val skills: FieldCandidate? = null,
    val experience: FieldCandidate? = null,
    val salary: FieldCandidate? = null,
    val sourceUrl: FieldCandidate? = null,
    val description: String = "",
)

class JobFieldParser {
    fun parse(text: String): ParsedJobFields {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.isEmpty()) return ParsedJobFields()
        val titleLine = lines.firstOrNull { line ->
            line.length in 3..100 && !line.contains('@') && !line.matches(Regex("(?i).*(apply|save job|posted| ago|followers).*"))
        }
        val companySource = lines.firstOrNull { it.matches(Regex("(?i).*(company|at)\\s*[:|]\\s*.+")) }
        val companyLine = companySource?.replaceFirst(Regex("(?i)^.*?(company|at)\\s*[:|]\\s*"), "")?.trim()
        val locationLine = lines.firstOrNull { it.matches(Regex("(?i).*(location|based in)\\s*[:|]\\s*.+")) }
        val expLine = lines.firstOrNull { it.contains(Regex("(?i)(years? of experience|\\b\\d+\\+?\\s*years?\\b)")) }
        val salaryLine = lines.firstOrNull { it.contains(Regex("(?i)(salary|compensation|\\$|₹|€|£)")) }
        val skillsLine = lines.firstOrNull { it.matches(Regex("(?i)skills?\\s*[:|].+")) }
        val urlLine = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE).find(text)?.value
        fun candidate(value: String?, evidence: String?, confidence: Float = 0.55f) =
            value?.takeIf(String::isNotBlank)?.let { FieldCandidate(it, confidence, evidence.orEmpty()) }
        return ParsedJobFields(
            title = candidate(titleLine, titleLine, if (titleLine != null) 0.65f else 0f),
            company = candidate(companyLine, companySource, 0.7f),
            location = candidate(locationLine?.replaceFirst(Regex("(?i)^.*?(location|based in)\\s*[:|]\\s*"), ""), locationLine),
            experience = candidate(expLine, expLine),
            salary = candidate(salaryLine, salaryLine),
            skills = candidate(skillsLine?.replaceFirst(Regex("(?i)^skills?\\s*[:|]\\s*"), ""), skillsLine),
            sourceUrl = candidate(urlLine, urlLine, 0.95f),
            description = lines.joinToString("\n"),
        )
    }
}
