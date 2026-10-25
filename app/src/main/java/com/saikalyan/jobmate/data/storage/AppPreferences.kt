package com.saikalyan.jobmate.data.storage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile("app_preferences") },
    )
    val darkTheme: Flow<Boolean> = store.data.map { it[DARK_THEME] ?: false }
    val senderName: Flow<String> = store.data.map { it[SENDER_NAME].orEmpty() }
    val linkedInProfileUrl: Flow<String> = store.data.map { it[LINKEDIN_PROFILE_URL].orEmpty() }
    suspend fun setDarkTheme(enabled: Boolean) { store.edit { it[DARK_THEME] = enabled } }
    suspend fun setSenderName(name: String) { store.edit { it[SENDER_NAME] = name.trim() } }
    suspend fun setLinkedInProfileUrl(url: String) { store.edit { it[LINKEDIN_PROFILE_URL] = url.trim() } }
    suspend fun clearSenderName() { store.edit { it.remove(SENDER_NAME) } }
    suspend fun clearLinkedInProfileUrl() { store.edit { it.remove(LINKEDIN_PROFILE_URL) } }
    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SENDER_NAME = stringPreferencesKey("sender_name")
        val LINKEDIN_PROFILE_URL = stringPreferencesKey("linkedin_profile_url")
    }
}
