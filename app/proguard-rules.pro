# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep data classes used by Room
-keepclassmembers class com.applock.guard.data.db.** {
    <fields>;
    <init>(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-dontwarn androidx.compose.**

# Security Crypto
-keep class androidx.security.crypto.** { *; }
