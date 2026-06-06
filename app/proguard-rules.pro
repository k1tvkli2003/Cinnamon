# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# =========================================================================
# AGGRESSIVE R8 PERFORMANCE SHRINKING & BUGS PREVENTION RULES
# =========================================================================

# Jetpack Compose and state runtime tracking
-keep class androidx.compose.** { *; }

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Retrofit, OkHttp, & Logging
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okhttp3.**

# Moshi JSON reflection parsing (Crucial for live AI Chat replies)
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep class com.example.data.ai.** { *; }

# Room Database & Entities
-dontwarn androidx.room.**
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep class com.example.data.local.** { *; }

