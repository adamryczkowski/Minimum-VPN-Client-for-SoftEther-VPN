# ==============================================================================
# ProGuard/R8 Rules for SoftEther Connect VPN Client
# ==============================================================================
#
# This file contains ProGuard/R8 rules for code shrinking, obfuscation,
# and optimization. These rules ensure the app works correctly when
# minification is enabled in release builds.
#
# For more details, see:
#   https://developer.android.com/build/shrink-code
#   https://www.guardsquare.com/manual/configuration/usage
# ==============================================================================

# ==============================================================================
# General Android Rules
# ==============================================================================

# Keep line number information for better stack traces
-keepattributes SourceFile,LineNumberTable

# Hide original source file name in stack traces
-renamesourcefileattribute SourceFile

# Keep annotations (required for many libraries)
-keepattributes *Annotation*

# Keep signature information (required for generics)
-keepattributes Signature

# Keep exception information
-keepattributes Exceptions

# Keep inner classes
-keepattributes InnerClasses

# Keep enclosing method for lambdas
-keepattributes EnclosingMethod

# ==============================================================================
# VPN Service Rules
# ==============================================================================

# Keep VPN service and related classes
-keep class kittoku.mvc.service.SoftEtherVpnService { *; }
-keep class kittoku.mvc.service.client.** { *; }

# Keep VPN protocol implementation classes
-keep class kittoku.mvc.unit.** { *; }

# Keep preference classes (used via reflection)
-keep class kittoku.mvc.preference.** { *; }
-keep class kittoku.mvc.preference.accessor.** { *; }
-keep class kittoku.mvc.preference.custom.** { *; }

# Keep auto-connect receiver
-keep class kittoku.mvc.autoconnect.BootReceiver { *; }

# ==============================================================================
# Security Library Rules
# ==============================================================================

# EncryptedSharedPreferences (ed-george fork - dev.spght:encryptedprefs-ktx)
-keep class dev.spght.encryptedprefs.** { *; }
-keep class dev.spght.encryptedprefs.core.** { *; }

# Google Tink (used by EncryptedSharedPreferences)
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** {
    *;
}

# Keep MasterKey and related security classes
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.MasterKey$Builder { *; }
-keep class androidx.security.crypto.MasterKey$KeyScheme { *; }

# Keep SecureCredentialStorage
-keep class kittoku.mvc.security.SecureCredentialStorage { *; }
-keep class kittoku.mvc.security.SecureCredentialStorage$Keys { *; }

# ==============================================================================
# Kotlin Serialization Rules
# ==============================================================================

# Keep serialization classes
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep @Serializable classes
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep kotlinx.serialization classes
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

# ==============================================================================
# Room Database Rules
# ==============================================================================

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Room generated classes
-keep class **_Impl { *; }

# ==============================================================================
# Koin Dependency Injection Rules
# ==============================================================================

# Keep Koin modules
-keep class org.koin.** { *; }
-keep class kittoku.mvc.di.** { *; }

# ==============================================================================
# Jetpack Compose Rules
# ==============================================================================

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }

# Keep Composable functions (for reflection)
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ==============================================================================
# Coroutines Rules
# ==============================================================================

# Keep coroutine classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep coroutine state machines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep coroutine debug info
-keepnames class kotlinx.coroutines.** { *; }

# ==============================================================================
# Kotlin Reflection Rules
# ==============================================================================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin reflection
-keep class kotlin.reflect.** { *; }

# ==============================================================================
# Network/SSL Rules
# ==============================================================================

# Keep SSL/TLS classes
-keep class javax.net.ssl.** { *; }
-keep class javax.security.** { *; }
-keep class java.security.** { *; }

# Keep OkHttp (if used)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==============================================================================
# Debug/Logging Rules
# ==============================================================================

# Remove logging in release builds (optional - uncomment to enable)
# -assumenosideeffects class android.util.Log {
#     public static *** d(...);
#     public static *** v(...);
#     public static *** i(...);
# }

# Keep debug classes for crash reporting
-keep class kittoku.mvc.debug.** { *; }

# ==============================================================================
# Data Classes and Models
# ==============================================================================

# Keep connection profile classes
-keep class kittoku.mvc.data.** { *; }
-keep class kittoku.mvc.profile.** { *; }
-keep class kittoku.mvc.statistics.** { *; }

# Keep split tunneling classes
-keep class kittoku.mvc.splittunnel.** { *; }

# ==============================================================================
# Fragment and Activity Rules
# ==============================================================================

# Keep fragments
-keep class kittoku.mvc.fragment.** { *; }

# Keep activities
-keep class kittoku.mvc.MainActivity { *; }

# ==============================================================================
# Suppress Warnings
# ==============================================================================

# Suppress warnings for missing classes (common in Android)
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ==============================================================================
# Optimization Settings
# ==============================================================================

# Don't optimize in debug builds (faster builds)
# -dontoptimize

# Allow access modification for better optimization
-allowaccessmodification

# Merge interfaces when possible
-mergeinterfacesaggressively

# Remove unused code
-repackageclasses ''
