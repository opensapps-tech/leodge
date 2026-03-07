package com.example.myapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * LeodgeWidgetService - Foreground service that keeps the widget always active.
 *
 * This service runs continuously in the foreground with a persistent notification
 * that displays live portfolio data in a compact area, similar to how WhatsApp
 * keeps its connection alive. The notification shows portfolio value, cash, and
 * invested amounts using custom layouts for both collapsed and expanded views.
 */
class LeodgeWidgetService : Service() {

    companion object {
        const val TAG = "LeodgeWidgetService"
        const val CHANNEL_ID = "leodge_widget_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.myapp.ACTION_START_SERVICE"
        const val ACTION_STOP = "com.example.myapp.ACTION_STOP_SERVICE"
        const val ACTION_UPDATE_NOTIFICATION = "com.example.myapp.ACTION_UPDATE_NOTIFICATION"
        const val WORK_TAG = "leodge_widget_update_work"

        // SharedPreferences keys for notification data
        private const val PREFS_NAME = "LeodgeWidgetPrefs"
        private const val KEY_TOTAL_VALUE = "total_value"
        private const val KEY_CASH = "cash"
        private const val KEY_INVESTED = "invested"
        private const val KEY_UPDATED = "updated"

        // Update interval - 15 minutes (minimum for WorkManager periodic work)
        const val UPDATE_INTERVAL_MINUTES = 15L

        @Volatile
        var isRunning = false
            private set

        private var serviceInstance: LeodgeWidgetService? = null

        fun start(context: Context) {
            val intent = Intent(context, LeodgeWidgetService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LeodgeWidgetService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * Update the notification with new portfolio data.
         * Can be called from React Native when portfolio data changes.
         */
        fun updateNotification(context: Context, totalValue: String, cash: String, invested: String, updated: String) {
            // Save to SharedPreferences for persistence
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_TOTAL_VALUE, totalValue)
                .putString(KEY_CASH, cash)
                .putString(KEY_INVESTED, invested)
                .putString(KEY_UPDATED, updated)
                .apply()

            // If service is running, update the notification immediately
            serviceInstance?.refreshNotification()
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        serviceInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_NOTIFICATION -> {
                // Update notification with latest data
                refreshNotification()
            }
            else -> {
                startForegroundService()
                scheduleWidgetUpdates()
                acquireWakeLock()
            }
        }

        // START_STICKY ensures the service restarts if killed by the system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceInstance = null
        releaseWakeLock()
        cancelWidgetUpdates()

        // Restart service if it was killed (for non-user stops)
        if (!isUserStopped()) {
            start(this)
        }
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
    }

    /**
     * Create a rich custom notification with portfolio details.
     * Uses custom RemoteViews for both collapsed and expanded states.
     */
    private fun createNotification(): Notification {
        // Get latest portfolio data
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalValue = prefs.getString(KEY_TOTAL_VALUE, null)
        val cash = prefs.getString(KEY_CASH, null)
        val invested = prefs.getString(KEY_INVESTED, null)
        val updated = prefs.getString(KEY_UPDATED, null)

        // Format display values
        val displayTotal = if (totalValue != null) "£$totalValue" else "£--.--"
        val displayCash = if (cash != null) "£$cash" else "£--.--"
        val displayInvested = if (invested != null) "£$invested" else "£--.--"
        val displayUpdated = if (updated != null) "Updated: $updated" else "Updated: --"

        // Create intent to open the app when notification is tapped
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Create stop action
        val stopIntent = Intent(this, LeodgeWidgetService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create refresh action
        val refreshIntent = Intent(this, LeodgeWidget::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build custom RemoteViews for expanded notification
        val expandedView = RemoteViews(packageName, R.layout.notification_leodge).apply {
            setTextViewText(R.id.notification_total, displayTotal)
            setTextViewText(R.id.notification_cash, displayCash)
            setTextViewText(R.id.notification_invested, displayInvested)
            setTextViewText(R.id.notification_updated, displayUpdated)
        }

        // Build custom RemoteViews for collapsed notification
        val collapsedView = RemoteViews(packageName, R.layout.notification_leodge_collapsed).apply {
            setTextViewText(R.id.notification_total_collapsed, displayTotal)
        }

        // Build the notification with custom views
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCustomContentView(collapsedView)
            .setCustomBigContentView(expandedView)
            // Actions
            .addAction(android.R.drawable.ic_menu_rotate, "Refresh", refreshPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    /**
     * Refresh the notification with current data.
     * Called when portfolio data changes.
     */
    internal fun refreshNotification() {
        if (isRunning) {
            val notification = createNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LEODGE Portfolio Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows live portfolio updates in the notification area"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleWidgetUpdates() {
        // Schedule periodic widget updates using WorkManager
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelWidgetUpdates() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_TAG)
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Leodge::WidgetWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // 10 minutes timeout, will re-acquire
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun isUserStopped(): Boolean {
        // Check SharedPreferences to determine if user explicitly stopped the service
        val prefs = getSharedPreferences("LeodgeServicePrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("user_stopped", false)
    }
}
