# LEODGE - Trading 212 Portfolio Monitor

> React Native Android app with persistent background service for widget updates

[![Android Build](https://github.com/your-org/myapp/actions/workflows/build.yml/badge.svg)](https://github.com/your-org/myapp/actions/workflows/build.yml)

---

## 🚀 Features

- **Trading 212 API Integration** - Real-time portfolio monitoring via Trading 212's REST API
- **Android Home Screen Widget** - Persistent widget displaying current portfolio value
- **Background Service** - Foreground service keeps widget updated even when app is closed (like WhatsApp)
- **Auto-Refresh Polling** - Automatic portfolio updates every 60 seconds
- **Secure Credential Storage** - API keys stored locally using AsyncStorage
- **Dark Theme UI** - Modern dark interface with green accent colors
- **Error Handling** - Detailed error reporting and logging

---

## 📁 Project Structure

```
leodge/
├── App.tsx                      ← Root React component with UI and business logic
├── src/
│   ├── types/
│   │   └── NativeModules.d.ts   ← TypeScript definitions for native modules
│   └── utils/
│       └── Logger.tsx           ← Logging utility
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/example/myapp/
│   │   │   │   ├── MainActivity.kt          ← React Native activity
│   │   │   │   ├── MainApplication.kt      ← Application class
│   │   │   │   ├── LeodgeWidget.kt         ← AppWidgetProvider
│   │   │   │   ├── LeodgeWidgetModule.kt   ← React Native bridge
│   │   │   │   ├── LeodgeWidgetService.kt ← Foreground service for updates
│   │   │   │   ├── LeodgeBootReceiver.kt   ← Boot receiver for persistence
│   │   │   │   ├── LeodgePackage.kt        ← Native module registration
│   │   │   │   └── LeodgeLoggerModule.kt   ← Logging module
│   │   │   └── res/                         ← Android resources
│   │   └── build.gradle         ← App-level build config
│   ├── build.gradle             ← Root build config
│   └── gradle.properties        ← Gradle flags
├── index.js                     ← JS entry point
├── package.json                 ← Dependencies
└── .github/workflows/build.yml  ← CI/CD pipeline
```

---

## 🔧 Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | React Native 0.73.6 |
| Language | TypeScript 5.5 |
| JS Engine | Hermes |
| Build Tool | Gradle 8.3 with AGP 8.3 |
| Language (Native) | Kotlin |
| CI/CD | GitHub Actions |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 35 (Android 15) |

---

## 🚀 Zero Local Toolchain Required

This project is configured for **100% cloud builds** via GitHub Actions.
Every push triggers a build. Download your APK from the **Actions → Artifacts** tab.

You do **not** need Android Studio, Gradle, NDK, or `npx react-native init` locally.

---

## 📲 Getting Your APK

1. Push any commit to GitHub
2. Go to **Actions** tab → select the latest run
3. Scroll to **Artifacts** → download `myapp-debug-apk-N`
4. Unzip and install the APK on your device

---

## 🔑 Release Builds

To sign release builds, add these GitHub Secrets (`Settings → Secrets → Actions`):

| Secret | Description |
|--------|-------------|
| `RELEASE_KEYSTORE_PATH` | Path to your `.jks` keystore in the runner |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Then update the workflow to run `assembleRelease`.

---

## 🛠 Local Development (Optional)

If you want to run locally, you'll need:
- Node 20+
- Android Studio / Android SDK

```bash
npm install
npx react-native run-android
```

---

## 📱 Background Service

The app includes a persistent foreground service that keeps the widget updated even when the app is closed. This works similarly to how WhatsApp maintains its background connection.

### How It Works

1. **Foreground Service** - Runs continuously in the background with a notification
2. **API Polling** - Fetches Trading 212 data every 60 seconds
3. **Widget Updates** - Automatically updates the home screen widget with new data
4. **Boot Receiver** - Restarts the service automatically after device reboot

### Permissions Required

- `INTERNET` - For API calls to Trading 212
- `FOREGROUND_SERVICE` - To run the background service
- `POST_NOTIFICATIONS` - To show the service notification
- `WAKE_LOCK` - To keep the service running during sleep
- `RECEIVE_BOOT_COMPLETED` - To restart service after reboot

### Usage

1. Enter your Trading 212 API credentials
2. Save credentials - the background service starts automatically
3. Add the widget to your home screen
4. The widget will update every 60 seconds, even when the app is closed

To stop the background service:
- Use the "Stop Background Service" button in the app
- Or clear app data (this removes saved credentials)

---

## 📦 Bundle ID

`com.example.myapp`

---

## 📄 License

MIT
