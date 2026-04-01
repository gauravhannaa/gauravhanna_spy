package com.gauravhanna.spy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NetworkChangeReceiver", "Network state changed: ${intent.action}")
        // Optionally trigger sync here
    }
}