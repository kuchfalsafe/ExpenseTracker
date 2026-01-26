# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep MainActivity in primary DEX for multidex
-keep class com.kishan.expensetracker.MainActivity { *; }

# Keep Application class
-keep class com.kishan.expensetracker.ExpenseTrackerApp { *; }

# Keep all activities
-keep class * extends android.app.Activity
-keep class * extends androidx.activity.ComponentActivity

# Keep all composable functions
-keep @androidx.compose.runtime.Composable class *
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Google Play Services Auth classes
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.**
