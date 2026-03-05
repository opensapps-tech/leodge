package com.example.myapp

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * LeodgeWidgetService - Foreground service for continuous widget updates
 * 
 * This service runs in the background to keep the widget updated even when
 * the app is closed, similar to how WhatsApp maintains its background connection.
 * 
 * The service:
 * - Polls Trading 212 API every 60 seconds
 * - Updates the widget with the latest portfolio data
 * - Shows a persistent notification (required for foreground services)
 */
class LeodgeWidgetService : Service() {

    companion object {
        private const val TAG = "LeodgeWidgetService"
        private const val CHANNEL_ID = "leodge_widget_channel"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 15000L // 15 seconds
        private const val PREFS_NAME = "LeodgeWidgetPrefs"
        private const val PREFS_CREDENTIALS = "LeodgeCredentials"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_TOTAL_VALUE = "total_value"
        private const val KEY_CASH = "cash"
        private const val KEY_INVESTED = "invested"
        private const val KEY_UPDATED = "updated"
        private const val KEY_SERVICE_RUNNING = "service_running"

        fun startService(context: Context, apiKey: String, apiSecret: String) {
            Log.d(TAG, "Starting service...")

            // Save credentials and mark service as running
            val prefs = context.getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_API_SECRET, apiSecret)
                .putBoolean(KEY_SERVICE_RUNNING, true)
                .commit() // Use commit() to ensure it's written before starting service

            val intent = Intent(context, LeodgeWidgetService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
                Log.d(TAG, "Started foreground service")
            } else {
                context.startService(intent)
                Log.d(TAG, "Started service (pre-O)")
            }
        }

        fun stopService(context: Context) {
            Log.d(TAG, "Stopping service...")

            // Clear service running flag but keep credentials
            val prefs = context.getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_SERVICE_RUNNING, false)
                .commit()

            val intent = Intent(context, LeodgeWidgetService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Service stop command sent")
        }

        fun isServiceRunning(context: Context): Boolean {
            // Check if service is marked as running
            val prefs = context.getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            val isMarkedRunning = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
            Log.d(TAG, "Service running flag: $isMarkedRunning")
            
            // Try to verify service is actually alive by checking notification
            if (isMarkedRunning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val activeNotifications = notificationManager?.activeNotifications
                    val hasNotification = activeNotifications?.any { it.id == NOTIFICATION_ID } == true
                    Log.d(TAG, "Service notification check: $hasNotification")
                    return hasNotification == true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to verify service status", e)
                    // If we can't verify, return the flag value
                    return isMarkedRunning
                }
            }
            return isMarkedRunning
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        try {
            createNotificationChannel()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with startId: $startId")
        
        if (!isRunning) {
            isRunning = true
            try {
                Log.d(TAG, "Starting foreground service...")
                startForeground(NOTIFICATION_ID, createNotification())
                startPolling()
                Log.d(TAG, "Foreground service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                // If we fail to start foreground, stop the service
                isRunning = false
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            Log.d(TAG, "Service already running, ignoring start command")
        }
        
        // START_STICKY ensures the service is restarted if killed by the system
        Log.d(TAG, "Returning START_STICKY")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed, but service continues running")
        // Keep service running even when app is removed from recent tasks
        // This is key for "always active" behavior
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        // Stop polling
        stopPolling()
        isRunning = false
        
        // Update service running flag
        val prefs = getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SERVICE_RUNNING, false)
            .apply()
        
        // Remove notification
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove notification", e)
        }
        
        Log.d(TAG, "Service cleanup complete")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep widget updated with latest portfolio data"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = try {
            packageManager.getLaunchIntentForPackage(packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get launch intent", e)
            null
        }
        
        val pendingIntent = try {
            intent?.let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create pending intent", e)
            null
        }

        return try {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LEODGE Widget Active")
                .setContentText("Portfolio monitoring running in background")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
            
            // Only set content intent if we successfully created one
            pendingIntent?.let {
                builder.setContentIntent(it)
            }
            
            builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification", e)
            // Create a simple notification if the main one fails
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LEODGE")
                .setContentText("Widget service running")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    private fun startPolling() {
        pollingRunnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    Log.d(TAG, "Polling cycle started")
                    fetchPortfolioData()
                    Log.d(TAG, "Scheduling next poll in ${POLL_INTERVAL_MS}ms")
                    handler.postDelayed(this, POLL_INTERVAL_MS)
                } else {
                    Log.d(TAG, "Polling stopped, not scheduling next poll")
                }
            }
        }
        pollingRunnable?.let {
            Log.d(TAG, "Posting initial polling task")
            handler.post(it)
        }
    }

    private fun stopPolling() {
        pollingRunnable?.let {
            Log.d(TAG, "Removing polling callbacks")
            handler.removeCallbacks(it)
            pollingRunnable = null
        }
    }

    private fun fetchPortfolioData() {
        try {
            val prefs = getSharedPreferences(PREFS_CREDENTIALS, Context.MODE_PRIVATE)
            val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
            val apiSecret = prefs.getString(KEY_API_SECRET, "") ?: ""

            if (apiKey.isEmpty() || apiSecret.isEmpty()) {
                Log.w(TAG, "No credentials available, stopping service")
                stopSelf()
                return
            }

            val credentials = "$apiKey:$apiSecret"
            val authHeader = "Basic ${android.util.Base64.encodeToString(
                credentials.toByteArray(),
                android.util.Base64.NO_WRAP
            )}"

            val url = URL("https://live.trading212.com/api/v0/equity/account/summary")
            val connection = url.openConnection() as HttpsURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", authHeader)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "API Response Code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonResponse = response.toString()
                Log.d(TAG, "API Response: $jsonResponse")

                val json = JSONObject(jsonResponse)
                val total = json.optDouble("totalValue", json.optDouble("total", 0.0))
                val cashObj = json.optJSONObject("cash")
                val cash = cashObj?.optDouble("availableToTrade", 0.0) ?: 0.0
                val investmentsObj = json.optJSONObject("investments")
                val invested = investmentsObj?.optDouble("currentValue", 0.0) ?: 0.0

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val updated = sdf.format(Date())

                // Save to widget preferences
                val widgetPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                widgetPrefs.edit()
                    .putString(KEY_TOTAL_VALUE, String.format("%.2f", total))
                    .putString(KEY_CASH, String.format("%.2f", cash))
                    .putString(KEY_INVESTED, String.format("%.2f", invested))
                    .putString(KEY_UPDATED, updated)
                    .apply()

                // Update widget
                updateWidget()
                
                Log.d(TAG, "Widget updated: Total=£${String.format("%.2f", total)}")
            } else {
                Log.e(TAG, "API Error: $responseCode")
            }

            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch portfolio data", e)
        }
    }

    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, LeodgeWidget::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)

            val intent = Intent(this, LeodgeWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
            
            Log.d(TAG, "Widget update broadcast sent for ${ids.size} widget(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget", e)
        }
    }
}
