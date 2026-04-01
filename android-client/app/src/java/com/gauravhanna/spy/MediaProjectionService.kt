package com.gauravhanna.spy

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MediaProjectionService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}