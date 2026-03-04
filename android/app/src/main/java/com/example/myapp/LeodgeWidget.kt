package com.example.myapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import android.widget.Toast

// Constants
private const val PREFS_NAME = "LeodgeWidgetPrefs"
private const val KEY_TOTAL_VALUE = "total_value"
private const val KEY_CASH = "cash"
private const val KEY_INVESTED = "invested"
private const val KEY_UPDATED = "updated"

class LeodgeWidget : AppWidgetProvider() {
    
    companion object {
        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget created
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val prefs = LeodgeWidget.getSharedPreferences(context)
    
    val totalValue = prefs.getString(KEY_TOTAL_VALUE, null)
    val cash = prefs.getString(KEY_CASH, null)
    val invested = prefs.getString(KEY_INVESTED, null)
    val updated = prefs.getString(KEY_UPDATED, null)
    
    // Format values
    val displayTotal = if (totalValue != null) "£$totalValue" else "£ --"
    val displayCash = if (cash != null) "£$cash" else "£0.00"
    val displayInvested = if (invested != null) "£$invested" else "£0.00"
    val displayUpdated = if (updated != null) "Last updated: $updated" else "Last updated: --"
    
    // Create views
    val views = RemoteViews(context.packageName, R.layout.widget_leodge).apply {
        setTextViewText(R.id.widget_value, displayTotal)
        setTextViewText(R.id.widget_cash, displayCash)
        setTextViewText(R.id.widget_invested, displayInvested)
        setTextViewText(R.id.widget_updated, displayUpdated)
    }
    
    // Create pending intent for refresh button - launches the app
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_refresh, pendingIntent)
    
    // Update widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
