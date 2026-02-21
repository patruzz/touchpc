# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data classes for serialization
-keep class com.touchpc.remotecontrol.data.** { *; }
-keep class com.touchpc.remotecontrol.protocol.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { <fields>; }

# Navigation Component
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# Keep sealed classes
-keep class com.touchpc.remotecontrol.transport.TransportState$* { *; }
-keep class com.touchpc.remotecontrol.protocol.Command$* { *; }
