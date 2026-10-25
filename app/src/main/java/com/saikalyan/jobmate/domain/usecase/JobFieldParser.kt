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
    val experienceMin: Int? = null,
    val experienceMax: Int? = null,
    val salary: FieldCandidate? = null,
    val employmentType: FieldCandidate? = null,
    val workArrangement: FieldCandidate? = null,
    val source: FieldCandidate? = null,
    val mandatorySkills: FieldCandidate? = null,
    val preferredSkills: FieldCandidate? = null,
    val companyRating: FieldCandidate? = null,
    val companyReviewCount: FieldCandidate? = null,
    val applicantCount: FieldCandidate? = null,
    val postedText: FieldCandidate? = null,
    val vacancyCount: FieldCandidate? = null,
    val experienceLevel: FieldCandidate? = null,
    val education: FieldCandidate? = null,
    val rawCapturedText: String = "",
    val sourceUrl: FieldCandidate? = null,
    val description: String = "",
)

class JobFieldParser {
    private enum class Layout { LINKEDIN, NAUKRI, GENERIC }

    fun parse(text: String): ParsedJobFields {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty)
        val values = lines.toList()
        return parse(RecognizedScreen(text, values.mapIndexed { index, value ->
            val top = 0.08f + index.toFloat() * 0.84f / values.size.coerceAtLeast(1)
            RecognizedLine(value, top = top, bottom = (top + 0.025f).coerceAtMost(1f))
        }))
    }

    fun parse(screen: RecognizedScreen): ParsedJobFields {
        val lines = screen.lines.filter { it.text.isNotBlank() && !isSystemChrome(it) }
            .sortedWith(compareBy<RecognizedLine> { it.top }.thenBy { it.left })
        if (lines.isEmpty()) return ParsedJobFields(description = cleanDescription(screen), rawCapturedText = screen.text)
        val layout = detectLayout(screen.text)
        val headerLimit = when (layout) {
            Layout.LINKEDIN -> 0.56f
            Layout.NAUKRI -> 0.64f
            Layout.GENERIC -> 0.58f
        }
        val headerLines = lines.filter { it.top <= headerLimit }
        val inlineHeader = findInlineHeader(headerLines)
        val titleLine = inlineHeader?.first ?: chooseTitle(headerLines)
        val titleIndex = titleLine?.let(headerLines::indexOf) ?: -1
        val explicitCompany = findExplicitValue(lines, COMPANY_LABEL)
        val companyLine = explicitCompany ?: inlineHeader?.second ?: when (layout) {
            // LinkedIn's header places the company immediately above the title.
            Layout.LINKEDIN -> chooseCompanyBeforeTitle(headerLines, titleLine)
            // Naukri's detail header places the company immediately below the title.
            Layout.NAUKRI, Layout.GENERIC -> chooseCompany(headerLines, titleIndex, titleLine)
        }
        val explicitLocation = findExplicitValue(lines, LOCATION_LABEL)
        val locationLine = explicitLocation ?: chooseLocation(headerLines, companyLine)
        val workArrangementLine = lines.firstOrNull { WORK_ARRANGEMENT.containsMatchIn(it.text) }
        val experienceLine = lines.firstOrNull { EXPERIENCE.matches(it.text) }
            ?: lines.firstOrNull { EXPERIENCE_LABEL.containsMatchIn(it.text) && EXPERIENCE.containsMatchIn(it.text) }
        val salaryLine = lines.firstOrNull { NOT_DISCLOSED.containsMatchIn(it.text) }
            ?: lines.firstOrNull { SALARY.containsMatchIn(it.text) }
        val employmentLine = lines.firstOrNull { EMPLOYMENT_TYPE.containsMatchIn(it.text) }
        val skillsCandidate = chooseSkills(lines)
        val (mandatorySkills, preferredSkills) = chooseNaukriSkillGroups(lines)
        val reviewLine = lines.firstOrNull { REVIEWS.containsMatchIn(it.text) }
        val ratingText = reviewLine?.let { line ->
            RATING_BEFORE_REVIEWS.find(line.text)?.groupValues?.get(1)
                ?: lines.lastOrNull { previous -> previous.top < line.top && line.top - previous.top <= 0.05f && SOLO_RATING.matches(previous.text.trim()) }?.text?.trim()
        }
        val applicantsLine = lines.firstOrNull { APPLICANTS.containsMatchIn(it.text) }
        val postedLine = lines.firstOrNull { POSTED_TEXT.containsMatchIn(it.text) }
        val vacancyLine = lines.firstOrNull { VACANCIES.containsMatchIn(it.text) }
        val levelLine = lines.firstOrNull { EXPERIENCE_LEVEL.containsMatchIn(it.text) }
        val educationLine = findEducationValue(lines)
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
            company = candidate(companyLine?.let { it.copy(text = cleanCompany(it.text)) }, if (explicitCompany != null) 0.96f else if (layout == Layout.GENERIC) 0.66f else 0.82f),
            location = candidate(locationLine?.let { it.copy(text = cleanLocation(it.text)) }, if (explicitLocation != null) 0.96f else 0.78f),
            skills = skillsCandidate,
            experience = candidate(experienceLine, if (EXPERIENCE.matches(experienceLine?.text.orEmpty())) 0.95f else 0.78f),
            experienceMin = experienceLine?.text?.let(::parseExperience)?.first,
            experienceMax = experienceLine?.text?.let(::parseExperience)?.second,
            salary = candidate(salaryLine?.let { it.copy(text = cleanSalary(it.text)) }, if (SALARY.containsMatchIn(salaryLine?.text.orEmpty())) 0.92f else 0.88f),
            employmentType = candidate(employmentLine, 0.88f),
            workArrangement = candidate(workArrangementLine?.let { it.copy(text = extractWorkArrangement(it.text)) }, 0.88f),
            source = when (layout) {
                Layout.LINKEDIN -> FieldCandidate("LinkedIn", 0.88f, "LinkedIn layout cues")
                Layout.NAUKRI -> FieldCandidate("Naukri", 0.88f, "Naukri layout cues")
                Layout.GENERIC -> null
            },
            mandatorySkills = mandatorySkills,
            preferredSkills = preferredSkills,
            companyRating = ratingText?.let { FieldCandidate(it, 0.82f, reviewLine?.text.orEmpty()) },
            companyReviewCount = reviewLine?.let { line -> REVIEW_COUNT.find(line.text)?.groupValues?.get(1)?.let { FieldCandidate(it.trim(), 0.88f, line.text) } },
            applicantCount = applicantsLine?.let { line -> APPLICANT_COUNT.find(line.text)?.groupValues?.get(1)?.replace(",", "")?.let { FieldCandidate(it, 0.90f, line.text) } },
            postedText = postedLine?.let { line -> POSTED_TEXT.find(line.text)?.value?.let { FieldCandidate(it, 0.86f, line.text) } },
            vacancyCount = vacancyLine?.let { line -> VACANCY_COUNT.find(line.text)?.groupValues?.get(1)?.let { FieldCandidate(it, 0.88f, line.text) } },
            experienceLevel = levelLine?.let { line -> EXPERIENCE_LEVEL.find(line.text)?.value?.let { FieldCandidate(it, 0.86f, line.text) } },
            education = candidate(educationLine, 0.78f),
            rawCapturedText = screen.text,
            sourceUrl = url?.let { FieldCandidate(it, 0.99f, it) },
            description = cleanDescription(screen, titleLine?.text, companyLine?.text),
        )
    }

    private fun detectLayout(text: String): Layout {
        val lower = text.lowercase()
        val naukriSignals = listOf("naukri", "key skills", "must have skills", "good to have skills", "lacs p.a", "lakhs p.a", "quick apply", "job highlights", "job details", "similar roles", "similarroles")
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
                !PROMOTIONAL_NOISE.containsMatchIn(value) &&
                !REVIEWS.containsMatchIn(value) && !APPLICANTS.containsMatchIn(value) && !SOLO_RATING.matches(value) &&
                !SECTION_HEADING.matches(value) && !COMPANY_LABEL.containsMatchIn(value) && !LOCATION_LABEL.containsMatchIn(value)
        }
        .maxByOrNull { line ->
            val topPreference = if (line.top in 0.07f..0.48f) 0.20f else 0f
            line.height * 10f + topPreference - line.top * 0.25f + line.text.length.coerceAtMost(70) * 0.001f
        }

    private fun findInlineHeader(headerLines: List<RecognizedLine>): Pair<RecognizedLine, RecognizedLine>? {
        val pattern = Regex("^(.{4,80}?)\\s+@\\s*([A-Za-z][A-Za-z0-9._-]{1,50})$")
        for (line in headerLines) {
            val match = pattern.matchEntire(line.text.trim()) ?: continue
            val title = match.groupValues[1].trim()
            val company = match.groupValues[2].trim()
            if (title.contains('@') || UI_NOISE.containsMatchIn(title) || PROMOTIONAL_NOISE.containsMatchIn(title)) continue
            return line.copy(text = title) to line.copy(text = company)
        }
        return null
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
                !METADATA.containsMatchIn(line.text) && !PROMOTIONAL_NOISE.containsMatchIn(line.text) && !LOCATION_LIKELY.containsMatchIn(line.text) &&
                (!REVIEWS.containsMatchIn(line.text) || cleanCompany(line.text).trim() != line.text.trim()) &&
                !APPLICANTS.containsMatchIn(line.text) && !SOLO_RATING.matches(line.text.trim()) &&
                !SECTION_HEADING.matches(line.text)
        }
    }

    private fun chooseCompanyBeforeTitle(
        headerLines: List<RecognizedLine>,
        titleLine: RecognizedLine?,
    ): RecognizedLine? {
        if (titleLine == null) return null
        return headerLines.asReversed().firstOrNull { line ->
            line.bottom <= titleLine.top + 0.005f && line.text.trim().length in 3..90 &&
                !UI_NOISE.containsMatchIn(line.text) && !METADATA.containsMatchIn(line.text) &&
                !PROMOTIONAL_NOISE.containsMatchIn(line.text) && !SECTION_HEADING.matches(line.text) && !LOCATION_LIKELY.containsMatchIn(line.text) &&
                (!REVIEWS.containsMatchIn(line.text) || cleanCompany(line.text).trim() != line.text.trim()) &&
                !APPLICANTS.containsMatchIn(line.text) && !SOLO_RATING.matches(line.text.trim())
        }
    }

    private fun chooseLocation(
        headerLines: List<RecognizedLine>,
        companyLine: RecognizedLine?,
    ): RecognizedLine? {
        val afterCompany = companyLine?.top ?: 0f
        val followingLines = headerLines.filter { line -> line.top >= afterCompany && line != companyLine }
        return followingLines.take(6).firstOrNull { line ->
            LOCATION_LIKELY.containsMatchIn(line.text)
        } ?: followingLines.take(4).firstOrNull { line ->
            line.text.contains(',') && line.text.length <= 100 && !METADATA.containsMatchIn(line.text) &&
                !UI_NOISE.containsMatchIn(line.text)
        }
    }

    private fun cleanLocation(value: String): String = value
        .replace(Regex("(?i)\\s*\\((?:weekly|daily|twice|thrice|once|on[- ]site|onsite|hybrid|remote)[^)]*\\)"), "")
        .replace(Regex("(?i)[·•]?\\s*\\d+\\s+(?:days?|weeks?|months?|years?)\\s+ago.*$"), "")
        .replace(Regex("(?i)[·•]?\\s*(?:over|more than)\\s+\\d+\\s+applicants?.*$"), "")
        .replace(Regex("(?i)[·•]?\\s*\\d+\\s+applicants?.*$"), "")
        .trim(' ', '·', '•', '|', '-')

    private fun cleanCompany(value: String): String = value
        .replace(Regex("(?i)[|·•]?\\s*[0-5](?:\\.\\d+)?\\s+[\\d,.]+\\s*[KkMm]?\\s*reviews?.*$"), "")
        .replace(Regex("(?i)[|·•]?\\s*[0-5](?:\\.\\d+)?\\s*$"), "")
        .trim(' ', '·', '•', '|', '-')

    private fun cleanSalary(value: String): String {
        NOT_DISCLOSED.find(value)?.value?.let { return it.trim() }
        return value.replace(Regex("(?i)^\\s*(?:salary|compensation|ctc)\\s*[:|–-]?\\s*"), "").trim()
    }

    private fun extractWorkArrangement(value: String): String =
        WORK_ARRANGEMENT.find(value)?.value?.trim(' ', '(', ')', '.', ',', '·') ?: value.trim()

    private fun cleanDescription(
        screen: RecognizedScreen,
        title: String? = null,
        company: String? = null,
    ): String {
        val sourceLines = screen.lines.filterNot(::isSystemChrome).map { it.text.trim() }.filter(String::isNotBlank)
        val allLines = if (sourceLines.isEmpty()) screen.text.lineSequence().map(String::trim).toList() else sourceLines
        val heading = allLines.indexOfFirst { JOB_DESCRIPTION_HEADING.matches(it) || ABOUT_JOB_HEADING.matches(it) }
        val sectionLines = if (heading >= 0) allLines.drop(heading + 1) else allLines
        val boundary = sectionLines.indexOfFirst { DESCRIPTION_END_HEADING.matches(it) }
        val descriptionLines = if (boundary >= 0) sectionLines.take(boundary) else sectionLines
        val excludedHeaderValues = setOfNotNull(title?.normalizedForMatch(), company?.normalizedForMatch())
        return descriptionLines.filterNot { line ->
            line.normalizedForMatch() in excludedHeaderValues ||
                UI_NOISE.matches(line) || CHROME_NOISE.matches(line) || CAPTURE_CONTROL.matches(line) ||
                TIME_OR_STATUS.matches(line) || NOTIFICATION_NOISE.containsMatchIn(line) || PROMOTIONAL_NOISE.containsMatchIn(line)
        }.distinct().joinToString("\n").trim()
    }

    private fun String.normalizedForMatch(): String = trim().replace(Regex("\\s+"), " ").lowercase()

    private fun isSystemChrome(line: RecognizedLine): Boolean =
        line.top <= 0.045f || TIME_OR_STATUS.matches(line.text.trim()) ||
            (line.top <= 0.18f && NOTIFICATION_NOISE.containsMatchIn(line.text))

    private fun parseExperience(value: String): Pair<Int?, Int?>? {
        EXPERIENCE_RANGE.find(value)?.let { match ->
            return match.groupValues[1].toDoubleOrNull()?.toInt() to match.groupValues[2].toDoubleOrNull()?.toInt()
        }
        EXPERIENCE_SINGLE.find(value)?.let { match ->
            val years = match.groupValues[1].removeSuffix("+").toDoubleOrNull()?.toInt() ?: return null
            return years to null
        }
        return null
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

    private fun findEducationValue(lines: List<RecognizedLine>): RecognizedLine? {
        val inlineLabel = Regex("(?i)^education\\s*[:|]\\s*(.+)$")
        lines.forEachIndexed { index, line ->
            inlineLabel.matchEntire(line.text.trim())?.groupValues?.get(1)?.let { value ->
                return line.copy(text = value.trim())
            }
            if (!Regex("(?i)^education\\s*:?$").matches(line.text.trim())) return@forEachIndexed
            val value = lines.drop(index + 1).takeWhile { next ->
                !SECTION_HEADING.matches(next.text.trim()) && !DESCRIPTION_END_HEADING.matches(next.text.trim())
            }.take(3).filterNot { UI_NOISE.matches(it.text) }.joinToString(" ") { it.text.trim() }
            if (value.isNotBlank()) return line.copy(text = value)
        }
        return null
    }

    private fun chooseSkills(lines: List<RecognizedLine>): FieldCandidate? {
        val naukriSkillHeading = Regex("(?i)^(?:must have skills|good to have skills)\\s*:?$")
        val skillGroups = lines.mapIndexedNotNull { index, line ->
            if (!naukriSkillHeading.matches(line.text.trim())) return@mapIndexedNotNull null
            val group = lines.drop(index + 1).takeWhile { next ->
                !naukriSkillHeading.matches(next.text.trim()) && !SECTION_HEADING.matches(next.text.trim())
            }.take(8).map { it.text.trim() }.filterNot { UI_NOISE.matches(it) }
            line.text to group
        }
        if (skillGroups.isNotEmpty()) {
            val skills = skillGroups.flatMap { it.second }.distinct()
            if (skills.isNotEmpty()) return FieldCandidate(skills.joinToString(", "), 0.86f, skillGroups.joinToString("; ") { it.first })
        }
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

    private fun chooseNaukriSkillGroups(lines: List<RecognizedLine>): Pair<FieldCandidate?, FieldCandidate?> {
        val heading = Regex("(?i)^(must have skills|good to have skills)\\s*(?::|\\|)?\\s*(.*)$")
        var mandatory: FieldCandidate? = null
        var preferred: FieldCandidate? = null
        lines.forEachIndexed { index, line ->
            val match = heading.matchEntire(line.text.trim()) ?: return@forEachIndexed
            val isMandatory = match.groupValues[1].startsWith("must", ignoreCase = true)
            val inline = match.groupValues[2].trim()
            val following = lines.drop(index + 1).takeWhile { next ->
                !heading.matches(next.text.trim()) && !SECTION_HEADING.matches(next.text.trim())
            }.take(8).map { it.text.trim() }.filterNot { UI_NOISE.matches(it) }
            val values = (listOf(inline).filter(String::isNotBlank) + following).distinct()
            if (values.isEmpty()) return@forEachIndexed
            val candidate = FieldCandidate(values.joinToString(", "), 0.86f, line.text)
            if (isMandatory) mandatory = candidate else preferred = candidate
        }
        return mandatory to preferred
    }

    private companion object {
        val COMPANY_LABEL = Regex("(?i)\\b(?:company|employer)\\s*[:|]")
        val LOCATION_LABEL = Regex("(?i)\\b(?:job\\s+)?location\\s*[:|]|\\bbased\\s+in\\s*[:|]")
        val URL = Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE)
        val EXPERIENCE = Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:-|–|to)\\s*\\d+(?:\\.\\d+)?\\s*(?:years?|yrs?)\\b|\\b\\d+(?:\\+)?\\s*(?:years?|yrs?)\\b")
        val EXPERIENCE_RANGE = Regex("(?i)\\b(\\d+(?:\\.\\d+)?)\\s*(?:-|–|to)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:years?|yrs?)\\b")
        val EXPERIENCE_SINGLE = Regex("(?i)\\b(\\d+(?:\\+)?)(?:\\s*)(?:years?|yrs?)\\b")
        val EXPERIENCE_LABEL = Regex("(?i)(experience|work\\s+exp)")
        val SALARY = Regex("(?i)(?:\\b(?:salary|compensation|ctc)\\b|\\b\\d[\\d,.]*\\s*(?:lpa|lacs?|lakhs?)\\b|₹\\s*\\d[\\d,]{3,}(?:\\s*(?:-|–|to)\\s*₹?\\s*\\d[\\d,]{3,})?)")
        val NOT_DISCLOSED = Regex("(?i)\\b(?:salary\\s+)?not disclosed\\b")
        val EMPLOYMENT_TYPE = Regex("(?i)\\bfull[- ]?time\\b.*\\bpermanent\\b|\\bpermanent\\b.*\\bfull[- ]?time\\b")
        val WORK_ARRANGEMENT = Regex("(?i)weekly\\s+(?:once|twice|thrice|three times|\\d+ times)\\s+in office|(?:on[- ]site|onsite|hybrid|remote|work from home)")
        val PROMOTIONAL_NOISE = Regex("(?i)\\b(?:bright minds|bold ideas|similar roles?|similar jobs?|better world of work|world of work|join us and be part|innovation)\\b|similarroles")
        val JOB_DESCRIPTION_HEADING = Regex("(?i)^(?:job\\s+)?description\\s*:?$")
        val ABOUT_JOB_HEADING = Regex("(?i)^about the job\\s*:?$")
        val DESCRIPTION_END_HEADING = Regex("(?i)^(?:preferred candidate profile|education|role category|industry type|department|employment type|company profile|about company|similar jobs?|similar roles?|jobs you might like)\\s*:?$")
        val TIME_OR_STATUS = Regex("(?i)^(?:\\d{1,2}:\\d{2}.*|\\d{1,3}(?:\\.\\d+)?\\s*(?:kb/s|% battery)|(?:5g|4g|lte|volte|wifi|wi-?fi|charging))$")
        val NOTIFICATION_NOISE = Regex("(?i)\\b(?:new message|notification|battery low|charging|missed call|whatsapp|telegram|messages?)\\b")
        val REVIEWS = Regex("(?i)\\breviews?\\b")
        val RATING_BEFORE_REVIEWS = Regex("(?i)(\\d(?:\\.\\d+)?)\\s*(?=[|·\\s]+[\\d,.]+\\s*[KkMm]?\\s*reviews?)")
        val SOLO_RATING = Regex("(?i)^[0-5](?:\\.\\d+)?$")
        val REVIEW_COUNT = Regex("(?i)([\\d,.]+\\s*[KkMm]?)\\s*reviews?")
        val APPLICANTS = Regex("(?i)\\b[\\d,]+\\s+applicants?\\b")
        val APPLICANT_COUNT = Regex("(?i)\\b([\\d,]+)\\s+applicants?\\b")
        val POSTED_TEXT = Regex("(?i)\\bPosted\\s+\\d+\\s*(?:d|days?|w|weeks?|mo|months?)\\s+ago\\b")
        val VACANCIES = Regex("(?i)\\b\\d+\\s+vacanc(?:y|ies)\\b")
        val VACANCY_COUNT = Regex("(?i)\\b(\\d+)\\s+vacanc(?:y|ies)\\b")
        val EXPERIENCE_LEVEL = Regex("(?i)\\b(?:entry|associate|mid[- ]senior|senior|director|executive)\\s+level\\b")
        val EDUCATION = Regex("(?i)(?:education\\s*[:|]|B\\.?Tech|B\\.?E\\.?|bachelor|master|degree|any specialization)")
        val SKILLS_HEADING = Regex("(?i)\\b(?:key\\s+)?skills?\\b")
        val SECTION_HEADING = Regex("(?i)^(about the job|job description|description|key skills|skills|required skills|role|responsibilities|requirements|qualifications|education|about company|company profile|employment type|industry|role category)\\s*:?$")
        val UI_NOISE = Regex("(?i)^(jobs?|search|home|messaging|notifications?|employer|apply(?: now)?|applied|easy apply|quick apply|save(?:d)?|posted|see more|see all|share|follow|promoted|verified|top applicant|job alert|next|dashboard|edit|status|saved|rejected|archive|delete|import|contacts|resumes|settings|app settings|sheets sync|capture review|job details|about company)$|\\b(?:applicants?|followers?|connections?)\\b|\\b\\d+\\s+(?:applicants?|followers?)\\b")
        val METADATA = Regex("(?i)\\b(?:experience|work\\s+exp|salary|compensation|ctc|lpa|lacs?|lakhs?|posted|applicants?|job type|employment type|full[- ]time|part[- ]time|contract|internship|hybrid|on[- ]site|onsite|remote|years? of experience)\\b|₹|\\$|€|£")
        val LOCATION_LIKELY = Regex("(?i)\\b(remote|hybrid|on[- ]site|onsite|work from home|india|united states|united kingdom|canada|australia|bengaluru|bangalore|mumbai|pune|hyderabad|chennai|delhi|gurugram|gurgaon|noida|kolkata|ahmedabad|jaipur|karnataka|maharashtra|telangana|tamil nadu|greater [a-z ]+ area)\\b")
        val CHROME_NOISE = Regex("(?i)^(?:JobMate|JobMate Dashboard|Dashboard|Job Details|About Company|Edit|Status|Saved|Rejected|Archive|Delete|Import|Contacts|Resumes|Settings|App settings|Sheets sync|Capture review|Job poster|People you can reach out to|Meet the hiring team|People in your network|Show match details|Help me stand out|Use AI to assess how you fit)$")
        val CAPTURE_CONTROL = Regex("(?i)^(?:CAP|NEXT|FINISH|SAVE|OCR|📷|✓|!)$")
    }
}
