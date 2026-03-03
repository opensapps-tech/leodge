package com.example.myapp

import android.content.Context
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

        // Also trigger widget update
        try {
            val widgetProvider = LeodgeWidget()
            widgetProvider.updateAllWidgets(reactApplicationContext)
        } catch (e: Exception) {
            // Widget update failed, but data is saved
        }
    }
}
