// android/app/src/main/java/com/leodge/LeodgeWidget.kt
package com.leodge

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews

class LeodgeWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(PREF_VALUE, "—") ?: "—"

        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, value)
        }
    }

    companion object {
        const val PREFS_NAME = "LeodgeWidgetPrefs"
        const val PREF_VALUE = "portfolioValue"

        fun updateAllWidgets(context: Context, totalValue: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_VALUE, totalValue).apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, LeodgeWidget::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id, totalValue)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            value: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_leodge)
            views.setTextViewText(R.id.widget_value, "£$value")

            // Tap widget → open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
