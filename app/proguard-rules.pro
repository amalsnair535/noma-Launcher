# Add project specific ProGuard rules here.

# Preserving line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses

# Hide the original source file name.
-renamesourcefileattribute SourceFile

# Application, ViewModel & Data Layers
-keep class com.freelauncher.app.** { *; }
-keepclassmembers class com.freelauncher.app.** { *; }

# Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao
-dontwarn androidx.room.**

# Moshi Rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep @com.squareup.moshi.JsonQualifier class *
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass *;
}

# Google ML Kit Digital Ink Recognition Rules
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_digital_ink_recognition.** { *; }
-dontwarn com.google.mlkit.**

# Firebase & Google Play Services Rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.gms.internal.**

# Coroutines, Retrofit & OkHttp
-keep class kotlinx.coroutines.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * { @retrofit2.http.* <methods>; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
