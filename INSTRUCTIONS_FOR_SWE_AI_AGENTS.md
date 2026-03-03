# INSTRUCTIONS FOR SWE AI AGENTS
## React Native Android Boilerplate — System Context & Operating Guide

> **Read this entire file before touching any code.**
> This document is the authoritative guide for any AI coding agent, SWE tool, or autonomous system working inside this repository. It describes the architecture, the constraints, the conventions, and the step-by-step playbooks for every common class of change.

---

## 1. WHAT THIS REPO IS

This is a **bare-bones React Native Android application** generated from a Python archetype tool. It is intentionally minimal — a working Hello World that compiles, bundles, and installs on a physical Android device.

The defining characteristic of this repo is that **the entire build pipeline runs on GitHub Actions**. There is no expectation that any developer, agent, or operator has Android Studio, Gradle, the Android NDK, or even Node installed locally. Every artifact (APK) is produced in CI.

**You must never break the CI pipeline.** A change that compiles locally but breaks GitHub Actions is a broken change. A change that adds a dependency but forgets to add it to `package.json` is a broken change. The build is the source of truth.

---

## 2. REPO MAP — KNOW WHERE EVERYTHING LIVES

```
project-root/
│
├── index.js                        ← JS entry point. Registers App component.
├── App.tsx                         ← Root React component. Renders HomeScreen.
├── app.json                        ← App name consumed by AppRegistry + RN toolchain.
├── package.json                    ← ALL JS dependencies live here. No exceptions.
├── tsconfig.json                   ← TypeScript config. Path alias @ → src/.
├── babel.config.js                 ← Babel config. module-resolver maps @ alias.
├── metro.config.js                 ← Metro bundler config.
│
├── src/
│   ├── screens/
│   │   └── HomeScreen.tsx          ← The Hello World screen. Start here for UI work.
│   ├── components/                 ← Reusable UI components go here.
│   ├── hooks/                      ← Custom React hooks go here.
│   ├── utils/                      ← Pure utility functions go here.
│   └── assets/                     ← Static assets (images, fonts, etc.) go here.
│
├── android/
│   ├── build.gradle                ← ROOT Gradle config. Versions, classpath. Touch carefully.
│   ├── settings.gradle             ← Declares modules, autolinking, composite builds.
│   ├── gradle.properties           ← Build flags (Hermes, NewArch, JVM memory).
│   ├── gradlew                     ← Gradle wrapper entrypoint (chmod +x, Unix).
│   ├── gradle/wrapper/
│   │   └── gradle-wrapper.properties ← Gradle version pin. Change here to upgrade.
│   └── app/
│       ├── build.gradle            ← APP-LEVEL Gradle. SDK versions, signing, deps.
│       ├── proguard-rules.pro      ← ProGuard rules for release builds.
│       └── src/main/
│           ├── AndroidManifest.xml ← Permissions, activities, features declared here.
│           ├── java/[pkg]/
│           │   ├── MainActivity.kt     ← Android entry Activity. Minimal — don't bloat.
│           │   └── MainApplication.kt  ← App bootstrap, ReactNativeHost, SoLoader init.
│           └── res/
│               ├── values/strings.xml  ← App name string resource.
│               ├── values/styles.xml   ← App theme (NoActionBar required for RN).
│               ├── values/colors.xml   ← Color resources for launcher icon etc.
│               ├── drawable/           ← Vector drawables, launcher foreground icon.
│               └── mipmap-*/           ← Launcher icons per density.
│
└── .github/
    └── workflows/
        └── build.yml               ← THE CI PIPELINE. Triggers on every push.
```

---

## 3. CORE ARCHITECTURAL PRINCIPLES

These are non-negotiable. Do not violate them.

### 3.1 JS-first, Native-last
Write everything in TypeScript/React Native first. Only drop to Kotlin/Java native code when the React Native bridge absolutely cannot serve the requirement (e.g. background services, custom hardware sensors, Bluetooth LE). Native modules require significant additional wiring — see §7.

### 3.2 All JS dependencies go in package.json
If you add a `require()` or `import` in any `.ts/.tsx/.js` file, the package **must** be listed in `dependencies` or `devDependencies` in `package.json`. Metro will silently fail or crash during the bundle step in CI if a package is missing. There is no local `node_modules` to catch this.

### 3.3 All Android permissions go in AndroidManifest.xml
Android enforces a strict declaration model. Using a device capability (camera, location, network, bluetooth, etc.) without declaring the permission in `AndroidManifest.xml` causes a runtime crash or silent denial on device. Declare first, use second.

### 3.4 The path alias `@` maps to `src/`
`tsconfig.json` and `babel.config.js` both configure `@` as an alias for the `src/` directory. Always use:
```typescript
import HomeScreen from '@/screens/HomeScreen';   // ✅ correct
import HomeScreen from '../screens/HomeScreen';   // ❌ avoid — breaks on refactor
```

### 3.5 The build pipeline is sacred
The file `.github/workflows/build.yml` is the build system. If you need a new tool, binary, or environment variable available at build time, add it there. The pipeline steps are ordered deliberately — do not reorder them.

### 3.6 Hermes is the JS engine
`hermesEnabled=true` is set in `gradle.properties`. All JavaScript is compiled to Hermes bytecode. This means:
- No `eval()` or `Function()` constructor — Hermes does not support them.
- Some bleeding-edge ES2024+ syntax may not be supported. Stick to what `@react-native/babel-preset` transpiles.
- Hermes is faster and uses less memory than JSC. Do not disable it.

### 3.7 New Architecture is OFF
`newArchEnabled=false` in `gradle.properties`. The app uses the legacy bridge. If you are adding a third-party native module, check it supports the old architecture. Do not enable New Architecture (`newArchEnabled=true`) unless you have verified every native dependency supports Fabric + TurboModules.

---

## 4. HOW TO ADD A NEW SCREEN

**The standard pattern — 100% in JS/TS, no native changes needed.**

1. Create the screen file:
```typescript
// src/screens/ProfileScreen.tsx
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

const ProfileScreen: React.FC = () => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Profile</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  title: { fontSize: 24, fontWeight: '700' },
});

export default ProfileScreen;
```

2. Wire up navigation — install React Navigation first (see §5.1).

3. Import using the `@` alias:
```typescript
import ProfileScreen from '@/screens/ProfileScreen';
```

4. No Android-side changes needed. No Manifest changes. No Gradle changes. Push and CI builds it.

---

## 5. HOW TO ADD JS/NPM DEPENDENCIES

### 5.1 Pure JS dependencies (no native code)
Example: `lodash`, `date-fns`, `zod`, `react-navigation/core`

```bash
# 1. Add to package.json dependencies
# 2. Import and use
```

In `package.json`:
```json
"dependencies": {
  "date-fns": "^3.6.0"
}
```

Push. CI runs `npm install` which fetches it. Done.

### 5.2 Dependencies with native Android code (autolinking)
Example: `@react-native-camera-roll/camera-roll`, `react-native-permissions`, `react-native-vector-icons`

React Native 0.60+ has **autolinking**. These packages wire themselves into the Android build automatically via `settings.gradle`'s `autolinkLibrariesFromCommand()`.

Steps:
1. Add to `package.json` `dependencies`
2. If the library requires it, add permissions to `AndroidManifest.xml` (check library docs)
3. If the library has a `setup` step (e.g. fonts, gradle config), follow it in `android/app/build.gradle`
4. Push — autolinking runs during `assembleDebug` in CI

**Never manually add `implementation("...")` lines to `app/build.gradle` for RN community packages — autolinking does this automatically.**

### 5.3 React Navigation (most common case)

```json
"dependencies": {
  "@react-navigation/native": "^6.1.18",
  "@react-navigation/native-stack": "^6.11.0",
  "react-native-screens": "^3.34.0",
  "react-native-safe-area-context": "^4.11.0"
}
```

Then in `android/app/src/main/java/[pkg]/MainActivity.kt`, add:
```kotlin
import android.os.Bundle

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(null) // ← pass null, not savedInstanceState
}
```

This is the only native change React Navigation requires.

---

## 6. HOW TO ADD ANDROID PERMISSIONS

All permissions are declared in `android/app/src/main/AndroidManifest.xml`.

### 6.1 Normal permissions (auto-granted on install)
```xml
<manifest ...>
  <uses-permission android:name="android.permission.INTERNET" />          <!-- already present -->
  <uses-permission android:name="android.permission.VIBRATE" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
</manifest>
```

### 6.2 Dangerous permissions (require runtime user prompt)
These must be declared in the Manifest **and** requested at runtime in JS.

```xml
<!-- Manifest declarations -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />     <!-- API 33+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />                                            <!-- API ≤32 -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />    <!-- API 33+ -->
```

Runtime request in JS using `react-native-permissions`:
```typescript
import { request, PERMISSIONS, RESULTS } from 'react-native-permissions';

const result = await request(PERMISSIONS.ANDROID.CAMERA);
if (result === RESULTS.GRANTED) { /* proceed */ }
```

### 6.3 Hardware feature declarations
If your app requires hardware (camera, GPS, etc.), declare it so the Play Store filters correctly:
```xml
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.location.gps" android:required="false" />
```

---

## 7. HOW TO ADD A NATIVE ANDROID MODULE (KOTLIN)

Only do this when no JS-only or community solution exists. Native modules are the highest-cost change in this repo.

### 7.1 Create the module file
```
android/app/src/main/java/[your.package]/
├── modules/
│   ├── BiometricModule.kt       ← The module implementation
│   └── BiometricPackage.kt      ← The package that registers it
```

### 7.2 BiometricModule.kt pattern
```kotlin
package your.package.modules

import com.facebook.react.bridge.*

class BiometricModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "BiometricModule"   // ← JS calls NativeModules.BiometricModule

    @ReactMethod
    fun authenticate(promise: Promise) {
        try {
            // native implementation
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("AUTH_ERROR", e.message)
        }
    }
}
```

### 7.3 BiometricPackage.kt pattern
```kotlin
package your.package.modules

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class BiometricPackage : ReactPackage {
    override fun createNativeModules(ctx: ReactApplicationContext): List<NativeModule> =
        listOf(BiometricModule(ctx))
    override fun createViewManagers(ctx: ReactApplicationContext): List<ViewManager<*, *>> =
        emptyList()
}
```

### 7.4 Register in MainApplication.kt
```kotlin
override fun getPackages(): List<ReactPackage> =
    PackageList(this).packages.apply {
        add(BiometricPackage())   // ← add your package here
    }
```

### 7.5 Use in JS
```typescript
import { NativeModules } from 'react-native';
const { BiometricModule } = NativeModules;
await BiometricModule.authenticate();
```

### 7.6 Add Gradle dependencies for the native library
In `android/app/build.gradle`:
```groovy
dependencies {
    implementation("androidx.biometric:biometric:1.1.0")
    // add other AndroidX / third-party deps here
}
```

---

## 8. HOW TO ADD A CUSTOM NATIVE UI COMPONENT (ViewManager)

When a JS component cannot render something (e.g. a MapView, a Camera preview, a custom hardware-drawn surface), you need a ViewManager.

### 8.1 Create the view and manager
```kotlin
// CustomMapView.kt
class CustomMapView(context: Context) : View(context) {
    // custom drawing logic
}

// CustomMapManager.kt
class CustomMapManager : SimpleViewManager<CustomMapView>() {
    override fun getName() = "CustomMapView"
    override fun createViewInstance(ctx: ThemedReactContext) = CustomMapView(ctx)

    @ReactProp(name = "zoomLevel")
    fun setZoomLevel(view: CustomMapView, zoom: Int) {
        view.setZoom(zoom)
    }
}
```

### 8.2 Register via Package
Add `CustomMapManager()` to `createViewManagers()` in your Package class.

### 8.3 Use in JS via requireNativeComponent
```typescript
import { requireNativeComponent } from 'react-native';
const CustomMapView = requireNativeComponent('CustomMapView');

// In JSX:
<CustomMapView zoomLevel={14} style={{ flex: 1 }} />
```

---

## 9. HOW TO ADD A SECOND ANDROID ACTIVITY

React Native apps typically use a single Activity. Add a second one only for specific cases: splash screens, deeplink handlers, notification-triggered flows, full-screen video, etc.

### 9.1 Create the Activity
```kotlin
// android/app/src/main/java/[pkg]/SplashActivity.kt
package your.package

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

### 9.2 Declare in AndroidManifest.xml
```xml
<activity
    android:name=".SplashActivity"
    android:exported="true"
    android:theme="@style/SplashTheme">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- Remove the intent-filter from MainActivity when using a splash activity -->
<activity
    android:name=".MainActivity"
    android:exported="false"
    ... />
```

---

## 10. HOW TO ADD DEEPLINKS

### 10.1 AndroidManifest.xml — declare the intent filter
```xml
<activity android:name=".MainActivity" ...>
    <intent-filter android:label="@string/app_name">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- Custom scheme: myapp://profile/123 -->
        <data android:scheme="myapp" />
    </intent-filter>
    <intent-filter android:label="@string/app_name">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- HTTPS deeplink: https://myapp.com/profile/123 -->
        <data android:scheme="https" android:host="myapp.com" />
    </intent-filter>
</activity>
```

### 10.2 Handle in JS
```typescript
import { Linking } from 'react-native';

useEffect(() => {
    const sub = Linking.addEventListener('url', ({ url }) => handleDeepLink(url));
    Linking.getInitialURL().then(url => url && handleDeepLink(url));
    return () => sub.remove();
}, []);
```

---

## 11. HOW TO CONFIGURE APP SIGNING FOR RELEASE

The current pipeline builds a **debug APK** signed with a generated debug keystore. For a Play Store release build:

### 11.1 Generate your release keystore (run ONCE locally)
```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias myapp-key \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

### 11.2 Add keystore to GitHub Secrets
In your repo: **Settings → Secrets and variables → Actions**

| Secret name | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | `base64 -i release.keystore` output |
| `RELEASE_STORE_PASSWORD` | your keystore password |
| `RELEASE_KEY_ALIAS` | `myapp-key` |
| `RELEASE_KEY_PASSWORD` | your key password |

### 11.3 Add decode step in build.yml before assembleRelease
```yaml
- name: Decode release keystore
  run: |
    echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d \
      > android/app/release.keystore
    echo "RELEASE_KEYSTORE_PATH=$GITHUB_WORKSPACE/android/app/release.keystore" \
      >> $GITHUB_ENV

- name: Assemble Release APK
  run: cd android && ./gradlew assembleRelease --no-daemon
  env:
    CI: true
    RELEASE_STORE_PASSWORD: ${{ secrets.RELEASE_STORE_PASSWORD }}
    RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
    RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
```

The `android/app/build.gradle` signing config is already wired to read these env vars.

---

## 12. HOW TO UPGRADE REACT NATIVE

RN upgrades are non-trivial. Follow this order exactly.

1. Update `package.json` — bump `react-native`, `react`, and all `@react-native/*` packages to the new version together. They must match.
2. Update `android/gradle.properties` — bump `ndkVersion` to the version listed in the new RN release notes.
3. Update `android/build.gradle` — bump `kotlinVersion` and `classpath("com.android.tools.build:gradle:...")` if the RN release requires it.
4. Check the [RN upgrade helper](https://react-native-community.github.io/upgrade-helper/) for diff of all files changed between your current and target version.
5. Push to CI. Do not assume it works until CI passes.

---

## 13. COMMON GRADLE / BUILD MISTAKES — DO NOT REPEAT THESE

These have all caused build failures in this repo. The CI pipeline was fixed for each one.

| Mistake | Symptom | Fix |
|---|---|---|
| `apply plugin:` for RN root plugin | `Plugin with id 'com.facebook.react.rootproject' not found` | Use `plugins { id(...) }` block instead |
| `org.gradle.configuration-cache=true` | `external process started 'npx ...'` error | Set to `false` — RN autolinking is incompatible with config cache |
| `setup-node cache: npm` without a lock file | `Dependencies lock file is not found` | Remove `cache:` from setup-node; cache node_modules explicitly after install |
| `npm ci` without committed lock file | `npm ci` can only install with existing package-lock.json | Use `npm install` instead |
| `${{ ENV_VAR }}` in `run:` shell commands | YAML parse error: `Unrecognized named-value` | Shell vars inside `run:` blocks use `$VAR`, never `${{ VAR }}` |
| Python slice `github.sha[:7]` in YAML expr | `Unexpected symbol` error | Use `echo "${{ github.sha }}" \| cut -c1-7` in shell |
| `bundleInDebug=true` in gradle.properties | Blank grey screen on device | Explicitly run `npx react-native bundle` step before `assembleDebug` |
| Missing npm package used in babel.config.js | `Cannot find module` during Metro transform | Add the package to `devDependencies` in `package.json` |
| Single `\` in Python f-string YAML template | Multi-line shell commands collapse to one line | Use `\\` in Python source to emit a literal `\` in output |

---

## 14. TECH STACK REFERENCE CARD

| Concern | Technology | Version | Where configured |
|---|---|---|---|
| UI Framework | React Native | 0.75.4 | `package.json` |
| Language (JS) | TypeScript | 5.5 | `package.json`, `tsconfig.json` |
| Language (Native) | Kotlin | 2.0.20 | `android/build.gradle` ext block |
| JS Engine | Hermes | bundled with RN | `gradle.properties` |
| Build Tool | Gradle | 8.10.2 | `gradle-wrapper.properties` |
| Android Plugin | AGP | 8.7.3 | `android/build.gradle` classpath |
| Java Runtime | Temurin JDK | 17 | `build.yml` setup-java step |
| Bundler | Metro | 0.80.x (bundled with RN) | `metro.config.js` |
| Linting | ESLint + `@react-native` config | 8.x | `.eslintrc.js` |
| Formatting | Prettier | 3.x | `.prettierrc.json` |
| Path alias | `@` → `src/` | — | `tsconfig.json`, `babel.config.js` |
| Min Android | API 24 (Android 7.0) | — | `android/app/build.gradle` |
| Target Android | API 35 (Android 15) | — | `android/app/build.gradle` |
| CI Platform | GitHub Actions | — | `.github/workflows/build.yml` |

---

## 15. WHAT THE CI PIPELINE DOES — STEP BY STEP

Understanding this prevents wasted build minutes.

```
1. actions/checkout@v4
   └── Clones the repo into /home/runner/work/[repo]/

2. actions/setup-java@v4 (Temurin 17)
   └── Installs JDK. Required for AGP 8+. Gradle cache keyed on build files.

3. actions/setup-node@v4 (Node 20)
   └── Installs Node. No lock-file caching here (would fail on first push).

4. npm install
   └── Fetches all JS dependencies. Creates/updates package-lock.json.
       This MUST run before the cache save step.

5. actions/cache (node_modules)
   └── Saves node_modules keyed on package-lock.json hash.
       Restores on subsequent pushes if deps unchanged.

6. Download + unzip Gradle (from gradle-wrapper.properties version)
   └── Adds gradle binary to PATH. Emits GRADLE_VERSION env var.

7. cd android && gradle wrapper
   └── Generates gradle/wrapper/gradle-wrapper.jar from the installed binary.
       This is why you do NOT need to commit the .jar file.

8. actions/cache (Gradle caches)
   └── Saves ~/.gradle/caches keyed on build file hashes.

9. keytool — generate debug keystore
   └── Creates android/app/debug.keystore fresh each run.
       Debug-only. Safe. Not a secret.

10. npx react-native bundle
    └── Runs Metro bundler. Outputs:
        android/app/src/main/assets/index.android.bundle
        android/app/src/main/res/drawable-*/  (image assets)
    This step is what makes the app run without a dev server.

11. cd android && ./gradlew assembleDebug
    └── Compiles Kotlin, packages resources, links the pre-built JS bundle,
        signs with debug keystore. Outputs the APK.

12. mv APK with commit SHA in filename
    └── Makes artifacts traceable to exact commit.

13. actions/upload-artifact
    └── APK available under Actions → Artifacts for 30 days.

14. Post build summary to $GITHUB_STEP_SUMMARY
    └── Shows build metadata in the Actions UI.
```

---

## 16. THINGS YOU MUST NEVER DO

- **Never commit secrets, keystores, or API keys.** Use GitHub Secrets and env vars.
- **Never enable `newArchEnabled=true`** unless every native dependency is verified Fabric-compatible.
- **Never disable Hermes** (`hermesEnabled=false`) — Hermes bytecode compilation is what makes CI-built APKs performant on real devices.
- **Never add `implementation(...)` in `app/build.gradle`** for React Native community packages — autolinking handles this.
- **Never use `eval()` or `new Function()`** — Hermes does not support them and will crash at runtime.
- **Never use `localStorage` or `sessionStorage`** — these are browser APIs. Use `@react-native-async-storage/async-storage` instead.
- **Never import from `react-native/Libraries/...` internal paths** in your own code — internal APIs are unstable across RN versions. Only use the public API surface.
- **Never break the CI pipeline.** Every commit triggers a build. A red pipeline is not acceptable.

---

## 17. QUICK DECISION TREE FOR COMMON TASKS

```
I need to...
│
├── Add a new screen
│   └── Create src/screens/MyScreen.tsx → wire into navigator → push
│
├── Add a UI component
│   └── Create src/components/MyComponent.tsx → import with @ alias → push
│
├── Add a JS-only library (lodash, zod, axios...)
│   └── Add to package.json dependencies → push
│
├── Add a RN community library with native code
│   └── Add to package.json → add Manifest permissions if needed → push
│       (autolinking handles the rest)
│
├── Add a device permission (camera, location...)
│   └── Add <uses-permission> to AndroidManifest.xml
│       → add runtime request in JS via react-native-permissions → push
│
├── Add custom native Kotlin code
│   └── Create Module + Package in android/app/src/main/java/[pkg]/modules/
│       → register in MainApplication.kt → use via NativeModules in JS → push
│
├── Add a second Activity
│   └── Create Activity.kt → declare in AndroidManifest.xml → push
│
├── Change the app icon
│   └── Replace drawables in android/app/src/main/res/mipmap-*/
│       and res/drawable/ic_launcher_foreground.xml → push
│
├── Change the app name
│   └── Edit android/app/src/main/res/values/strings.xml
│       AND app.json → push
│
├── Change min/target SDK version
│   └── Edit minSdkVersion / targetSdkVersion in android/build.gradle ext block → push
│
├── Change Gradle version
│   └── Edit distributionUrl in android/gradle/wrapper/gradle-wrapper.properties → push
│
└── Build a release APK for the Play Store
    └── Set up keystore secrets (see §11) → update build.yml to run assembleRelease → push
```

---

*This document was generated alongside the project archetype. Keep it updated as the project evolves. An AI agent that follows this guide will produce correct, buildable, CI-passing changes on the first attempt.*
