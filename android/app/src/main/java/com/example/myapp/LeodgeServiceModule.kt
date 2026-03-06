package com.example.myapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

/**
 * LeodgeServiceModule - React Native bridge module for controlling the persistent
 * widget background service.
 *
 * Provides methods to start/stop the service and check its status from JavaScript.
 */
@ReactModule(name = LeodgeServiceModule.NAME)
class LeodgeServiceModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "LeodgeServiceModule"
        private const val PREFS_NAME = "LeodgeServicePrefs"
        private const val KEY_AUTO_START = "auto_start_service"
    }

    private val prefs: SharedPreferences by lazy {
        reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Start the persistent widget service.
     * The service will run in the foreground with a notification.
     */
    @ReactMethod
    fun startService(promise: Promise) {
        try {
            // Clear user_stopped flag
            prefs.edit().putBoolean("user_stopped", false).apply()

            LeodgeWidgetService.start(reactContext)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SERVICE_ERROR", "Failed to start service: ${e.message}", e)
        }
    }

    /**
     * Stop the persistent widget service.
     */
    @ReactMethod
    fun stopService(promise: Promise) {
        try {
            // Set user_stopped flag so the service doesn't auto-restart
            prefs.edit().putBoolean("user_stopped", true).apply()

            LeodgeWidgetService.stop(reactContext)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SERVICE_ERROR", "Failed to stop service: ${e.message}", e)
        }
    }

    /**
     * Check if the widget service is currently running.
     */
    @ReactMethod
    fun isServiceRunning(promise: Promise) {
        promise.resolve(LeodgeWidgetService.isRunning)
    }

    /**
     * Get the auto-start preference (whether service starts on boot).
     */
    @ReactMethod
    fun getAutoStartEnabled(promise: Promise) {
        val enabled = prefs.getBoolean(KEY_AUTO_START, true)
        promise.resolve(enabled)
    }

    /**
     * Set the auto-start preference.
     */
    @ReactMethod
    fun setAutoStartEnabled(enabled: Boolean, promise: Promise) {
        prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
        promise.resolve(true)
    }

    /**
     * Request to ignore battery optimizations for the app.
     * This helps keep the service running on some devices.
     */
    @ReactMethod
    fun requestBatteryOptimizationExemption(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = reactContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val packageName = reactContext.packageName

                promise.resolve(powerManager.isIgnoringBatteryOptimizations(packageName))
            } else {
                promise.resolve(true)
            }
        } catch (e: Exception) {
            promise.reject("BATTERY_OPT_ERROR", e.message, e)
        }
    }

    override fun getName(): String = NAME

    override fun getConstants(): Map<String, Any>? {
        return hashMapOf(
            "UPDATE_INTERVAL_MINUTES" to LeodgeWidgetService.UPDATE_INTERVAL_MINUTES
        )
    }
}
