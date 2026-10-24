package com.saikalyan.jobmate.data.remote

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteSheetRow(val index: Int, val id: String, val deleted: Boolean, val updatedAt: String, val values: List<String>)
data class CreatedSpreadsheet(val id: String, val url: String)
class SheetsApiException(val statusCode: Int, message: String) : IOException(message)

/** Minimal HTTPS REST client; bearer tokens are transient and never logged or persisted. */
@Singleton
class GoogleSheetsClient @Inject constructor() {
    fun createSpreadsheet(accessToken: String): CreatedSpreadsheet {
        val body = JSONObject().put("properties", JSONObject().put("title", "JobMate Sync"))
            .put("sheets", JSONArray().put(JSONObject().put("properties", JSONObject().put("title", JOBS_TAB)))
                .put(JSONObject().put("properties", JSONObject().put("title", CONTACTS_TAB))))
        val created = JSONObject(request("POST", "$SHEETS_BASE/spreadsheets", accessToken, body.toString()))
        val id = created.getString("spreadsheetId")
        val data = JSONArray()
            .put(headerData("'$JOBS_TAB'!A1:V1", JOB_HEADERS))
            .put(headerData("'$CONTACTS_TAB'!A1:J1", CONTACT_HEADERS))
        request("POST", "$SHEETS_BASE/spreadsheets/$id/values:batchUpdate", accessToken,
            JSONObject().put("valueInputOption", "RAW").put("data", data).toString())
        return CreatedSpreadsheet(id, created.optString("spreadsheetUrl", "https://docs.google.com/spreadsheets/d/$id"))
    }

    fun readRows(accessToken: String, spreadsheetId: String, tab: String): List<RemoteSheetRow> {
        val endColumn = if (tab == CONTACTS_TAB) "J" else "V"
        val range = URLEncoder.encode("'$tab'!A2:$endColumn", StandardCharsets.UTF_8.name())
        val response = JSONObject(request("GET", "$SHEETS_BASE/spreadsheets/$spreadsheetId/values/$range?majorDimension=ROWS", accessToken))
        val rows = response.optJSONArray("values") ?: return emptyList()
        return buildList {
            for (i in 0 until rows.length()) {
                val valuesArray = rows.optJSONArray(i) ?: continue
                val values = (0 until valuesArray.length()).map { valuesArray.optString(it, "") }
                val id = values.getOrNull(0).orEmpty().trim()
                if (id.isBlank()) continue
                val updatedAtIndex = if (tab == CONTACTS_TAB) 9 else 21
                add(RemoteSheetRow(i + 2, id, values.getOrNull(1).equals("true", true), values.getOrNull(updatedAtIndex).orEmpty(), values))
            }
        }
    }

    fun writeRow(accessToken: String, spreadsheetId: String, tab: String, rowIndex: Int, values: List<String>) {
        val lastColumn = if (tab == JOBS_TAB) "V" else "J"
        val range = URLEncoder.encode("'$tab'!A$rowIndex:$lastColumn$rowIndex", StandardCharsets.UTF_8.name())
        val row = JSONArray().put(JSONArray(values))
        request("PUT", "$SHEETS_BASE/spreadsheets/$spreadsheetId/values/$range?valueInputOption=RAW", accessToken,
            JSONObject().put("majorDimension", "ROWS").put("values", row).toString())
    }

    fun appendRow(accessToken: String, spreadsheetId: String, tab: String, values: List<String>) {
        val lastColumn = if (tab == JOBS_TAB) "V" else "J"
        val range = URLEncoder.encode("'$tab'!A:$lastColumn", StandardCharsets.UTF_8.name())
        request("POST", "$SHEETS_BASE/spreadsheets/$spreadsheetId/values/$range:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS", accessToken,
            JSONObject().put("majorDimension", "ROWS").put("values", JSONArray().put(JSONArray(values))).toString())
    }

    private fun headerData(range: String, headers: List<String>) = JSONObject().put("range", range)
        .put("majorDimension", "ROWS").put("values", JSONArray().put(JSONArray(headers)))

    private fun request(method: String, url: String, token: String, body: String? = null): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8") }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw SheetsApiException(code, "Google Sheets request failed ($code).")
            return response
        } finally { connection.disconnect() }
    }

    companion object {
        const val JOBS_TAB = "Jobs"
        const val CONTACTS_TAB = "Contacts"
        val JOB_HEADERS = listOf("id", "deleted", "title", "companyName", "location", "description", "skillsJson", "experienceMin", "experienceMax", "salaryText", "employmentType", "postedAt", "source", "sourceUrl", "status", "appliedAt", "followUpAt", "notes", "captureMethod", "fingerprint", "createdAt", "updatedAt")
        val CONTACT_HEADERS = listOf("id", "deleted", "fullName", "email", "companyName", "domain", "designation", "notes", "createdAt", "updatedAt")
        private const val SHEETS_BASE = "https://sheets.googleapis.com/v4"
        private const val TIMEOUT_MS = 20_000
    }
}
