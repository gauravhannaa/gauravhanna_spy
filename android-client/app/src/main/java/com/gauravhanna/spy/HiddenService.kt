package com.gauravhanna.spy

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class HiddenService : Service() {

    companion object {
        private const val TAG = "HiddenService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HiddenService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HiddenService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HiddenService destroyed")
    }
}