package com.example.myapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileWriter

class LeodgeWidgetService : Service() {

    companion object {
        const val TAG = "LeodgeWidget"
        const val CHANNEL_ID = "leodge_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.myapp.START"
        const val ACTION_STOP = "com.example.myapp.STOP"
        const val ACTION_UPDATE = "com.example.myapp.UPDATE"
        
        const val PREFS_NAME = "LeodgePrefs"
        const val KEY_TOTAL = "total_value"
        const val KEY_CASH = "cash"
        const val KEY_INVESTED = "invested"
        
        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, LeodgeWidgetService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LeodgeWidgetService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun updateData(context: Context, total: String, cash: String, invested: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_TOTAL, total)
                .putString(KEY_CASH, cash)
                .putString(KEY_INVESTED, invested)
                .apply()
            
            // If service running, update notification
            if (isRunning) {
                val intent = Intent(context, LeodgeWidgetService::class.java).apply { action = ACTION_UPDATE }
                context.startService(intent)
            }
        }
    }

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        log("Service onCreate")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("Service onStartCommand: ${intent?.action}")
        
        try {
            when (intent?.action) {
                ACTION_STOP -> {
                    log("Stopping service")
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_UPDATE -> {
                    log("Updating notification")
                    updateNotification()
                    return START_STICKY
                }
                else -> {
                    log("Starting foreground")
                    startForeground(NOTIFICATION_ID, createNotification())
                    isRunning = true
                    scheduleUpdates()
                }
            }
        } catch (e: Exception) {
            log("ERROR in onStartCommand: ${e.message}\n${Log.getStackTraceString(e)}")
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        log("Service onDestroy")
        isRunning = false
        updateRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "LEODGE", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Portfolio tracker"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val total = prefs.getString(KEY_TOTAL, "0.00") ?: "0.00"
        
        log("Creating notification with total: £$total")

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("LEODGE")
            .setContentText("Portfolio: £$total")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun updateNotification() {
        try {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
            log("Notification updated")
        } catch (e: Exception) {
            log("ERROR updating notification: ${e.message}")
        }
    }

    private fun scheduleUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    log("Scheduled update tick")
                    updateNotification()
                    handler.postDelayed(this, 60000) // Update every minute
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun log(msg: String) {
        try {
            Log.d(TAG, msg)
            // Also write to Downloads/leodge_crash.log for user access
            val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, "leodge.log")
            FileWriter(logFile, true).use { it.appendLine("${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())} $msg") }
        } catch (e: Exception) {
            Log.e(TAG, "Log failed: ${e.message}")
        }
    }
}
