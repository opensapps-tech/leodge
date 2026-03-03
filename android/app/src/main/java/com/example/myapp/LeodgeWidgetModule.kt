package com.example.myapp

import android.content.Context
import android.content.SharedPreferences
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = LeodgeWidgetModule.NAME)
class LeodgeWidgetModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "LeodgeWidgetModule"
        private const val PREFS_NAME = "LeodgeWidgetPrefs"
        private const val KEY_PORTFOLIO_VALUE = "portfolio_value"
    }

    private val prefs: SharedPreferences by lazy {
        reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @ReactMethod
    fun updateWidget(totalValue: String, promise: Promise) {
        try {
            // Save the value to SharedPreferences
            prefs.edit().putString(KEY_PORTFOLIO_VALUE, totalValue).apply()
            
            // Notify the widget to update
            val intent = android.content.Intent(reactContext, LeodgeWidget::class.java).apply {
                action = "android.appwidget.action.APPWIDGET_UPDATE"
            }
            val ids = android.appwidget.AppWidgetManager.getInstance(reactContext)
                .getAppWidgetIds(android.componentType.LeodgeWidget::class.java.name.let { 
                    android.content.ComponentName(reactContext, LeodgeWidget::class.java) 
                })
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            reactContext.sendBroadcast(intent)
            
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("WIDGET_ERROR", e.message, e)
        }
    }

    override fun getName(): String = NAME
}
