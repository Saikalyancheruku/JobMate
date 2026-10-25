package com.saikalyan.jobmate.domain.usecase

/** OCR text plus its normalized position on the captured screen (0.0–1.0). */
data class RecognizedLine(
    val text: String,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
}

data class RecognizedScreen(val text: String, val lines: List<RecognizedLine>)

/** Parsed values retain the screen line used as evidence and a conservative confidence estimate. */
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
    private enum class Layout { LINKEDIN, NAUKRI, GENERIC }

    fun parse(text: String): ParsedJobFields {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty)
        val values = lines.toList()
        return parse(RecognizedScreen(text, values.mapIndexed { index, value ->
            val top = if (values.isEmpty()) 0f else index.toFloat() / values.size
            RecognizedLine(value, top = top, bottom = (top + 0.025f).coerceAtMost(1f))
        }))
    }

    fun parse(screen: RecognizedScreen): ParsedJobFields {
        val lines = screen.lines.filter { it.text.isNotBlank() }.sortedWith(compareBy<RecognizedLine> { it.top }.thenBy { it.left })
        if (lines.isEmpty()) return ParsedJobFields(description = screen.text.trim())
        val layout = detectLayout(screen.text)
        val headerLimit = when (layout) {
            Layout.LINKEDIN -> 0.56f
            Layout.NAUKRI -> 0.64f
            Layout.GENERIC -> 0.58f
        }
        val headerLines = lines.filter { it.top <= headerLimit }
        val titleLine = chooseTitle(headerLines)
        val titleIndex = titleLine?.let(headerLines::indexOf) ?: -1
        val explicitCompany = findExplicitValue(lines, COMPANY_LABEL)
        val companyLine = explicitCompany ?: chooseCompany(headerLines, titleIndex, titleLine)
        val explicitLocation = findExplicitValue(lines, LOCATION_LABEL)
        val locationLine = explicitLocation ?: chooseLocation(headerLines, companyLine)
        val experienceLine = lines.firstOrNull { EXPERIENCE.matches(it.text) || EXPERIENCE_LABEL.containsMatchIn(it.text) }
        val salaryLine = lines.firstOrNull { SALARY.containsMatchIn(it.text) }
        val skillsCandidate = chooseSkills(lines)
        val url = URL.find(screen.text)?.value?.trimEnd('.', ',', ')', ']')

        fun candidate(line: RecognizedLine?, confidence: Float) =
            line?.text?.trim()?.takeIf(String::isNotBlank)?.let { FieldCandidate(it, confidence, line.text) }

        val titleConfidence = when {
            titleLine == null -> 0f
            titleLine.height >= 0.022f -> 0.90f
            titleLine.top <= 0.40f -> 0.78f
            else -> 0.62f
        }
        return ParsedJobFields(
            title = candidate(titleLine, titleConfidence),
            company = candidate(companyLine, if (explicitCompany != null) 0.96f else if (layout == Layout.GENERIC) 0.66f else 0.82f),
            location = candidate(locationLine, if (explicitLocation != null) 0.96f else 0.78f),
            skills = skillsCandidate,
            experience = candidate(experienceLine, if (EXPERIENCE.matches(experienceLine?.text.orEmpty())) 0.95f else 0.78f),
            salary = candidate(salaryLine, if (SALARY.matches(salaryLine?.text.orEmpty())) 0.92f else 0.78f),
            sourceUrl = url?.let { FieldCandidate(it, 0.99f, it) },
            description = screen.text.trim(),
        )
    }

    private fun detectLayout(text: String): Layout {
        val lower = text.lowercase()
        val naukriSignals = listOf("naukri", "key skills", "lacs p.a", "lakhs p.a", "quick apply", "job highlights")
            .count { lower.contains(it) }
        val linkedInSignals = listOf("linkedin", "easy apply", "about the job", "people you can reach out", "applicants")
            .count { lower.contains(it) }
        return when {
            naukriSignals > linkedInSignals && naukriSignals > 0 -> Layout.NAUKRI
            linkedInSignals > 0 -> Layout.LINKEDIN
            else -> Layout.GENERIC
        }
    }

    private fun chooseTitle(headerLines: List<RecognizedLine>): RecognizedLine? = headerLines
        .filter { line ->
            val value = line.text.trim()
            value.length in 4..110 && !value.contains('@') &&
                !UI_NOISE.containsMatchIn(value) && !METADATA.containsMatchIn(value) &&
                !SECTION_HEADING.matches(value) && !COMPANY_LABEL.containsMatchIn(value) && !LOCATION_LABEL.containsMatchIn(value)
        }
        .maxByOrNull { line ->
            val topPreference = if (line.top in 0.07f..0.48f) 0.20f else 0f
            line.height * 10f + topPreference - line.top * 0.25f + line.text.length.coerceAtMost(70) * 0.001f
        }

    private fun chooseCompany(
        headerLines: List<RecognizedLine>,
        titleIndex: Int,
        titleLine: RecognizedLine?,
    ): RecognizedLine? {
        if (titleLine == null) return null
        if (titleIndex < 0) return null
        return headerLines.drop(titleIndex + 1).take(5).firstOrNull { line ->
            line.top >= titleLine.top && line != titleLine &&
                line.text.length in 2..90 && !UI_NOISE.containsMatchIn(line.text) &&
                !METADATA.containsMatchIn(line.text) && !LOCATION_LIKELY.containsMatchIn(line.text) &&
                !SECTION_HEADING.matches(line.text)
        }
    }

    private fun chooseLocation(
        headerLines: List<RecognizedLine>,
        companyLine: RecognizedLine?,
    ): RecognizedLine? {
        val afterCompany = companyLine?.top ?: 0f
        val followingLines = headerLines.filter { line -> line.top >= afterCompany && line != companyLine }
        return followingLines.take(6).firstOrNull { line ->
            LOCATION_LIKELY.containsMatchIn(line.text) && !METADATA.containsMatchIn(line.text)
        } ?: followingLines.take(4).firstOrNull { line ->
            line.text.contains(',') && line.text.length <= 100 && !METADATA.containsMatchIn(line.text) &&
                !UI_NOISE.containsMatchIn(line.text)
        }
    }

    private fun findExplicitValue(lines: List<RecognizedLine>, label: Regex): RecognizedLine? {
        lines.forEachIndexed { index, line ->
            label.find(line.text)?.let { match ->
                val inlineValue = line.text.substring(match.range.last + 1).trim(':', '|', '-', ' ')
                if (inlineValue.isNotBlank()) return line.copy(text = inlineValue)
                val next = lines.getOrNull(index + 1)
                if (next != null && !UI_NOISE.containsMatchIn(next.text) && !METADATA.containsMatchIn(next.text)) return next
            }
        }
        return null
    }

    private fun chooseSkills(lines: List<RecognizedLine>): FieldCandidate? {
        val headingIndex = lines.indexOfFirst { SKILLS_HEADING.containsMatchIn(it.text) }
        if (headingIndex >= 0) {
            val heading = lines[headingIndex]
            val inline = heading.text.substringAfter(':', "").trim()
            val parts = buildList {
                if (inline.isNotBlank()) add(inline)
                lines.drop(headingIndex + 1).takeWhile { !SECTION_HEADING.matches(it.text) }.take(4)
                    .forEach { add(it.text.trim()) }
            }
            if (parts.isNotEmpty()) return FieldCandidate(parts.joinToString(", "), 0.88f, heading.text)
        }
        val labeled = lines.firstOrNull { line ->
            line.text.matches(Regex("(?i)(key\\s+)?skills?\\s*[:|].+"))
        } ?: return null
        val value = labeled.text.replaceFirst(Regex("(?i)^(?:key\\s+)?skills?\\s*[:|]\\s*"), "").trim()
        return value.takeIf(String::isNotBlank)?.let { FieldCandidate(it, 0.90f, labeled.text) }
    }

    private companion object {
        val COMPANY_LABEL = Regex("(?i)\\b(?:company|employer)\\s*[:|]")
        val LOCATION_LABEL = Regex("(?i)\\b(?:job\\s+)?location\\s*[:|]|\\bbased\\s+in\\s*[:|]")
        val URL = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE)
        val EXPERIENCE = Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:-|–|to)\\s*\\d+(?:\\.\\d+)?\\s*(?:years?|yrs?)\\b|\\b\\d+(?:\\+)?\\s*(?:years?|yrs?)\\b")
        val EXPERIENCE_LABEL = Regex("(?i)(experience|work\\s+exp)")
        val SALARY = Regex("(?i)(salary|compensation|\\bctc\\b|\\blpa\\b|\\blacs?\\b|\\blakhs?\\b|₹|\\$|€|£)")
        val SKILLS_HEADING = Regex("(?i)\\b(?:key\\s+)?skills?\\b")
        val SECTION_HEADING = Regex("(?i)^(about the job|job description|description|key skills|skills|required skills|role|responsibilities|requirements|qualifications|education|about company|company profile|employment type|industry|role category)\\s*:?$")
        val UI_NOISE = Regex("(?i)^(jobs?|search|home|messaging|notifications?|apply(?: now)?|easy apply|quick apply|save(?:d)?|posted|see more|see all|share|follow|promoted|verified|top applicant|job alert)$|\\b(?:applicants?|followers?|connections?)\\b|\\b\\d+\\s+(?:applicants?|followers?)\\b")
        val METADATA = Regex("(?i)\\b(?:experience|work\\s+exp|salary|compensation|ctc|lpa|lacs?|lakhs?|posted|applicants?|job type|employment type|full[- ]time|part[- ]time|contract|internship|hybrid|on[- ]site|onsite|remote|years? of experience)\\b|₹|\\$|€|£")
        val LOCATION_LIKELY = Regex("(?i)\\b(remote|hybrid|on[- ]site|onsite|work from home|india|united states|united kingdom|canada|australia|bengaluru|bangalore|mumbai|pune|hyderabad|chennai|delhi|gurugram|gurgaon|noida|kolkata|ahmedabad|jaipur|karnataka|maharashtra|telangana|tamil nadu|greater [a-z ]+ area)\\b")
    }
}
