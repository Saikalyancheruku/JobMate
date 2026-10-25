package com.saikalyan.jobmate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import android.content.Context
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.local.entity.ContactEntity
import com.saikalyan.jobmate.data.local.entity.JobEntity
import com.saikalyan.jobmate.data.local.entity.SyncConflictEntity
import com.saikalyan.jobmate.data.repository.SyncQueueManager
import com.saikalyan.jobmate.data.secure.SheetsConnection
import com.saikalyan.jobmate.data.secure.SheetsSyncPreferences
import com.saikalyan.jobmate.domain.model.JobStatus
import com.saikalyan.jobmate.data.local.dao.SyncConflictDao
import com.saikalyan.jobmate.data.local.dao.SyncQueueDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject

data class SheetsSyncUiState(val connection: SheetsConnection = SheetsConnection(false, null, null, false), val conflicts: List<SyncConflictEntity> = emptyList())

@HiltViewModel
class SheetsSyncViewModel @Inject constructor(
    private val database: JobMateDatabase,
    private val preferences: SheetsSyncPreferences,
    private val queue: SyncQueueDao,
    private val conflictsDao: SyncConflictDao,
    private val queueManager: SyncQueueManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(SheetsSyncUiState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch { _state.value = _state.value.copy(connection = preferences.get()) }
        viewModelScope.launch { preferences.connection.collect { _state.value = _state.value.copy(connection = it) } }
        viewModelScope.launch { conflictsDao.observeUnresolved().collect { _state.value = _state.value.copy(conflicts = it) } }
    }

    fun onAuthorized(existingSpreadsheetId: String? = null, createNewSpreadsheet: Boolean = false) { viewModelScope.launch {
        when {
            !existingSpreadsheetId.isNullOrBlank() -> preferences.saveSheet(
                existingSpreadsheetId,
                "https://docs.google.com/spreadsheets/d/$existingSpreadsheetId",
            )
            createNewSpreadsheet -> preferences.clearSheetReference()
        }
        preferences.enable()
        // The worker reads and reconciles the Sheet before queuing local-only/newer rows.
        // Enqueuing every local row here made the local database overwrite Sheet edits
        // before the first pull could happen.
        queueManager.schedule()
        queueManager.schedulePeriodic()
        _state.value = _state.value.copy(connection = preferences.get())
    } }

    fun disconnect() { viewModelScope.launch {
        preferences.disconnect()
        WorkManager.getInstance(context).cancelUniqueWork(SyncQueueManager.WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(SyncQueueManager.PERIODIC_WORK_NAME)
        _state.value = _state.value.copy(connection = preferences.get())
    } }

    fun syncNow() { viewModelScope.launch {
        queue.getFailed().forEach { queue.updateState(it.id, SyncQueueManager.STATE_PENDING, it.attemptCount, null, it.remoteUpdatedAt) }
        queueManager.schedule()
    } }

    fun setError(message: String) { viewModelScope.launch { preferences.recordError(message) } }

    fun resolve(conflict: SyncConflictEntity, useRemote: Boolean) { viewModelScope.launch {
        if (useRemote) {
            val values = JSONObject(conflict.remoteSnapshot).getJSONArray("values")
            val remoteDeleted = values.optString(1).equals("true", true)
            when (conflict.entityType) {
                "JOB" -> if (remoteDeleted) database.jobDao().deleteById(conflict.entityId) else {
                    val remoteJob = values.toJob()
                    val localJob = database.jobDao().findById(conflict.entityId)
                    database.jobDao().upsert(remoteJob.copy(aiCorrected = localJob?.aiCorrected ?: false))
                }
                "CONTACT" -> if (remoteDeleted) database.contactDao().deleteById(conflict.entityId) else database.contactDao().upsert(values.toContact())
            }
            queue.updateState(conflict.id, SyncQueueManager.STATE_SYNCED, 0, null, Instant.now())
            conflictsDao.setResolution(conflict.id, "REMOTE")
        } else {
            queue.updateState(conflict.id, SyncQueueManager.STATE_PENDING, 0, null, conflict.detectedAt)
            conflictsDao.setResolution(conflict.id, "LOCAL")
            queueManager.schedule()
        }
    } }

    private fun JSONArray.toJob(): JobEntity {
        val values = (0 until length()).map { optString(it, "") }
        val skills = JSONArray(values[6]).let { array -> (0 until array.length()).map { array.getString(it) } }
        val status = JobStatus.fromStorage(values[14])?.storageValue ?: JobStatus.SAVED.storageValue
        return JobEntity(values[0], values[2], values[3], values[4].ifBlank { null }, values[5].ifBlank { null }, skills,
            values[7].toIntOrNull(), values[8].toIntOrNull(), values[9].ifBlank { null }, values[10].ifBlank { null }, instant(values[11]),
            values[12].ifBlank { null }, values[13].ifBlank { null }, status, instant(values[15]), instant(values[16]), values[17].ifBlank { null },
            values[18].ifBlank { null }, values[19], instant(values[20]) ?: Instant.now(), instant(values[21]) ?: Instant.now(),
            values.getOrNull(22)?.ifBlank { null }, values.getOrNull(23)?.let(::stringListFromJson).orEmpty(),
            values.getOrNull(24)?.let(::stringListFromJson).orEmpty(), values.getOrNull(25)?.ifBlank { null },
            values.getOrNull(26)?.ifBlank { null }, values.getOrNull(27)?.toIntOrNull(), values.getOrNull(28)?.ifBlank { null },
            values.getOrNull(29)?.toIntOrNull(), values.getOrNull(30)?.ifBlank { null }, values.getOrNull(31)?.ifBlank { null },
            null, values.getOrNull(32)?.let(::instant))
    }

    private fun stringListFromJson(value: String): List<String> =
        JSONArray(value).let { array -> (0 until array.length()).map { array.getString(it) } }

    private fun JSONArray.toContact(): ContactEntity {
        val values = (0 until length()).map { optString(it, "") }
        return ContactEntity(values[0], values[2], values[3], values[4].ifBlank { null }, values[5].ifBlank { null },
            values[6].ifBlank { null }, values[7].ifBlank { null }, instant(values[8]) ?: Instant.now(), instant(values[9]) ?: Instant.now())
    }
    private fun instant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()
}
