package com.example.myapp

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class LeodgeWidgetModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "LeodgeWidgetModule"

    @ReactMethod
    fun updateWidget(totalValue: String) {
        val prefs: SharedPreferences = reactApplicationContext
            .getSharedPreferences("leodge_widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("portfolio_value", totalValue).apply()

        // Trigger widget update via broadcast
        val intent = Intent(reactApplicationContext, LeodgeWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetManager = AppWidgetManager.getInstance(reactApplicationContext)
            val widgetIds = widgetManager.getAppWidgetIds(
                ComponentName(reactApplicationContext, LeodgeWidget::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        }
        reactApplicationContext.sendBroadcast(intent)
    }
}
