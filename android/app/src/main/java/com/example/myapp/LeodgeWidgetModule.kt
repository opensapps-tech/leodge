package com.example.myapp

import android.content.Context
import android.content.ComponentName
import android.content.SharedPreferences
import android.appwidget.AppWidgetManager
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = LeodgeWidgetModule.NAME)
class LeodgeWidgetModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "LeodgeWidgetModule"
        private const val PREFS_NAME = "LeodgeWidgetPrefs"
        private const val KEY_TOTAL_VALUE = "total_value"
        private const val KEY_CASH = "cash"
        private const val KEY_INVESTED = "invested"
        private const val KEY_UPDATED = "updated"
    }

    private val prefs: SharedPreferences by lazy {
        reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @ReactMethod
    fun updateWidget(totalValue: String, cash: String, invested: String, updated: String, promise: Promise) {
        try {
            // Save all values to SharedPreferences
            prefs.edit()
                .putString(KEY_TOTAL_VALUE, totalValue)
                .putString(KEY_CASH, cash)
                .putString(KEY_INVESTED, invested)
                .putString(KEY_UPDATED, updated)
                .apply()
            
            // Notify the widget to update
            val appWidgetManager = AppWidgetManager.getInstance(reactContext)
            val componentName = ComponentName(reactContext, LeodgeWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            
            val intent = android.content.Intent(reactContext, LeodgeWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            reactContext.sendBroadcast(intent)
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("WIDGET_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun startBackgroundService(apiKey: String, apiSecret: String, promise: Promise) {
        try {
            LeodgeWidgetService.startService(reactContext, apiKey, apiSecret)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SERVICE_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun stopBackgroundService(promise: Promise) {
        try {
            LeodgeWidgetService.stopService(reactContext)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SERVICE_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun isBackgroundServiceRunning(promise: Promise) {
        try {
            val isRunning = LeodgeWidgetService.isServiceRunning(reactContext)
            promise.resolve(isRunning)
        } catch (e: Exception) {
            promise.reject("SERVICE_ERROR", e.message, e)
        }
    }

    override fun getName(): String = NAME
}
