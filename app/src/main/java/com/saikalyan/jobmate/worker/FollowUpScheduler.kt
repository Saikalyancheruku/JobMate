package com.saikalyan.jobmate.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.saikalyan.jobmate.domain.model.Job
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowUpScheduler @Inject constructor(@ApplicationContext context: Context, private val clock: Clock) {
    private val workManager = WorkManager.getInstance(context)
    fun schedule(job: Job) {
        val followUpAt = job.followUpAt
        if (followUpAt == null || job.status.name == "CLOSED") { cancel(job.id); return }
        val data = Data.Builder().putString(FollowUpNotificationWorker.KEY_JOB_ID, job.id)
            .putString(FollowUpNotificationWorker.KEY_TITLE, job.title).putString(FollowUpNotificationWorker.KEY_COMPANY, job.companyName).build()
        val delay = Duration.between(clock.instant(), followUpAt).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<FollowUpNotificationWorker>().setInputData(data).setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS).build()
        workManager.enqueueUniqueWork(workName(job.id), ExistingWorkPolicy.REPLACE, request)
    }
    fun cancel(jobId: String) { workManager.cancelUniqueWork(workName(jobId)) }
    private fun workName(id: String) = "follow-up:$id"
}
