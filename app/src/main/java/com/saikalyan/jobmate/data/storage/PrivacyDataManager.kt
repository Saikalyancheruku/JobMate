package com.saikalyan.jobmate.data.storage

import android.content.Context
import androidx.work.WorkManager
import com.saikalyan.jobmate.data.local.JobMateDatabase
import com.saikalyan.jobmate.data.secure.SecureCredentialStore
import com.saikalyan.jobmate.data.secure.AiApiKeyStore
import com.saikalyan.jobmate.data.secure.SheetsSyncPreferences
import com.saikalyan.jobmate.worker.GoogleSheetsSyncWorker
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class PrivacyDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: JobMateDatabase,
    private val credentials: SecureCredentialStore,
    private val aiApiKeyStore: AiApiKeyStore,
    private val sheetsPreferences: SheetsSyncPreferences,
) {
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelAllWork()
        if (sheetsPreferences.get().enabled) runCatching {
            val request = RevokeAccessRequest.builder().setScopes(listOf(Scope(GoogleSheetsSyncWorker.DRIVE_FILE_SCOPE))).build()
            Tasks.await(Identity.getAuthorizationClient(context).revokeAccess(request), 15, TimeUnit.SECONDS)
        }
        sheetsPreferences.disconnect()
        credentials.clear()
        aiApiKeyStore.clear()
        database.clearAllTables()
        File(context.filesDir, "resumes").deleteRecursively()
        File(context.cacheDir, "exports").deleteRecursively()
        File(context.filesDir, "resumes").mkdirs()
    }
}
