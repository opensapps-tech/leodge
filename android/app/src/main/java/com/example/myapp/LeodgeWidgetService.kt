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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * LeodgeWidgetService - Foreground service that keeps the widget always active.
 *
 * This service runs continuously in the foreground with a persistent notification,
 * similar to how WhatsApp keeps its connection alive. It ensures the widget
 * continues to receive updates even when the app is closed or the device is dozing.
 */
class LeodgeWidgetService : Service() {

    companion object {
        const val TAG = "LeodgeWidgetService"
        const val CHANNEL_ID = "leodge_widget_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.myapp.ACTION_START_SERVICE"
        const val ACTION_STOP = "com.example.myapp.ACTION_STOP_SERVICE"
        const val WORK_TAG = "leodge_widget_update_work"

        // Update interval - 15 minutes (minimum for WorkManager periodic work)
        const val UPDATE_INTERVAL_MINUTES = 15L

        @Volatile
        var isRunning = false
            private set

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
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
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

    private fun createNotification(): Notification {
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LEODGE Portfolio Monitor")
            .setContentText("Widget is actively monitoring your portfolio")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LEODGE Widget Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the portfolio widget updated in real-time"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
