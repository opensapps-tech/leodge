package com.example.myapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews

// Constants accessible outside the class
private const val PREFS_NAME = "LeodgeWidgetPrefs"
private const val KEY_PORTFOLIO_VALUE = "portfolio_value"

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
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val prefs = LeodgeWidget.getSharedPreferences(context)
    val portfolioValue = prefs.getString(KEY_PORTFOLIO_VALUE, null)
    
    val displayText = if (portfolioValue != null) {
        "LEODGE £$portfolioValue"
    } else {
        "LEODGE £ --"
    }

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.widget_leodge).apply {
        setTextViewText(R.id.widget_text, displayText)
    }

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
