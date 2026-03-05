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
        private const val KEY_API_KEY = "api_key"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed received")
            
            // Check if credentials exist (service was previously running)
            val prefs = context.getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            val apiKey = prefs.getString(KEY_API_KEY, "")
            
            if (!apiKey.isNullOrEmpty()) {
                Log.d(TAG, "Credentials found, restarting background service")
                try {
                    LeodgeWidgetService.startService(context, apiKey, prefs.getString("api_secret", "") ?: "")
                    Log.d(TAG, "Background service restarted successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restart background service", e)
                }
            } else {
                Log.d(TAG, "No credentials found, skipping service restart")
            }
        }
    }
}
