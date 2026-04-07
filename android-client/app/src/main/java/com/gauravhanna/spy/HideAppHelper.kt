package com.gauravhanna.spy

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object HideAppHelper {

    // ✅ Keep if used, otherwise delete this file

    @Suppress("unused")
    fun hideAppIcon(context: Context) {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, "${context.packageName}.HiddenLauncherActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("unused")
    fun showAppIcon(context: Context) {
        try {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, "${context.packageName}.HiddenLauncherActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}