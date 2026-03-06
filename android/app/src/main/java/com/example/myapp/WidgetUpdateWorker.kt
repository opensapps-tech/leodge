package com.example.myapp

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * WidgetUpdateWorker - WorkManager worker for periodic widget updates.
 *
 * This worker is triggered periodically to refresh the widget display.
 * It runs even when the app is closed and survives Doze mode.
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val context = applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LeodgeWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            // Trigger widget update for all instances
            if (appWidgetIds.isNotEmpty()) {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
