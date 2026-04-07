package com.gauravhanna.spy

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object AppUsageCollector {
    private const val TAG = "AppUsageCollector"
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun start(context: Context) {
        executor.scheduleAtFixedRate({
            collectAndSend(context)
        }, 0, 60, TimeUnit.SECONDS) // every minute
    }

    // Force sync for network reconnection
    fun forceSync(context: Context) {
        executor.execute {
            Log.d(TAG, "AppUsage force sync triggered")
            collectAndSend(context)
        }
    }

    private fun collectAndSend(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 24 * 60 * 60 * 1000 // last 24 hours
                val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

                if (stats != null) {
                    for (stat in stats) {
                        if (stat.totalTimeInForeground > 0) {
                            // ✅ Use new sendAppUsage signature (appPackage, appName, foregroundTime, timestamp)
                            NetworkHelper.sendAppUsage(
                                context,
                                stat.packageName,
                                stat.packageName, // appName (can use package name as fallback)
                                (stat.totalTimeInForeground / 1000).toInt(),
                                stat.lastTimeUsed
                            )
                            Log.d(TAG, "App usage sent: ${stat.packageName} - ${stat.totalTimeInForeground / 1000}s")
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Usage stats permission not granted: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting app usage: ${e.message}")
            }
        }
    }
}