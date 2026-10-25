package com.saikalyan.jobmate.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import com.saikalyan.jobmate.data.local.entity.JobEntity
import com.saikalyan.jobmate.data.local.entity.SyncConflictEntity
import com.saikalyan.jobmate.data.local.entity.SyncQueueEntity
import com.saikalyan.jobmate.data.remote.GoogleSheetsClient
import com.saikalyan.jobmate.data.remote.RemoteSheetRow
import com.saikalyan.jobmate.data.remote.SheetsApiException
import com.saikalyan.jobmate.data.secure.SheetsSyncPreferences
import com.saikalyan.jobmate.data.repository.SyncQueueManager
import com.saikalyan.jobmate.domain.model.JobStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SheetsSyncEntryPoint {
    fun database(): JobMateDatabase
    fun preferences(): SheetsSyncPreferences
    fun client(): GoogleSheetsClient
    fun clock(): Clock
}

class GoogleSheetsSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = syncMutex.withLock { withContext(Dispatchers.IO) {
        val entry = EntryPointAccessors.fromApplication(applicationContext, SheetsSyncEntryPoint::class.java)
        val db = entry.database()
        val prefs = entry.preferences()
        val api = entry.client()
        val connection = prefs.get()
        if (!connection.enabled) return@withContext Result.success()
        try {
            val request = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE))).build()
            val authorization = Tasks.await(Identity.getAuthorizationClient(applicationContext).authorize(request), 30, TimeUnit.SECONDS)
            if (authorization.hasResolution()) { prefs.requireAuthorization(); return@withContext Result.success() }
            val token = authorization.accessToken ?: run { prefs.requireAuthorization(); return@withContext Result.success() }
            val spreadsheetId = connection.spreadsheetId ?: api.createSpreadsheet(token).let { created ->
                prefs.saveSheet(created.id, created.url)
                created.id
            }
            api.ensureJobHeaders(token, spreadsheetId)
            val remoteJobs = api.readRows(token, spreadsheetId, GoogleSheetsClient.JOBS_TAB).associateBy { it.id }.toMutableMap()
            val remoteContacts = api.readRows(token, spreadsheetId, GoogleSheetsClient.CONTACTS_TAB).associateBy { it.id }.toMutableMap()
            val queue = db.syncQueueDao()
            for (item in queue.getPending()) {
                try {
                    when (item.entityType) {
                        "JOB" -> syncJob(item, db, api, token, spreadsheetId, remoteJobs, queue, db.syncConflictDao(), entry.clock())
                        "CONTACT" -> syncContact(item, db, api, token, spreadsheetId, remoteContacts, queue, db.syncConflictDao(), entry.clock())
                        else -> queue.updateState(item.id, SyncQueueManager.STATE_FAILED, item.attemptCount, "UNKNOWN_ENTITY", item.remoteUpdatedAt)
                    }
                } catch (failure: SheetsApiException) {
                    if (failure.statusCode == 401) { prefs.requireAuthorization(); return@withContext Result.success() }
                    if (failure.statusCode != 429 && failure.statusCode < 500) {
                        prefs.recordError("Google Sheets API returned ${failure.statusCode}. Check that the API is enabled and the sheet is accessible.")
                        queue.updateState(item.id, SyncQueueManager.STATE_FAILED, item.attemptCount + 1, "REMOTE_API_${failure.statusCode}", item.remoteUpdatedAt)
                        return@withContext Result.failure()
                    }
                    queue.updateState(item.id, SyncQueueManager.STATE_PENDING, item.attemptCount + 1, "REMOTE_API", item.remoteUpdatedAt)
                    return@withContext Result.retry()
                }
            }
            mergeRemoteJobs(remoteJobs.values, db, queue)
            mergeRemoteContacts(remoteContacts.values, db, queue)
            Result.success()
        } catch (failure: SheetsApiException) {
            if (failure.statusCode == 401) { prefs.requireAuthorization(); Result.success() }
            else if (failure.statusCode == 429 || failure.statusCode >= 500) Result.retry()
            else { prefs.recordError("Google Sheets API returned ${failure.statusCode}. Check that Sheets API is enabled and authorization is configured."); Result.failure() }
        } catch (_: Exception) {
            Result.retry()
        }
    } }

    private suspend fun syncJob(item: SyncQueueEntity, db: JobMateDatabase, api: GoogleSheetsClient, token: String, sheetId: String,
        remote: MutableMap<String, RemoteSheetRow>, queue: com.saikalyan.jobmate.data.local.dao.SyncQueueDao,
        conflicts: com.saikalyan.jobmate.data.local.dao.SyncConflictDao, clock: Clock) {
        val local = db.jobDao().findById(item.entityId)
        val forceLocal = conflicts.findById(item.id)?.resolution == "LOCAL"
        if (item.operation == SyncQueueManager.DELETE && !forceLocal) {
            val current = remote[item.entityId]
            if (current != null && !current.deleted && newer(current.updatedAt, item.queuedAt)) {
                val tombstone = listOf(item.entityId, "true") + List(19) { "" } + listOf(item.queuedAt.toString())
                conflicts.upsert(SyncConflictEntity(item.id, "JOB", item.entityId, JSONObject().put("values", JSONArray(tombstone)).toString(),
                    JSONObject().put("values", JSONArray(current.values)).toString(), clock.instant(), null))
                queue.updateState(item.id, SyncQueueManager.STATE_CONFLICT, item.attemptCount, "REMOTE_CHANGED", parseInstant(current.updatedAt))
                return
            }
        }
        if (item.operation == SyncQueueManager.DELETE || local == null) {
            val time = clock.instant().toString()
            val old = remote[item.entityId]
            val values = old?.values?.toMutableList()?.apply { while (size < 22) add(""); set(1, "true"); set(21, time) }
                ?: listOf(item.entityId, "true") + List(19) { "" } + listOf(time)
            write(api, token, sheetId, GoogleSheetsClient.JOBS_TAB, old, values)
            remote[item.entityId] = RemoteSheetRow(old?.index ?: -1, item.entityId, true, time, values)
            queue.updateState(item.id, SyncQueueManager.STATE_SYNCED, item.attemptCount, null, Instant.parse(time))
            return
        }
        val values = local.toSheetRow()
        val current = remote[item.entityId]
        if (!forceLocal && current != null && newer(current.updatedAt, local.updatedAt) && current.values != values) {
            conflicts.upsert(SyncConflictEntity(item.id, "JOB", item.entityId, JSONObject().put("values", JSONArray(values)).toString(),
                JSONObject().put("values", JSONArray(current.values)).toString(), clock.instant(), null))
            queue.updateState(item.id, SyncQueueManager.STATE_CONFLICT, item.attemptCount, "REMOTE_CHANGED", parseInstant(current.updatedAt))
            return
        }
        write(api, token, sheetId, GoogleSheetsClient.JOBS_TAB, current, values)
        remote[item.entityId] = RemoteSheetRow(current?.index ?: -1, item.entityId, false, local.updatedAt.toString(), values)
        queue.updateState(item.id, SyncQueueManager.STATE_SYNCED, item.attemptCount, null, parseInstant(current?.updatedAt))
    }

    private suspend fun syncContact(item: SyncQueueEntity, db: JobMateDatabase, api: GoogleSheetsClient, token: String, sheetId: String,
        remote: MutableMap<String, RemoteSheetRow>, queue: com.saikalyan.jobmate.data.local.dao.SyncQueueDao,
        conflicts: com.saikalyan.jobmate.data.local.dao.SyncConflictDao, clock: Clock) {
        val local = db.contactDao().findById(item.entityId)
        val forceLocal = conflicts.findById(item.id)?.resolution == "LOCAL"
        if (item.operation == SyncQueueManager.DELETE && !forceLocal) {
            val current = remote[item.entityId]
            if (current != null && !current.deleted && newer(current.updatedAt, item.queuedAt)) {
                val tombstone = listOf(item.entityId, "true") + List(7) { "" } + listOf(item.queuedAt.toString())
                conflicts.upsert(SyncConflictEntity(item.id, "CONTACT", item.entityId, JSONObject().put("values", JSONArray(tombstone)).toString(),
                    JSONObject().put("values", JSONArray(current.values)).toString(), clock.instant(), null))
                queue.updateState(item.id, SyncQueueManager.STATE_CONFLICT, item.attemptCount, "REMOTE_CHANGED", parseInstant(current.updatedAt))
                return
            }
        }
        if (item.operation == SyncQueueManager.DELETE || local == null) {
            val time = clock.instant().toString()
            val old = remote[item.entityId]
            val values = old?.values?.toMutableList()?.apply { while (size < 10) add(""); set(1, "true"); set(9, time) }
                ?: listOf(item.entityId, "true") + List(7) { "" } + listOf(time)
            write(api, token, sheetId, GoogleSheetsClient.CONTACTS_TAB, old, values)
            remote[item.entityId] = RemoteSheetRow(old?.index ?: -1, item.entityId, true, time, values)
            queue.updateState(item.id, SyncQueueManager.STATE_SYNCED, item.attemptCount, null, Instant.parse(time))
            return
        }
        val values = local.toSheetRow()
        val current = remote[item.entityId]
        if (!forceLocal && current != null && newer(current.updatedAt, local.updatedAt) && current.values != values) {
            conflicts.upsert(SyncConflictEntity(item.id, "CONTACT", item.entityId, JSONObject().put("values", JSONArray(values)).toString(),
                JSONObject().put("values", JSONArray(current.values)).toString(), clock.instant(), null))
            queue.updateState(item.id, SyncQueueManager.STATE_CONFLICT, item.attemptCount, "REMOTE_CHANGED", parseInstant(current.updatedAt))
            return
        }
        write(api, token, sheetId, GoogleSheetsClient.CONTACTS_TAB, current, values)
        remote[item.entityId] = RemoteSheetRow(current?.index ?: -1, item.entityId, false, local.updatedAt.toString(), values)
        queue.updateState(item.id, SyncQueueManager.STATE_SYNCED, item.attemptCount, null, parseInstant(current?.updatedAt))
    }

    private suspend fun mergeRemoteJobs(rows: Collection<RemoteSheetRow>, db: JobMateDatabase, queue: com.saikalyan.jobmate.data.local.dao.SyncQueueDao) {
        for (row in rows) {
            if (queue.findById("JOB:${row.id}")?.state in setOf(SyncQueueManager.STATE_PENDING, SyncQueueManager.STATE_CONFLICT)) continue
            val local = db.jobDao().findById(row.id)
            if (row.deleted) { if (local != null) db.jobDao().deleteById(row.id); continue }
            val remote = row.toJobEntity() ?: continue
            if (local == null) db.jobDao().upsert(remote)
            else if (remote.updatedAt.isAfter(local.updatedAt)) {
                // Raw OCR text is deliberately local-only and must survive a sheet pull.
                db.jobDao().upsert(remote.copy(rawCapturedText = local.rawCapturedText, capturedAt = remote.capturedAt ?: local.capturedAt))
            }
        }
    }

    private suspend fun mergeRemoteContacts(rows: Collection<RemoteSheetRow>, db: JobMateDatabase, queue: com.saikalyan.jobmate.data.local.dao.SyncQueueDao) {
        for (row in rows) {
            if (queue.findById("CONTACT:${row.id}")?.state in setOf(SyncQueueManager.STATE_PENDING, SyncQueueManager.STATE_CONFLICT)) continue
            val local = db.contactDao().findById(row.id)
            if (row.deleted) { if (local != null) db.contactDao().deleteById(row.id); continue }
            val remote = row.toContactEntity() ?: continue
            if (local == null || remote.updatedAt.isAfter(local.updatedAt)) db.contactDao().upsert(remote)
        }
    }

    private fun write(api: GoogleSheetsClient, token: String, sheetId: String, tab: String, old: RemoteSheetRow?, values: List<String>) {
        if (old == null || old.index < 2) api.appendRow(token, sheetId, tab, values)
        else api.writeRow(token, sheetId, tab, old.index, values)
    }

    private fun JobEntity.toSheetRow() = listOf(id, "false", title, companyName, location.orEmpty(), description.orEmpty(), JSONArray(skills).toString(),
        experienceMin?.toString().orEmpty(), experienceMax?.toString().orEmpty(), salaryText.orEmpty(), employmentType.orEmpty(), postedAt?.toString().orEmpty(),
        source.orEmpty(), sourceUrl.orEmpty(), status, appliedAt?.toString().orEmpty(), followUpAt?.toString().orEmpty(), notes.orEmpty(), captureMethod.orEmpty(), fingerprint,
        createdAt.toString(), updatedAt.toString(), workArrangement.orEmpty(), JSONArray(mandatorySkills).toString(), JSONArray(preferredSkills).toString(),
        companyRating.orEmpty(), companyReviewCount.orEmpty(), applicantCount?.toString().orEmpty(), postedText.orEmpty(), vacancyCount?.toString().orEmpty(),
        experienceLevel.orEmpty(), education.orEmpty(), capturedAt?.toString().orEmpty())

    private fun ContactEntity.toSheetRow() = listOf(id, "false", fullName, normalizedEmail, companyName.orEmpty(), domain.orEmpty(), designation.orEmpty(), notes.orEmpty(), createdAt.toString(), updatedAt.toString())

    private fun RemoteSheetRow.toJobEntity(): JobEntity? {
        return try {
            val v = values
            if (v.size < 22) return null
            if (v[2].isBlank() || v[3].isBlank()) return null
            val validStatus = JobStatus.fromStorage(v[14])?.storageValue ?: return null
            JobEntity(v[0], v[2], v[3], v[4].ifBlank { null }, v[5].ifBlank { null },
                JSONArray(v[6]).let { array -> (0 until array.length()).map { array.getString(it) } },
                v[7].toIntOrNull(), v[8].toIntOrNull(), v[9].ifBlank { null }, v[10].ifBlank { null }, parseInstant(v[11]),
                v[12].ifBlank { null }, v[13].ifBlank { null }, validStatus, parseInstant(v[15]), parseInstant(v[16]), v[17].ifBlank { null },
                v[18].ifBlank { null }, v[19], parseInstant(v[20]) ?: return null, parseInstant(v[21]) ?: return null,
                v.getOrNull(22)?.ifBlank { null }, v.getOrNull(23)?.let(::stringListFromJson).orEmpty(), v.getOrNull(24)?.let(::stringListFromJson).orEmpty(),
                v.getOrNull(25)?.ifBlank { null }, v.getOrNull(26)?.ifBlank { null }, v.getOrNull(27)?.toIntOrNull(), v.getOrNull(28)?.ifBlank { null },
                v.getOrNull(29)?.toIntOrNull(), v.getOrNull(30)?.ifBlank { null }, v.getOrNull(31)?.ifBlank { null }, null,
                v.getOrNull(32)?.let(::parseInstant))
        } catch (_: Exception) { null }
    }

    private fun RemoteSheetRow.toContactEntity(): ContactEntity? {
        return try {
            if (values.size < 10) return null
            if (values[2].isBlank() || !values[3].contains('@')) return null
            ContactEntity(values[0], values[2].trim(), values[3].trim().lowercase(Locale.ROOT), values[4].ifBlank { null }, values[5].ifBlank { null }, values[6].ifBlank { null },
                values[7].ifBlank { null }, parseInstant(values[8]) ?: return null, parseInstant(values[9]) ?: return null)
        } catch (_: Exception) { null }
    }

    private fun newer(remote: String, local: Instant) = parseInstant(remote)?.isAfter(local) == true

    private fun stringListFromJson(value: String): List<String> =
        JSONArray(value).let { array -> (0 until array.length()).map { array.getString(it) } }
    private fun parseInstant(value: String?): Instant? = try { value?.takeIf(String::isNotBlank)?.let(Instant::parse) } catch (_: Exception) { null }

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private val syncMutex = Mutex()
    }
}
