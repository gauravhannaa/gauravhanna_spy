package com.gauravhanna.spy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm triggered")
        // Start background service if needed
        // context.startService(Intent(context, BackgroundService::class.java))
    }
}