package com.saikalyan.jobmate.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.saikalyan.jobmate.MainActivity

class FollowUpNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Job follow-up"
        val company = inputData.getString(KEY_COMPANY).orEmpty()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Job follow-ups", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(applicationContext, jobId.hashCode(), Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(applicationContext).notify(jobId.hashCode(), NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Follow up: $title")
            .setContentText(company).setContentIntent(open).setAutoCancel(true).build())
        return Result.success()
    }
    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_TITLE = "job_title"
        const val KEY_COMPANY = "company"
        private const val CHANNEL_ID = "job_follow_ups"
    }
}
