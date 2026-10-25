package com.saikalyan.jobmate.data.remote

import com.saikalyan.jobmate.domain.model.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class ReferralEmailDraft(val subject: String, val body: String)

@Singleton
class GeminiReferralDraftClient @Inject constructor() {
    suspend fun generate(job: Job, profile: String, apiKey: String): ReferralEmailDraft = withContext(Dispatchers.IO) {
        require(profile.isNotBlank()) { "Add and save your professional profile in AI settings first." }
        val prompt = """
            Draft a concise, professional email asking a contact for a referral for the job below.
            Use only facts present in the job and candidate profile. Do not invent experience, relationships, or claims.
            Return a subject and body as JSON. This draft will be reused for different contacts.
            Use {{firstName}} for the recipient's first name and {{senderName}} for the sender's name.
            You may use {{jobTitle}}, {{companyName}}, and {{jobUrl}} placeholders. Do not include other placeholders.
            Do not include a fabricated contact name, signature, or email address.
            Do not claim a resume or CV is attached; JobMate adds that statement only when the user selects a resume.

            Candidate profile:
            ${profile.take(8_000)}

            Job:
            Title: ${job.title}
            Company: ${job.companyName}
            Location: ${job.location.orEmpty()}
            Experience: ${listOfNotNull(job.experienceMin?.let { "minimum $it years" }, job.experienceMax?.let { "maximum $it years" }).joinToString(", ")}
            Skills: ${(job.mandatorySkills + job.preferredSkills + job.skills).distinct().joinToString(", ")}
            Description: ${job.description.orEmpty().take(5_000)}
            URL: ${job.sourceUrl.orEmpty()}
        """.trimIndent()
        val connection = (URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            val request = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject().put("temperature", 0.3).put("responseMimeType", "application/json")
                    .put("responseSchema", JSONObject().put("type", "OBJECT").put("properties", JSONObject()
                        .put("subject", JSONObject().put("type", "STRING"))
                        .put("body", JSONObject().put("type", "STRING")))
                        .put("required", JSONArray().put("subject").put("body"))))
            connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(when (connection.responseCode) {
                    400, 401, 403 -> "Gemini rejected the API key or request. Check the saved key and model access."
                    429 -> "Gemini rate limit reached. Try again later."
                    in 500..599 -> "Gemini is temporarily unavailable. Try again later."
                    else -> "Gemini request failed (HTTP ${connection.responseCode})."
                })
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val text = JSONObject(response).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            val json = JSONObject(text)
            ReferralEmailDraft(
                subject = json.optString("subject").trim().takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Gemini returned no email subject."),
                body = json.optString("body").trim().takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Gemini returned no email body."),
            )
        } finally {
            connection.disconnect()
        }
    }

    private companion object { const val MODEL = "gemini-3.5-flash-lite" }
}
