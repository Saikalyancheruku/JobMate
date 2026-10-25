package com.saikalyan.jobmate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
    private val captureInProgress = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingCaptureStart: Runnable? = null
    private var pendingCaptureTimeout: Runnable? = null
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var windowManager: WindowManager? = null
    private var captureBubbleRoot: View? = null
    private var captureBubble: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopProjection()
                stopSelf(startId)
            }
            ACTION_CAPTURE -> {
                requestCaptureAfterNotificationCloses()
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
            createCaptureBubble()
            observeCaptureStatus()
            controller.started()
        } catch (_: Exception) {
            stopProjection()
            controller.failed("Could not start capture. Check screen-sharing and Display over other apps permissions, then try again.")
            stopSelf(startId)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSurface() {
        val metrics = DisplayMetrics()
        val display = (getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay
        display.getRealMetrics(metrics)
        val reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
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
        mainHandler.post {
            pendingCaptureTimeout?.let(mainHandler::removeCallbacks)
            pendingCaptureTimeout = null
            captureBubble?.apply { text = "OCR"; visibility = View.VISIBLE }
        }
        scope.launch {
            processAndRelease(image)
        }
    }

    private suspend fun processAndRelease(image: Image) {
        try {
            controller.publish(recognizer.recognize(image))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            controller.failed("OCR failed (${failure.javaClass.simpleName}). Try again or use the in-app capture button.")
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
            .setContentText("CAP adds a page to this job; NEXT starts another job.")
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Stop", stopPendingIntent).build())
            .build()
    }

    /** Wait for Android to close the notification shade before requesting the next projected frame. */
    private fun requestCaptureAfterNotificationCloses() {
        if (projection == null) {
            controller.failed("Capture session is not active. Start screen capture in JobMate first.")
            return
        }
        if (!captureInProgress.compareAndSet(false, true)) {
            return
        }
        captureBubble?.visibility = View.INVISIBLE
        controller.waitingForFrame()
        val request = Runnable {
            pendingCaptureStart = null
            if (projection == null) {
                captureInProgress.set(false)
                controller.failed("The screen-capture session has ended. Start a new session in JobMate.")
            } else {
                captureRequested.set(true)
                val timeout = Runnable {
                    pendingCaptureTimeout = null
                    if (captureRequested.compareAndSet(true, false)) {
                        captureInProgress.set(false)
                        controller.failed("Could not capture this screen. Try tapping the bubble again.")
                    }
                }
                pendingCaptureTimeout = timeout
                mainHandler.postDelayed(timeout, CAPTURE_FRAME_TIMEOUT_MS)
            }
        }
        pendingCaptureStart = request
        mainHandler.postDelayed(request, NOTIFICATION_DISMISS_DELAY_MS)
    }

    private fun createCaptureBubble() {
        check(Settings.canDrawOverlays(this)) { "JobMate needs Display over other apps access for the capture bubble." }
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val size = (56 * density).toInt()
        val params = WindowManager.LayoutParams(
            size * 2 + (8 * density).toInt(),
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (220 * density).toInt()
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val bubble = TextView(this).apply {
            text = "CAP"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            contentDescription = "Capture and save this job screen"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(25, 103, 210))
                setStroke((2 * density).toInt(), Color.WHITE)
            }
            elevation = 8 * density
        }
        val nextBubble = TextView(this).apply {
            text = "NEXT"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            contentDescription = "Start capturing a different job listing"
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16 * density
                setColor(Color.rgb(55, 71, 79))
            }
            elevation = 8 * density
        }
        root.addView(bubble, LinearLayout.LayoutParams(size, size).apply { marginEnd = (4 * density).toInt() })
        root.addView(nextBubble, LinearLayout.LayoutParams(size, size))
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        fun installTouch(view: View, onTap: () -> Unit) {
            view.setOnTouchListener(object : View.OnTouchListener {
                private var downRawX = 0f
                private var downRawY = 0f
                private var startX = 0
                private var startY = 0
                private var dragged = false

                override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = params.x
                        startY = params.y
                        dragged = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) dragged = true
                        if (dragged) {
                            params.x = startX + dx.toInt()
                            params.y = startY + dy.toInt()
                            runCatching { manager.updateViewLayout(root, params) }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragged) onTap()
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
            })
        }
        installTouch(bubble) { requestCaptureAfterNotificationCloses() }
        installTouch(nextBubble) {
            if (captureInProgress.get()) {
                Toast.makeText(this, "Wait for the current page to finish saving.", Toast.LENGTH_SHORT).show()
            } else {
                controller.startNextJob()
                Toast.makeText(this, "New job started. Capture its pages with CAP.", Toast.LENGTH_SHORT).show()
            }
        }
        manager.addView(root, params)
        windowManager = manager
        captureBubbleRoot = root
        captureBubble = bubble
    }

    private fun observeCaptureStatus() {
        scope.launch {
            controller.state.collect { state ->
                if (state is CaptureSessionState.Saved || state is CaptureSessionState.Error) {
                    captureInProgress.set(false)
                }
                mainHandler.post { updateCaptureBubble(state) }
            }
        }
    }

    private fun updateCaptureBubble(state: CaptureSessionState) {
        val bubble = captureBubble ?: return
        when (state) {
            CaptureSessionState.Idle -> bubble.text = "CAP"
            CaptureSessionState.Capturing -> { bubble.text = "CAP"; bubble.visibility = View.VISIBLE }
            CaptureSessionState.WaitingForFrame -> { bubble.text = "…"; bubble.visibility = View.INVISIBLE }
            CaptureSessionState.Saving -> { bubble.text = "SAVE"; bubble.visibility = View.VISIBLE }
            is CaptureSessionState.Saved -> { bubble.text = "✓"; bubble.visibility = View.VISIBLE }
            is CaptureSessionState.Error -> {
                bubble.text = "!"
                bubble.contentDescription = state.message
                bubble.visibility = View.VISIBLE
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun removeCaptureBubble() {
        val root = captureBubbleRoot ?: return
        runCatching { windowManager?.removeView(root) }
        captureBubbleRoot = null
        captureBubble = null
        windowManager = null
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
        pendingCaptureStart?.let(mainHandler::removeCallbacks)
        pendingCaptureStart = null
        pendingCaptureTimeout?.let(mainHandler::removeCallbacks)
        pendingCaptureTimeout = null
        captureRequested.set(false)
        captureInProgress.set(false)
        removeCaptureBubble()
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
        private const val NOTIFICATION_DISMISS_DELAY_MS = 500L
        private const val CAPTURE_FRAME_TIMEOUT_MS = 5000L
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

