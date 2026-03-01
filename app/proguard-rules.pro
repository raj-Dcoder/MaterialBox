# ============================================================
# MaterialBox ProGuard Rules
# These rules tell R8 what NOT to strip out during minification
# ============================================================

# --- Room Database ---
# Keep Room entities and DAOs — R8 can't see they're used via reflection
-keep class com.rajveer.materialbox.data.entity.** { *; }
-keep class com.rajveer.materialbox.data.dao.** { *; }
-keep class com.rajveer.materialbox.data.converter.** { *; }

# --- Hilt / Dagger ---
# Hilt uses annotation processing + reflection — keep generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- Keep line numbers for crash reports ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Compose ---
# Compose uses reflection for previews and some internals
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- General Android ---
-keep class * extends android.app.Application
-keep class * extends android.app.Activity