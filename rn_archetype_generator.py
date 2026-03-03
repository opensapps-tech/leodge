#!/usr/bin/env python3
"""
╔═══════════════════════════════════════════════════════════════╗
║     React Native Android Archetype Generator v1.0             ║
║     Generates a fully wired bare-bones RN Android project     ║
║     with GitHub Actions cloud build pipeline                   ║
╚═══════════════════════════════════════════════════════════════╝

No Android Studio, no local Gradle, no local Node required.
Just push to GitHub and get your APK back.
"""

import os
import sys
import stat
import re
import textwrap
from pathlib import Path

# ─── ANSI Colors ─────────────────────────────────────────────────────────────
GREEN  = '\033[92m'
BLUE   = '\033[94m'
YELLOW = '\033[93m'
RED    = '\033[91m'
CYAN   = '\033[96m'
BOLD   = '\033[1m'
DIM    = '\033[2m'
RESET  = '\033[0m'


def clr(text, *codes):
    return ''.join(codes) + text + RESET


def print_banner():
    print(clr("""
  ██████╗ ███╗   ██╗      █████╗ ██████╗  ██████╗██╗  ██╗
  ██╔══██╗████╗  ██║     ██╔══██╗██╔══██╗██╔════╝██║  ██║
  ██████╔╝██╔██╗ ██║     ███████║██████╔╝██║     ███████║
  ██╔══██╗██║╚██╗██║     ██╔══██║██╔══██╗██║     ██╔══██║
  ██║  ██║██║ ╚████║     ██║  ██║██║  ██║╚██████╗██║  ██║
  ╚═╝  ╚═╝╚═╝  ╚═══╝     ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝
  React Native Android Archetype Generator
  ─────────────────────────────────────────────────────────
  No local toolchain needed · GitHub Actions builds your APK
""", CYAN, BOLD))


def print_step(msg):
    print(f"\n{clr('▶', CYAN, BOLD)} {clr(msg, BOLD)}")


def print_ok(msg):
    print(f"  {clr('✓', GREEN, BOLD)} {msg}")


def print_file(path):
    print(f"  {clr('📄', '', '')} {clr(path, DIM)}")


def ask(question, default=None, validator=None):
    while True:
        default_hint = f" {clr(f'[{default}]', YELLOW)}" if default else ""
        raw = input(f"\n{clr('?', CYAN, BOLD)} {clr(question, BOLD)}{default_hint}: ").strip()
        value = raw if raw else default
        if not value:
            print(clr("  This field is required.", RED))
            continue
        if validator and not validator(value):
            continue
        return value


def ask_choice(question, choices, default=None):
    print(f"\n{clr('?', CYAN, BOLD)} {clr(question, BOLD)}")
    for i, c in enumerate(choices, 1):
        marker = clr("●", CYAN, BOLD) if c == default else clr("○", DIM)
        print(f"  {marker} {i}. {c}")
    while True:
        hint = f" [{choices.index(default)+1}]" if default else ""
        raw = input(f"  {clr(f'Enter number{hint}:', BLUE)} ").strip()
        if not raw and default:
            return default
        try:
            idx = int(raw) - 1
            if 0 <= idx < len(choices):
                return choices[idx]
        except ValueError:
            pass
        print(clr("  Invalid choice. Please enter a number.", RED))


def validate_app_name(name):
    if not re.match(r'^[A-Z][A-Za-z0-9]{1,29}$', name):
        print(clr("  Must start with a capital letter, alphanumeric only, 2-30 chars. e.g. MyApp", RED))
        return False
    return True


def validate_package(pkg):
    if not re.match(r'^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*){1,}$', pkg):
        print(clr("  Must be lowercase dot-notation with 2+ segments e.g. com.acme.myapp", RED))
        return False
    return True


def validate_gh_repo(repo):
    if not re.match(r'^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$', repo):
        print(clr("  Must be owner/repo format e.g. acmecorp/myapp", RED))
        return False
    return True


def pkg_to_path(pkg):
    return pkg.replace('.', '/')


def make_executable(path):
    st = os.stat(path)
    os.chmod(path, st.st_mode | stat.S_IEXEC | stat.S_IXGRP | stat.S_IXOTH)


def write(base, rel_path, content):
    full = os.path.join(base, rel_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    print_file(rel_path)
    return full


# ─────────────────────────────────────────────────────────────────────────────
#   FILE TEMPLATES
# ─────────────────────────────────────────────────────────────────────────────

def tpl_package_json(app_name, bundle_id, pkg_mgr):
    lock = "npm ci" if pkg_mgr == "npm" else "yarn install --frozen-lockfile"
    return f"""\
{{
  "name": "{app_name.lower()}",
  "version": "1.0.0",
  "private": true,
  "scripts": {{
    "android": "react-native run-android",
    "start": "react-native start",
    "test": "jest",
    "lint": "eslint src --ext .ts,.tsx",
    "type-check": "tsc --noEmit",
    "clean": "react-native clean"
  }},
  "dependencies": {{
    "react": "18.3.1",
    "react-native": "0.75.4"
  }},
  "devDependencies": {{
    "@babel/core": "^7.25.0",
    "@babel/preset-env": "^7.25.0",
    "@babel/runtime": "^7.25.0",
    "@react-native/babel-preset": "0.75.4",
    "@react-native/eslint-config": "0.75.4",
    "@react-native/metro-config": "0.75.4",
    "@react-native/typescript-config": "0.75.4",
    "@types/react": "^18.3.0",
    "@types/react-test-renderer": "^18.3.0",
    "babel-jest": "^29.7.0",
    "eslint": "^8.57.0",
    "jest": "^29.7.0",
    "prettier": "^3.3.0",
    "react-test-renderer": "18.3.1",
    "typescript": "^5.5.0"
  }},
  "jest": {{
    "preset": "react-native"
  }},
  "engines": {{
    "node": ">=20"
  }}
}}
"""


def tpl_tsconfig():
    return """\
{
  "extends": "@react-native/typescript-config/tsconfig.json",
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "strict": true
  },
  "include": ["src", "index.js", "App.tsx", "*.ts", "*.tsx"]
}
"""


def tpl_babel_config():
    return """\
module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [
    [
      'module-resolver',
      {
        root: ['./src'],
        extensions: ['.ios.js', '.android.js', '.js', '.ts', '.tsx', '.json'],
        alias: {
          '@': './src',
        },
      },
    ],
  ],
};
"""


def tpl_metro_config():
    return """\
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 */
const config = {};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
"""


def tpl_index_js(app_name):
    return f"""\
/**
 * React Native App Entry Point
 * @format
 */

import {{AppRegistry}} from 'react-native';
import App from './App';
import {{name as appName}} from './app.json';

AppRegistry.registerComponent(appName, () => App);
"""


def tpl_app_json(app_name):
    return f"""\
{{
  "name": "{app_name}",
  "displayName": "{app_name}"
}}
"""


def tpl_app_tsx(app_name):
    return f"""\
/**
 * Root Application Component
 * @format
 */

import React from 'react';
import {{
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
}} from 'react-native';

import {{
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
}} from 'react-native/Libraries/NewAppScreen';

import HomeScreen from '@/screens/HomeScreen';

function App(): React.JSX.Element {{
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {{
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
    flex: 1,
  }};

  return (
    <SafeAreaView style={{backgroundStyle}}>
      <StatusBar
        barStyle={{isDarkMode ? 'light-content' : 'dark-content'}}
        backgroundColor={{backgroundStyle.backgroundColor}}
      />
      <HomeScreen />
    </SafeAreaView>
  );
}}

export default App;
"""


def tpl_home_screen(app_name):
    return f"""\
import React from 'react';
import {{
  ScrollView,
  StyleSheet,
  Text,
  useColorScheme,
  View,
}} from 'react-native';
import {{Colors}} from 'react-native/Libraries/NewAppScreen';

const HomeScreen: React.FC = () => {{
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <ScrollView
      contentInsetAdjustmentBehavior="automatic"
      style={{styles.scrollView}}>
      <View style={{styles.container}}>
        <View style={{styles.headerContainer}}>
          <Text style={{[styles.title, {{color: isDarkMode ? Colors.white : Colors.black}}]}}>
            👋 Hello, {app_name}!
          </Text>
          <Text style={{[styles.subtitle, {{color: isDarkMode ? Colors.light : Colors.dark}}]}}>
            Your React Native Android app is live.
          </Text>
        </View>

        <View style={{[styles.card, {{backgroundColor: isDarkMode ? Colors.darker : Colors.lighter}}]}}>
          <Text style={{[styles.cardTitle, {{color: isDarkMode ? Colors.white : Colors.black}}]}}>
            🚀 Built on GitHub Actions
          </Text>
          <Text style={{[styles.cardBody, {{color: isDarkMode ? Colors.light : Colors.dark}}]}}>
            Every push to your repository triggers a cloud build.
            No local toolchain required.
          </Text>
        </View>

        <View style={{[styles.card, {{backgroundColor: isDarkMode ? Colors.darker : Colors.lighter}}]}}>
          <Text style={{[styles.cardTitle, {{color: isDarkMode ? Colors.white : Colors.black}}]}}>
            📱 Optimized for Modern Devices
          </Text>
          <Text style={{[styles.cardBody, {{color: isDarkMode ? Colors.light : Colors.dark}}]}}>
            Targets Android 14 (API 34). Runs great on Samsung Galaxy
            S24 Ultra, S25 Ultra and all modern flagship devices.
          </Text>
        </View>
      </View>
    </ScrollView>
  );
}};

const styles = StyleSheet.create({{
  scrollView: {{
    flex: 1,
  }},
  container: {{
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 40,
    paddingBottom: 32,
    gap: 20,
  }},
  headerContainer: {{
    alignItems: 'center',
    paddingVertical: 32,
    gap: 8,
  }},
  title: {{
    fontSize: 32,
    fontWeight: '700',
    letterSpacing: -0.5,
    textAlign: 'center',
  }},
  subtitle: {{
    fontSize: 16,
    fontWeight: '400',
    textAlign: 'center',
    opacity: 0.7,
  }},
  card: {{
    borderRadius: 16,
    padding: 20,
    gap: 8,
    shadowColor: '#000',
    shadowOffset: {{width: 0, height: 2}},
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  }},
  cardTitle: {{
    fontSize: 17,
    fontWeight: '600',
    letterSpacing: -0.2,
  }},
  cardBody: {{
    fontSize: 14,
    lineHeight: 22,
    opacity: 0.75,
  }},
}});

export default HomeScreen;
"""


def tpl_gitignore():
    return """\
# OSX
.DS_Store

# Xcode
build/
*.pbxuser
!default.pbxuser
*.mode1v3
!default.mode1v3
*.mode2v3
!default.mode2v3
*.perspectivev3
!default.perspectivev3
xcuserdata
*.xccheckout
*.moved-aside
DerivedData
*.hmap
*.ipa
*.xcuserstate
ios/.xcode.env.local

# Android/IntelliJ
build/
.idea
.gradle
local.properties
*.iml
*.hprof
.cxx
*.keystore
!debug.keystore

# Node
node_modules/
npm-debug.log
yarn-error.log

# Watchman
.watchmanconfig

# Fastlane
*/fastlane/report.xml
*/fastlane/Preview.html
*/fastlane/screenshots
*/fastlane/test_output

# Bundle artifact
*.jsbundle

# Ruby / CocoaPods
/ios/Pods/
/vendor/bundle/

# Temporary files created by Metro
.metro-health-check*

# Testing
/coverage

# Environment
.env
.env.local
.env.production

# Editor
.vscode/settings.json
.idea/
*.swp
*.swo
"""


def tpl_prettierrc():
    return """\
{
  "arrowParens": "avoid",
  "bracketSameLine": true,
  "bracketSpacing": false,
  "singleQuote": true,
  "trailingComma": "all"
}
"""


def tpl_eslintrc():
    return """\
module.exports = {
  root: true,
  extends: ['@react-native'],
  rules: {
    'prettier/prettier': [
      'error',
      {
        arrowParens: 'avoid',
        bracketSameLine: true,
        bracketSpacing: false,
        singleQuote: true,
        trailingComma: 'all',
      },
    ],
  },
};
"""


# ─── ANDROID FILES ───────────────────────────────────────────────────────────

def tpl_android_root_build_gradle():
    return """\
buildscript {
    ext {
        buildToolsVersion = "35.0.0"
        minSdkVersion    = 24
        compileSdkVersion = 35
        targetSdkVersion  = 35
        ndkVersion        = "27.1.12297006"
        kotlinVersion     = "2.0.20"
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

apply plugin: "com.facebook.react.rootproject"
"""


def tpl_android_settings_gradle(app_name):
    return f"""\
pluginManagement {{
    includeBuild("../node_modules/@react-native/gradle-plugin")
    repositories {{
        google()
        mavenCentral()
        gradlePluginPortal()
    }}
}}

plugins {{
    id("com.facebook.react.settings")
}}

extensions.configure(com.facebook.react.ReactSettingsExtension) {{ ex ->
    ex.autolinkLibrariesFromCommand()
}}

rootProject.name = '{app_name}'
include ':app'
includeBuild('../node_modules/@react-native/gradle-plugin')
"""


def tpl_app_build_gradle(bundle_id):
    return f"""\
apply plugin: "com.android.application"
apply plugin: "org.jetbrains.kotlin.android"
apply plugin: "com.facebook.react"

/**
 * React Native build configuration.
 */
react {{
    autolinkLibrariesWithApp()
}}

android {{
    ndkVersion        rootProject.ext.ndkVersion
    buildToolsVersion rootProject.ext.buildToolsVersion
    compileSdk        rootProject.ext.compileSdkVersion

    namespace "{bundle_id}"

    defaultConfig {{
        applicationId   "{bundle_id}"
        minSdk          rootProject.ext.minSdkVersion
        targetSdk       rootProject.ext.targetSdkVersion
        versionCode     1
        versionName     "1.0.0"
    }}

    signingConfigs {{
        debug {{
            storeFile     file('debug.keystore')
            storePassword 'android'
            keyAlias      'androiddebugkey'
            keyPassword   'android'
        }}
        release {{
            // CI: keystore injected from GitHub Secrets via env vars
            if (System.getenv("RELEASE_KEYSTORE_PATH")) {{
                storeFile     file(System.getenv("RELEASE_KEYSTORE_PATH"))
                storePassword System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias      System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword   System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }} else {{
                // Fallback to debug keystore if secrets not set
                storeFile     file('debug.keystore')
                storePassword 'android'
                keyAlias      'androiddebugkey'
                keyPassword   'android'
            }}
        }}
    }}

    buildTypes {{
        debug {{
            debuggable          true
            signingConfig       signingConfigs.debug
            applicationIdSuffix ".debug"
        }}
        release {{
            signingConfig       signingConfigs.release
            minifyEnabled       false
            proguardFiles       getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"
        }}
    }}

    compileOptions {{
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }}

    kotlinOptions {{
        jvmTarget = '17'
    }}
}}

dependencies {{
    // Core React Native (version managed by RN Gradle Plugin)
    implementation("com.facebook.react:react-android")

    if (hermesEnabled.toBoolean()) {{
        implementation("com.facebook.react:hermes-android")
    }} else {{
        implementation jscFlavor
    }}
}}
"""


def tpl_gradle_properties():
    return """\
# Project-wide Gradle settings

# JVM memory allocation
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError

# Enable Gradle Daemon
org.gradle.daemon=false

# Enable parallel builds
org.gradle.parallel=true

# Enable configuration cache (Gradle 8+)
org.gradle.configuration-cache=true

# AndroidX
android.useAndroidX=true

# React Native
newArchEnabled=false
hermesEnabled=true

# Kotlin Code Style
kotlin.code.style=official
"""


def tpl_gradle_wrapper_properties(gradle_version="8.10.2"):
    return f"""\
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-{gradle_version}-bin.zip
networkTimeout=10000
validateDistributionUrl=true
"""


def tpl_gradlew():
    """Standard Gradle wrapper shell script (unix)."""
    return r"""#!/bin/sh
#
# Copyright © 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Gradle startup script for UN*X
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
        /*)   app_path=$link ;; #(
        *)    app_path=$APP_HOME$link ;;
    esac
done

APP_HOME=$( cd "${APP_HOME:-./}" && pwd -P ) || exit

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD=maximum

warn () {
    echo "$*"
} >&2

die () {
    echo
    echo "$*"
    echo
    exit 1
} >&2

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$( uname )" in              #(
    CYGWIN* )         cygwin=true  ;; #(
    Darwin* )         darwin=true  ;; #(
    MSYS* | MINGW* )  msys=true    ;; #(
    NONSTOP* )        nonstop=true ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar


# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=$JAVA_HOME/jre/sh/java
    else
        JAVACMD=$JAVA_HOME/bin/java
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD=java
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if ! "$cygwin" && ! "$darwin" && ! "$nonstop" ; then
    case $MAX_FD in #(
        max*)
            # In POSIX sh, ulimit -H is undefined. That's why the result is checked to see if it worked.
            # shellcheck disable=SC2039,SC3045
            MAX_FD=$( ulimit -H -n ) ||
                warn "Could not query maximum file descriptor limit"
            ;;
    esac
    case $MAX_FD in  #(
        '' | soft) :;; #(
        *)
            # In POSIX sh, ulimit -n is undefined. That's why the result is checked to see if it worked.
            # shellcheck disable=SC2039,SC3045
            ulimit -n "$MAX_FD" ||
                warn "Could not set maximum file descriptor limit to $MAX_FD"
    esac
fi

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$@"

exec "$JAVACMD" "$@"
"""


def tpl_gradlew_bat():
    """Standard Gradle wrapper batch script (Windows)."""
    return r"""@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% neq 0 goto mainEnd

if "%GRADLE_EXIT_CONSOLE%" == "" goto mainEnd

exit %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
"""


def tpl_android_manifest(app_name, bundle_id):
    return f"""\
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MainApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:allowBackup="false"
        android:theme="@style/AppTheme"
        android:supportsRtl="true">

        <activity
            android:name=".MainActivity"
            android:label="@string/app_name"
            android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode"
            android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
"""


def tpl_main_activity(bundle_id, app_name):
    return f"""\
package {bundle_id}

import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

/**
 * MainActivity - Entry point of the React Native Android application.
 *
 * Extends ReactActivity which sets up the React Native bridge
 * and renders the JavaScript bundle.
 */
class MainActivity : ReactActivity() {{

    /**
     * Returns the name of the main component registered in JavaScript.
     * Used to schedule rendering of the component.
     */
    override fun getMainComponentName(): String = "{app_name}"

    /**
     * Creates the React Activity Delegate.
     * Fabric (new renderer) is enabled via fabricEnabled flag from gradle.properties.
     */
    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}}
"""


def tpl_main_application(bundle_id):
    return f"""\
package {bundle_id}

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader

/**
 * MainApplication - Application class that bootstraps React Native.
 *
 * Sets up the ReactNativeHost which manages the JS engine (Hermes)
 * and the React Native bridge.
 */
class MainApplication : Application(), ReactApplication {{

    override val reactNativeHost: ReactNativeHost =
        object : DefaultReactNativeHost(this) {{
            override fun getPackages(): List<ReactPackage> =
                PackageList(this).packages
                // Add your custom native packages here if needed:
                // packages.add(MyReactNativePackage())

            override fun getJSMainModuleName(): String = "index"

            override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

            override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
            override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
        }}

    override val reactHost: ReactHost
        get() = getDefaultReactHost(applicationContext, reactNativeHost)

    override fun onCreate() {{
        super.onCreate()
        SoLoader.init(this, false)
        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {{
            load()
        }}
    }}
}}
"""


def tpl_strings_xml(app_name):
    return f"""\
<resources>
    <string name="app_name">{app_name}</string>
</resources>
"""


def tpl_styles_xml():
    return """\
<resources>
    <!--
        Base application theme. Using NoActionBar so React Native can
        manage the full screen layout including the status bar.
    -->
    <style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">
        <!-- React Native handles the window background -->
    </style>
</resources>
"""


def tpl_colors_xml():
    return """\
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Launcher icon background color -->
    <color name="ic_launcher_background">#1a1a2e</color>
</resources>
"""


def tpl_ic_launcher_xml():
    return """\
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
"""


def tpl_ic_launcher_foreground_xml():
    return """\
<?xml version="1.0" encoding="utf-8"?>
<!--
    Launcher foreground icon - simple circle with RN-inspired styling.
    Replace with your own branded icon for production.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Outer ring -->
    <path
        android:fillColor="#61DAFB"
        android:pathData="M54,54m-30,0a30,30 0 1,1 60,0a30,30 0 1,1 -60,0"/>
    <!-- Inner dot -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M54,54m-12,0a12,12 0 1,1 24,0a12,12 0 1,1 -24,0"/>
</vector>
"""


def tpl_proguard_rules():
    return """\
# React Native ProGuard Rules
# Keep the classes needed by React Native

-keep class com.facebook.react.** { *; }
-keep class com.facebook.hermes.** { *; }
-keep class com.facebook.jni.** { *; }
-keep class com.swmansion.** { *; }

# Keep your package classes
-keep class **.MainApplication { *; }
-keep class **.MainActivity { *; }

# Uncomment when adding release signing:
# -dontoptimize
"""


def tpl_github_actions(app_name, bundle_id, pkg_mgr, gh_repo):
    install_cmd = "npm ci" if pkg_mgr == "npm" else "yarn install --frozen-lockfile"
    cache_key   = "npm" if pkg_mgr == "npm" else "yarn"
    cache_path  = "~/.npm" if pkg_mgr == "npm" else "~/.cache/yarn"

    return f"""\
# ─────────────────────────────────────────────────────────────────
#  GitHub Actions — React Native Android Build Pipeline
#  App: {app_name}
#  Bundle ID: {bundle_id}
#
#  Triggers on every push to any branch.
#  Downloads debug APK from the Actions "Artifacts" tab.
#
#  No Android Studio, Gradle, or Node needed locally.
# ─────────────────────────────────────────────────────────────────

name: Android Build

on:
  push:
    branches:
      - '**'
  pull_request:
    branches:
      - '**'
  workflow_dispatch:          # allow manual trigger from GitHub UI

concurrency:
  group: ${{{{ github.workflow }}}}-${{{{ github.ref }}}}
  cancel-in-progress: true   # cancel stale builds on new pushes

jobs:
  build-android:
    name: 🤖 Build Android APK
    runs-on: ubuntu-latest
    timeout-minutes: 60

    steps:
      # ── Checkout ────────────────────────────────────────────────
      - name: Checkout repository
        uses: actions/checkout@v4

      # ── Java 17 (required for AGP 8+) ───────────────────────────
      - name: Set up Java 17 (Temurin)
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: gradle

      # ── Node 20 LTS ─────────────────────────────────────────────
      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: '{cache_key}'

      # ── npm / yarn cache ─────────────────────────────────────────
      - name: Cache node_modules
        uses: actions/cache@v4
        with:
          path: node_modules
          key: node-modules-${{{{ hashFiles('package-lock.json', 'yarn.lock') }}}}
          restore-keys: node-modules-

      # ── Install JS dependencies ──────────────────────────────────
      - name: Install JavaScript dependencies
        run: {install_cmd}

      # ── Bootstrap Gradle wrapper ─────────────────────────────────
      #   We generate the wrapper in CI so we don't need to commit
      #   the binary gradle-wrapper.jar to the repository.
      - name: Install Gradle (for wrapper bootstrap)
        run: |
          GRADLE_VERSION=$(grep "distributionUrl" android/gradle/wrapper/gradle-wrapper.properties \
            | sed 's/.*gradle-\\(.*\\)-bin.zip/\\1/')
          echo "Bootstrapping Gradle $GRADLE_VERSION"
          wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" \
            -O /tmp/gradle.zip
          unzip -q /tmp/gradle.zip -d /opt/gradle
          echo "/opt/gradle/gradle-$GRADLE_VERSION/bin" >> $GITHUB_PATH
          echo "GRADLE_VERSION=$GRADLE_VERSION" >> $GITHUB_ENV

      - name: Generate Gradle wrapper
        run: |
          cd android
          gradle wrapper \
            --gradle-version="$GRADLE_VERSION" \
            --distribution-type=bin
          chmod +x gradlew

      # ── Gradle build cache ───────────────────────────────────────
      - name: Cache Gradle caches
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{{{ hashFiles('android/**/*.gradle*', 'android/gradle/wrapper/gradle-wrapper.properties') }}}}
          restore-keys: gradle-

      # ── Debug keystore ───────────────────────────────────────────
      #   Generated fresh each run (only used for debug builds).
      #   For release builds, set up RELEASE_KEYSTORE_PATH secret.
      - name: Generate debug keystore
        run: |
          cd android/app
          keytool -genkey -v \\
            -keystore debug.keystore \\
            -alias androiddebugkey \\
            -keyalg RSA -keysize 2048 \\
            -validity 10000 \\
            -storepass android -keypass android \\
            -dname "CN=Android Debug,O=Android,C=US" \\
            -noprompt

      # ── React Native Metro bundler pre-check ─────────────────────
      - name: Validate JS bundle (dry run)
        run: |
          npx react-native bundle \\
            --platform android \\
            --dev false \\
            --entry-file index.js \\
            --bundle-output /tmp/main.bundle \\
            --assets-dest /tmp/assets \\
            2>&1 | tail -20

      # ── Assemble Debug APK ───────────────────────────────────────
      - name: Assemble Debug APK
        run: |
          cd android
          ./gradlew assembleDebug \\
            --no-daemon \\
            --stacktrace \\
            --info
        env:
          CI: true

      # ── Find & rename APK ────────────────────────────────────────
      - name: Locate and rename APK
        run: |
          APK=$(find android/app/build/outputs/apk/debug -name "*.apk" | head -1)
          echo "Found APK: $APK"
          SHA=$(echo "${{{{ github.sha }}}}" | cut -c1-7)
          DEST="android/app/build/outputs/apk/debug/{app_name.lower()}-debug-$SHA.apk"
          mv "$APK" "$DEST"
          echo "APK_PATH=$DEST" >> $GITHUB_ENV
          ls -lh "$DEST"

      # ── Upload APK as workflow artifact ──────────────────────────
      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: "{app_name.lower()}-debug-apk-${{{{ github.run_number }}}}"
          path: ${{{{ env.APK_PATH }}}}
          retention-days: 30
          if-no-files-found: error

      # ── Build summary ────────────────────────────────────────────
      - name: Post build summary
        if: success()
        run: |
          echo "## ✅ Build Successful!" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Field | Value |" >> $GITHUB_STEP_SUMMARY
          echo "|-------|-------|" >> $GITHUB_STEP_SUMMARY
          echo "| App | {app_name} |" >> $GITHUB_STEP_SUMMARY
          echo "| Bundle ID | {bundle_id} |" >> $GITHUB_STEP_SUMMARY
          echo "| Commit | ${{{{ github.sha }}}} |" >> $GITHUB_STEP_SUMMARY
          echo "| Branch | ${{{{ github.ref_name }}}} |" >> $GITHUB_STEP_SUMMARY
          echo "| Build | #${{{{ github.run_number }}}} |" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "Download the APK from the **Artifacts** section above ⬆️" >> $GITHUB_STEP_SUMMARY
"""


def tpl_readme(app_name, bundle_id, gh_repo, pkg_mgr):
    install_cmd = "npm ci" if pkg_mgr == "npm" else "yarn"
    return f"""\
# {app_name}

> React Native Android app — generated by [rn-archetype-generator](https://github.com/{gh_repo})

[![Android Build](https://github.com/{gh_repo}/actions/workflows/build.yml/badge.svg)](https://github.com/{gh_repo}/actions/workflows/build.yml)

---

## 🚀 Zero Local Toolchain Required

This project is configured for **100% cloud builds** via GitHub Actions.  
Every push triggers a build. Download your APK from the **Actions → Artifacts** tab.

You do **not** need Android Studio, Gradle, NDK, or `npx react-native init` locally.

---

## 📁 Project Structure

```
{app_name}/
├── src/
│   ├── screens/
│   │   └── HomeScreen.tsx       ← Hello World screen
│   └── components/              ← Reusable UI components
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/            ← Kotlin native code
│   │   │   └── res/             ← Android resources
│   │   └── build.gradle         ← App-level build config
│   ├── build.gradle             ← Root build config
│   ├── settings.gradle          ← Project settings & autolinking
│   ├── gradle.properties        ← Gradle flags (Hermes, New Arch)
│   └── gradle/wrapper/          ← Gradle version pin
├── index.js                     ← JS entry point
├── App.tsx                      ← Root React component
├── package.json                 ← Dependencies
└── .github/workflows/build.yml  ← CI/CD pipeline
```

---

## 🔧 Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | React Native 0.75.4 |
| Language | TypeScript 5.5 |
| JS Engine | Hermes |
| Build Tool | Gradle 8.10+ with AGP 8.7 |
| Language (Native) | Kotlin 2.0 |
| CI/CD | GitHub Actions |
| Java | Temurin 17 |
| Min SDK | API 24 (Android 7.0) |
| Target SDK | API 35 (Android 15) |

---

## 📲 Getting Your APK

1. Push any commit to GitHub
2. Go to **Actions** tab → select the latest run
3. Scroll to **Artifacts** → download `{app_name.lower()}-debug-apk-N`
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
{install_cmd}
npx react-native run-android
```

---

## 📦 Bundle ID

`{bundle_id}`

---

## 📄 License

MIT
"""


def tpl_src_index(dir_name):
    """Barrel export for a src subdirectory."""
    return f"// {dir_name} — add your exports here\n"


# ─────────────────────────────────────────────────────────────────────────────
#   MAIN GENERATOR
# ─────────────────────────────────────────────────────────────────────────────

def collect_config():
    print_step("Let's configure your project")

    app_name = ask(
        "App name (PascalCase, e.g. MyApp)",
        default="MyApp",
        validator=validate_app_name
    )

    bundle_id = ask(
        "Bundle / package ID (e.g. com.acme.myapp)",
        default=f"com.example.{app_name.lower()}",
        validator=validate_package
    )

    display_name = ask(
        "Display name shown on device",
        default=app_name
    )

    gh_repo = ask(
        "GitHub repository (owner/repo, e.g. acmecorp/myapp)",
        default=f"your-org/{app_name.lower()}",
        validator=validate_gh_repo
    )

    pkg_mgr = ask_choice(
        "Package manager",
        ["npm", "yarn"],
        default="npm"
    )

    output_dir = ask(
        "Output directory (project will be created inside it)",
        default=os.getcwd()
    )

    gradle_version = ask(
        "Gradle version to use",
        default="8.10.2"
    )

    return {
        "app_name":     app_name,
        "bundle_id":    bundle_id,
        "display_name": display_name,
        "gh_repo":      gh_repo,
        "pkg_mgr":      pkg_mgr,
        "output_dir":   output_dir,
        "gradle_ver":   gradle_version,
    }


def generate(cfg):
    app_name   = cfg["app_name"]
    bundle_id  = cfg["bundle_id"]
    gh_repo    = cfg["gh_repo"]
    pkg_mgr    = cfg["pkg_mgr"]
    gradle_ver = cfg["gradle_ver"]
    base       = os.path.join(cfg["output_dir"], app_name)
    pkg_path   = pkg_to_path(bundle_id)

    if os.path.exists(base):
        print(clr(f"\n  ⚠️  Directory '{base}' already exists.", YELLOW))
        cont = input("  Overwrite? [y/N]: ").strip().lower()
        if cont != 'y':
            print(clr("  Aborted.", RED))
            sys.exit(1)

    print_step("Generating project structure")

    # ── Root files ──────────────────────────────────────────────────────────
    write(base, "package.json",       tpl_package_json(app_name, bundle_id, pkg_mgr))
    write(base, "app.json",           tpl_app_json(app_name))
    write(base, "index.js",           tpl_index_js(app_name))
    write(base, "App.tsx",            tpl_app_tsx(app_name))
    write(base, "tsconfig.json",      tpl_tsconfig())
    write(base, "babel.config.js",    tpl_babel_config())
    write(base, "metro.config.js",    tpl_metro_config())
    write(base, ".gitignore",         tpl_gitignore())
    write(base, ".prettierrc.json",   tpl_prettierrc())
    write(base, ".eslintrc.js",       tpl_eslintrc())

    # ── src/ ─────────────────────────────────────────────────────────────────
    write(base, "src/screens/HomeScreen.tsx", tpl_home_screen(app_name))
    write(base, "src/components/.gitkeep",   "")
    write(base, "src/hooks/.gitkeep",        "")
    write(base, "src/utils/.gitkeep",        "")
    write(base, "src/assets/.gitkeep",       "")

    # ── Android root ─────────────────────────────────────────────────────────
    print_step("Generating Android project files")

    write(base, "android/build.gradle",         tpl_android_root_build_gradle())
    write(base, "android/settings.gradle",       tpl_android_settings_gradle(app_name))
    write(base, "android/gradle.properties",     tpl_gradle_properties())
    write(base, "android/gradle/wrapper/gradle-wrapper.properties",
                                                 tpl_gradle_wrapper_properties(gradle_ver))

    # gradlew shell script
    gradlew_path = write(base, "android/gradlew", tpl_gradlew())
    make_executable(gradlew_path)

    write(base, "android/gradlew.bat",           tpl_gradlew_bat())

    # ── Android app module ───────────────────────────────────────────────────
    write(base, "android/app/build.gradle",      tpl_app_build_gradle(bundle_id))
    write(base, "android/app/proguard-rules.pro", tpl_proguard_rules())

    # AndroidManifest
    write(base, "android/app/src/main/AndroidManifest.xml",
          tpl_android_manifest(app_name, bundle_id))

    # Kotlin sources
    java_dir = f"android/app/src/main/java/{pkg_path}"
    write(base, f"{java_dir}/MainActivity.kt",
          tpl_main_activity(bundle_id, app_name))
    write(base, f"{java_dir}/MainApplication.kt",
          tpl_main_application(bundle_id))

    # Resources
    write(base, "android/app/src/main/res/values/strings.xml",  tpl_strings_xml(app_name))
    write(base, "android/app/src/main/res/values/styles.xml",   tpl_styles_xml())
    write(base, "android/app/src/main/res/values/colors.xml",   tpl_colors_xml())

    # Adaptive icon (API 26+)
    for variant in ["ic_launcher", "ic_launcher_round"]:
        write(base, f"android/app/src/main/res/mipmap-anydpi-v26/{variant}.xml",
              tpl_ic_launcher_xml())

    # Launcher foreground vector drawable
    write(base, "android/app/src/main/res/drawable/ic_launcher_foreground.xml",
          tpl_ic_launcher_foreground_xml())

    # ── GitHub Actions ───────────────────────────────────────────────────────
    print_step("Generating GitHub Actions CI/CD pipeline")

    write(base, ".github/workflows/build.yml",
          tpl_github_actions(app_name, bundle_id, pkg_mgr, gh_repo))

    # ── README ───────────────────────────────────────────────────────────────
    write(base, "README.md",
          tpl_readme(app_name, bundle_id, gh_repo, pkg_mgr))

    return base


def print_next_steps(base, cfg):
    app_name  = cfg["app_name"]
    gh_repo   = cfg["gh_repo"]
    pkg_mgr   = cfg["pkg_mgr"]
    install   = "npm install" if pkg_mgr == "npm" else "yarn"

    print(f"""
{clr('═' * 60, CYAN)}
{clr('  ✅  Project generated!', GREEN, BOLD)}
{clr('═' * 60, CYAN)}

  📁 Location  →  {clr(base, YELLOW)}
  📦 App Name  →  {clr(app_name, YELLOW)}
  🔑 Bundle ID →  {clr(cfg['bundle_id'], YELLOW)}
  🐙 Repo      →  {clr('https://github.com/' + gh_repo, YELLOW)}

{clr('  Next steps:', BOLD)}

  {clr('1.', CYAN)} Open the project folder:
     {clr(f'cd {base}', DIM)}

  {clr('2.', CYAN)} Initialize git and push to GitHub:
     {clr('git init', DIM)}
     {clr('git add .', DIM)}
     {clr('git commit -m "Initial commit — generated by rn-archetype"', DIM)}
     {clr(f'git remote add origin https://github.com/{gh_repo}.git', DIM)}
     {clr('git push -u origin main', DIM)}

  {clr('3.', CYAN)} GitHub Actions kicks in automatically.
     Watch the build at:
     {clr(f'https://github.com/{gh_repo}/actions', DIM)}

  {clr('4.', CYAN)} Download your APK from Actions → Artifacts.
     Install on your Samsung S24 Ultra / S25 Ultra or any device! 🎉

  {clr('Optional — local dev (needs Node + Android SDK):', DIM)}
     {clr(f'{install}', DIM)}
     {clr('npx react-native run-android', DIM)}

{clr('  Happy coding! 🚀', GREEN, BOLD)}
{clr('═' * 60, CYAN)}
""")


def main():
    print_banner()

    try:
        cfg  = collect_config()
        base = generate(cfg)
        print_next_steps(base, cfg)
    except KeyboardInterrupt:
        print(clr("\n\n  Cancelled.", YELLOW))
        sys.exit(0)


if __name__ == "__main__":
    main()
