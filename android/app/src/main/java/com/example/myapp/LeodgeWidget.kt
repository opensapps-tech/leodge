package com.example.myapp

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews

class LeodgeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget enabled for the first time
    }

    override fun onDisabled(context: Context) {
        // Widget disabled
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, LeodgeWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val prefs: SharedPreferences = context
                .getSharedPreferences("leodge_widget_prefs", Context.MODE_PRIVATE)
            val portfolioValue = prefs.getString("portfolio_value", null)

            val views = RemoteViews(context.packageName, R.layout.widget_leodge)

            val displayText = if (portfolioValue != null) {
                "LEODGE £$portfolioValue"
            } else {
                "LEODGE £ --"
            }

            views.setTextViewText(R.id.widget_text, displayText)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
