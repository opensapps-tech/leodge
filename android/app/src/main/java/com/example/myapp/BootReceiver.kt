package com.example.myapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build

/**
 * BootReceiver - Automatically restarts the widget service when the device boots.
 *
 * This ensures the widget becomes active immediately after a device restart,
 * similar to how WhatsApp and other persistent apps work.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "LeodgeServicePrefs"
        private const val KEY_AUTO_START = "auto_start_service"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Check if auto-start is enabled
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val autoStart = prefs.getBoolean(KEY_AUTO_START, true)

                if (autoStart) {
                    // Clear the user_stopped flag on boot
                    prefs.edit().putBoolean("user_stopped", false).apply()

                    // Start the foreground service
                    LeodgeWidgetService.start(context)
                }
            }
        }
    }
}
