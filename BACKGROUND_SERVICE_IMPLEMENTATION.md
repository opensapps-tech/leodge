# Persistent Background Service Implementation

## Overview
This document describes the implementation of a persistent background service for the LEODGE Trading 212 Portfolio Monitor that keeps the widget always active, similar to how WhatsApp maintains its background connection.

**Note**: The background service polls the API every 15 seconds for near real-time updates.

## Architecture

### Components

1. **LeodgeWidgetService.kt** - Foreground Service
   - Runs continuously in the background
   - Polls Trading 212 API every 60 seconds
   - Updates the widget with new data
   - Shows a persistent notification (required for foreground services)
   - Uses `START_STICKY` to ensure automatic restart if killed by the system

2. **LeodgeWidgetModule.kt** - React Native Bridge
   - Exposes methods to start/stop the background service from JavaScript
   - Methods:
     - `startBackgroundService(apiKey, apiSecret)` - Starts the service
     - `stopBackgroundService()` - Stops the service
     - `isBackgroundServiceRunning()` - Checks service status

3. **LeodgeBootReceiver.kt** - Boot Receiver
   - Broadcast receiver that listens for `BOOT_COMPLETED` events
   - Automatically restarts the background service after device reboot
   - Only starts the service if credentials are saved (service was previously running)

4. **AndroidManifest.xml** - Configuration
   - Added required permissions:
     - `FOREGROUND_SERVICE` - Required for foreground services
     - `POST_NOTIFICATIONS` - Required for showing notifications
     - `WAKE_LOCK` - Prevents device sleep during operation
     - `RECEIVE_BOOT_COMPLETED` - For boot receiver to work
   - Registered the service, receiver, and permissions

5. **App.tsx** - UI Integration
   - Added background service control section (Android only)
   - Shows service status (Running/Stopped)
   - Allows users to start/stop the service
   - Automatically starts service when credentials are saved
   - Checks service status on app launch

## How It Works

### Service Lifecycle

1. **Initial Start**
   - User saves API credentials in the app
   - App calls `LeodgeWidgetModule.startBackgroundService()`
   - Native module creates and starts the foreground service
   - Service creates notification channel and shows persistent notification
   - Service begins polling API every 60 seconds

2. **Running State**
   - Service runs in background with `START_STICKY` flag
   - Every 60 seconds:
     - Fetches portfolio data from Trading 212 API
     - Saves data to SharedPreferences
     - Broadcasts update intent to widget
     - Widget updates its UI with new data
   - This continues even if the app is swiped away or closed

3. **App Closed/Background**
   - Foreground service continues running
   - Widget continues to update
   - Persistent notification remains visible in notification shade
   - Tapping notification opens the app

4. **Device Reboot**
   - `LeodgeBootReceiver` receives `BOOT_COMPLETED` broadcast
   - Checks if credentials exist (service was running before reboot)
   - If yes, restarts the foreground service automatically
   - Service resumes normal polling behavior

5. **Service Stop**
   - User taps "Stop Background Service" button
   - App calls `LeodgeWidgetModule.stopBackgroundService()`
   - Native module calls `context.stopService()`
   - Service stops polling and removes notification
   - Credentials are cleared from service preferences
   - Widget stops updating (shows last known value)

### Persistence Mechanisms

1. **START_STICKY**
   - Tells Android to restart the service if it's killed by the system
   - Service recreates itself with last intent (if available)

2. **Boot Receiver**
   - Ensures service starts after device reboot
   - Only starts if credentials exist (prevents auto-start on fresh install)

3. **Foreground Service**
   - Higher priority than background services
   - Less likely to be killed by the system
   - Required to show persistent notification

4. **WAKE_LOCK**
   - Prevents CPU from sleeping during API calls
   - Ensures polling happens on schedule

## User Experience

### UI Changes

1. **Background Service Section** (new)
   - Shows service status with green/red indicator
   - "Start/Stop Background Service" button
   - Description text explaining the feature
   - Only visible on Android

2. **Persistent Notification**
   - Shows "LEODGE Widget Active" when service is running
   - Text: "Portfolio monitoring running in background"
   - Tapping opens the app
   - Cannot be dismissed (ongoing notification)
   - Low priority (doesn't interrupt user)

### Behavior

- **First Launch**: Service starts automatically when credentials are saved
- **App Reopen**: Service status is checked and displayed
- **Widget**: Updates every 60 seconds regardless of app state
- **Battery**: Minimal impact - network call every 60 seconds
- **Data Usage**: Very low - small JSON payload every 60 seconds

## Benefits

1. **Always-Updated Widget**
   - Widget stays fresh without opening the app
   - Similar experience to WhatsApp's persistent connection

2. **Reliability**
   - Service restarts automatically if killed
   - Survives app closure and device reboot
   - Uses multiple persistence mechanisms

3. **User Control**
   - Users can stop the service anytime
   - Clear visual feedback on service status
   - No surprises - notification is always visible

4. **Battery Efficient**
   - Simple polling interval (60 seconds)
   - No continuous wake locks
   - Uses system networking stack efficiently

## Technical Details

### API Polling

```kotlin
// Every 60 seconds
val handler = Handler(Looper.getMainLooper())
val pollingRunnable = object : Runnable {
    override fun run() {
        if (isRunning) {
            fetchPortfolioData()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }
}
```

### Widget Update

```kotlin
// After fetching data, update widget
val appWidgetManager = AppWidgetManager.getInstance(this)
val componentName = ComponentName(this, LeodgeWidget::class.java)
val ids = appWidgetManager.getAppWidgetIds(componentName)

val intent = Intent(this, LeodgeWidget::class.java).apply {
    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
}
sendBroadcast(intent)
```

### Credential Persistence

Service stores credentials separately from app's AsyncStorage:
- App uses `AsyncStorage` for UI
- Service uses `SharedPreferences` for background operation
- Both stay in sync via React Native bridge

## Security Considerations

1. **Credential Storage**
   - API keys stored in SharedPreferences (private mode)
   - Only accessible to the app
   - Cleared when service is stopped

2. **Network Security**
   - Uses HTTPS for API calls
   - Basic Auth with Base64 encoding
   - Credentials never sent over HTTP

3. **Service Permissions**
   - Service is not exported (cannot be accessed by other apps)
   - Foreground service type: `dataSync`

## Troubleshooting

### Service Not Starting
- Check that credentials are saved
- Verify app has necessary permissions
- Check device battery optimization settings
- Ensure app is not restricted by OEM battery savers

### Widget Not Updating
- Check service status in app
- Verify widget is added to home screen
- Check notification shade for service notification
- Restart app if needed

### Service Stops After Reboot
- Verify `RECEIVE_BOOT_COMPLETED` permission
- Check if app has boot permission enabled
- Verify credentials are still saved
- Check system log for errors

## Future Enhancements

1. **Adaptive Polling**
   - Increase interval during night hours
   - Decrease interval when market is open
   - Skip polling on weekends

2. **Smart Wake-Up**
   - Use WorkManager for more efficient scheduling
   - Support for Doze mode and App Standby

3. **Battery Optimization**
   - Detect when device is on WiFi vs cellular
   - Adaptive polling based on battery level
   - Option to pause when battery is low

4. **Widget Enhancements**
   - Add tap to refresh
   - Show percentage change
   - Multiple widget sizes
