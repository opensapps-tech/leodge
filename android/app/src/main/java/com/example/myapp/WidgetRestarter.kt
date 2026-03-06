package com.example.myapp

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * WidgetRestarter - Restarts the widget service when the app is updated or
 * when the widget is added to the home screen.
 *
 * This ensures the service is running whenever needed without requiring
 * the user to manually open the app.
 */
class WidgetRestarter : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                // Widget was added or updated - ensure service is running
                if (!LeodgeWidgetService.isRunning) {
                    LeodgeWidgetService.start(context)
                }
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // App was updated - restart the service
                LeodgeWidgetService.start(context)
            }
        }
    }
}
