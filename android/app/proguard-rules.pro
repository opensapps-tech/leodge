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
