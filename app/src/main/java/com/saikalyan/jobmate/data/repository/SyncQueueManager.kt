package com.saikalyan.jobmate.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.saikalyan.jobmate.data.local.dao.SyncQueueDao
import com.saikalyan.jobmate.data.local.entity.SyncQueueEntity
import com.saikalyan.jobmate.data.secure.SheetsSyncPreferences
import com.saikalyan.jobmate.worker.GoogleSheetsSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueManager @Inject constructor(
    private val queue: SyncQueueDao,
    private val preferences: SheetsSyncPreferences,
    private val clock: Clock,
    @ApplicationContext context: Context,
) {
    private val workManager = WorkManager.getInstance(context)
    suspend fun enqueue(type: String, id: String, operation: String = UPSERT, scheduleWork: Boolean = true) {
        if (!preferences.get().enabled || id.isBlank()) return
        val key = "$type:$id"
        queue.upsert(SyncQueueEntity(key, type, id, operation, 1, clock.instant(), 0, null, STATE_PENDING, null))
        if (scheduleWork) schedule()
    }
    fun schedule() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<GoogleSheetsSyncWorker>().setConstraints(constraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS).build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
    fun schedulePeriodic() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<GoogleSheetsSyncWorker>(12, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
    companion object {
        const val UPSERT = "UPSERT"
        const val DELETE = "DELETE"
        const val STATE_PENDING = "PENDING"
        const val STATE_SYNCED = "SYNCED"
        const val STATE_CONFLICT = "CONFLICT"
        const val STATE_FAILED = "FAILED"
        const val WORK_NAME = "jobmate-sheets-sync"
        const val PERIODIC_WORK_NAME = "jobmate-sheets-periodic-sync"
    }
}
