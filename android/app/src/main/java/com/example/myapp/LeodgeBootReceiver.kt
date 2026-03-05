package com.example.myapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

/**
 * LeodgeBootReceiver - Broadcast receiver that starts the widget service on device boot
 *
 * This ensures that when the device restarts, the background service is automatically
 * restarted if it was previously running (like WhatsApp's persistence behavior).
 */
class LeodgeBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "LeodgeBootReceiver"
        private const val PREFS_CREDENTIALS = "LeodgeCredentials"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Boot completed received")

            // Check if service was running before reboot
            val prefs = context.getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            val wasRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
            val apiKey = prefs.getString(KEY_API_KEY, "")
            val apiSecret = prefs.getString(KEY_API_SECRET, "")

            if (wasRunning && !apiKey.isNullOrEmpty() && !apiSecret.isNullOrEmpty()) {
                Log.d(TAG, "Service was running, restarting background service")
                try {
                    LeodgeWidgetService.startService(context, apiKey, apiSecret)
                    Log.d(TAG, "Background service restarted successfully after boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restart background service after boot", e)
                }
            } else {
                Log.d(TAG, "Service was not running before boot or no credentials, skipping")
            }
        }
    }
}
