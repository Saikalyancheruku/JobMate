package com.saikalyan.jobmate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.graphics.ImageFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.saikalyan.jobmate.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class CaptureForegroundService : Service() {
    @Inject lateinit var controller: CaptureSessionController
    @Inject lateinit var recognizer: com.saikalyan.jobmate.data.remote.OcrTextRecognizer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, _ -> controller.failed("On-device text recognition failed.") })
    private val captureRequested = AtomicBoolean(false)
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopProjection()
                stopSelf(startId)
            }
            ACTION_CAPTURE -> {
                if (projection == null) controller.failed("Capture session is not active.")
                else {
                    captureRequested.set(true)
                    controller.waitingForFrame()
                }
            }
            ACTION_START -> startProjection(intent, startId)
            else -> {
                controller.failed("Capture was not started with Android screen-capture consent.")
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startProjection(intent: Intent, startId: Int) {
        if (projection != null) {
            controller.failed("This capture session is already active. Stop it before starting another.")
            return
        }
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val token = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
        }
        if (resultCode == 0 || token == null) {
            controller.failed("Android screen-capture consent was missing or expired. Request consent again.")
            stopSelf(startId)
            return
        }
        try {
            createNotificationChannel()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                if (Build.VERSION.SDK_INT >= 29) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0,
            )
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val activeProjection = manager.getMediaProjection(resultCode, token)
                ?: throw IllegalStateException("Android did not provide a screen-capture session.")
            projection = activeProjection
            activeProjection.registerCallback(projectionCallback, Handler(mainLooper))
            createCaptureSurface()
            controller.started()
        } catch (_: Exception) {
            controller.failed("Could not start the screen-capture session. Try granting consent again.")
            stopProjection()
            stopSelf(startId)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSurface() {
        val metrics = DisplayMetrics()
        val display = (getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        display.getRealMetrics(metrics)
        val reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, ImageFormat.YUV_420_888, 2)
        val thread = HandlerThread("JobMateCaptureFrame").apply { start() }
        val handler = Handler(thread.looper)
        imageReader = reader
        workerThread = thread
        workerHandler = handler
        reader.setOnImageAvailableListener(::onImageAvailable, handler)
        virtualDisplay = projection?.createVirtualDisplay(
            "JobMateUserCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler,
        ) ?: throw IllegalStateException("Could not create the capture display.")
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = try { reader.acquireLatestImage() } catch (_: Exception) { null } ?: return
        if (!captureRequested.getAndSet(false)) {
            image.close()
            return
        }
        scope.launch { processAndRelease(image) }
    }

    private suspend fun processAndRelease(image: Image) {
        try {
            controller.publish(recognizer.recognize(image))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            controller.failed("On-device recognition could not read this screen section.")
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, CaptureForegroundService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPendingIntent = PendingIntent.getActivity(
            this,
            OPEN_REQUEST_CODE,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("JobMate screen capture is active")
            .setContentText("Capture only when you tap Capture this section.")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Job capture", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun stopProjection() {
        val active = projection
        projection = null
        if (active != null) {
            runCatching { active.unregisterCallback(projectionCallback) }
            runCatching { active.stop() }
        }
        releaseCaptureResources()
        controller.stopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            projection = null
            releaseCaptureResources()
            controller.stopped()
            stopSelf()
        }
    }

    private fun releaseCaptureResources() {
        captureRequested.set(false)
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null
        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
    }

    override fun onDestroy() {
        stopProjection()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "jobmate_capture"
        private const val NOTIFICATION_ID = 8201
        private const val STOP_REQUEST_CODE = 8202
        private const val OPEN_REQUEST_CODE = 8203
        const val ACTION_START = "com.saikalyan.jobmate.capture.START"
        const val ACTION_CAPTURE = "com.saikalyan.jobmate.capture.CAPTURE"
        const val ACTION_STOP = "com.saikalyan.jobmate.capture.STOP"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_PROJECTION_DATA = "projection_data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val start = Intent(context, CaptureForegroundService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_PROJECTION_DATA, data)
            ContextCompat.startForegroundService(context, start)
        }

        fun captureCurrentSection(context: Context) {
            context.startService(Intent(context, CaptureForegroundService::class.java).setAction(ACTION_CAPTURE))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CaptureForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}

