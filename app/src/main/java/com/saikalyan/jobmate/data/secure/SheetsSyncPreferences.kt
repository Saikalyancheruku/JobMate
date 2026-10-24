package com.saikalyan.jobmate.data.secure

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SheetsConnection(val enabled: Boolean, val spreadsheetId: String?, val spreadsheetUrl: String?, val authRequired: Boolean, val lastError: String? = null)

@Singleton
class SheetsSyncPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val mutex = Mutex()
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile("sheets_sync_preferences") },
    )
    val connection: Flow<SheetsConnection> = store.data.map { p ->
        SheetsConnection(p[ENABLED] ?: false, p[SHEET_ID], p[SHEET_URL], p[AUTH_REQUIRED] ?: false, p[LAST_ERROR])
    }
    suspend fun get(): SheetsConnection {
        return connection.first()
    }
    suspend fun enable() { store.edit { it[ENABLED] = true; it[AUTH_REQUIRED] = false; it.remove(LAST_ERROR) } }
    suspend fun requireAuthorization() { store.edit { it[AUTH_REQUIRED] = true; it[LAST_ERROR] = "Google authorization needs your attention. Reconnect to resume sync." } }
    suspend fun saveSheet(id: String, url: String) { store.edit { it[SHEET_ID] = id; it[SHEET_URL] = url; it[ENABLED] = true; it[AUTH_REQUIRED] = false; it.remove(LAST_ERROR) } }
    suspend fun recordError(message: String) { store.edit { it[LAST_ERROR] = message } }
    suspend fun setEnabled(value: Boolean) { store.edit { it[ENABLED] = value } }
    suspend fun disconnect() = mutex.withLock { store.edit { it.clear() } }
    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val AUTH_REQUIRED = booleanPreferencesKey("auth_required")
        val SHEET_ID = stringPreferencesKey("spreadsheet_id")
        val SHEET_URL = stringPreferencesKey("spreadsheet_url")
        val LAST_ERROR = stringPreferencesKey("last_error")
    }
}
