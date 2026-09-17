# ============================================
# Ultimate NoSSL - ProGuard/R8 Rules
# ============================================

# --- Xposed Framework Integration ---
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
-keep class com.ultimate.nossl.UltimateHook { *; }
-keep interface de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep interface de.robv.android.xposed.IXposedHookZygoteInit { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit { *; }

# --- Native C++ / JNI Bindings ---
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.ultimate.nossl.UltimateHook {
    private native void initNative();
    public static native void scanFlutterNative();
}

# --- Hook Classes (Reflection targets) ---
-keep class com.ultimate.nossl.hooks.** { *; }
-keep class com.ultimate.nossl.core.** { *; }
-keep class com.ultimate.nossl.utils.** { *; }

# --- Room Database & Entities ---
-keep class com.ultimate.nossl.data.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# --- ShadowHook (Native Hooking) ---
-keep class com.bytedance.shadowhook.** { *; }
-dontwarn com.bytedance.shadowhook.**

# --- Security / SSL Classes ---
-keep class javax.net.ssl.** { *; }
-keep class java.security.** { *; }
-keep class javax.security.cert.** { *; }

# --- Android Lifecycle & ViewModel ---
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# --- General Attributes & Reflection Safety ---
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable
-keepattributes Exceptions, Deprecated, RunInvisibleAnnotations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Prevent stripping of @Keep annotated classes ---
-keep @androidx.annotation.Keep class * { *; }
-keep class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
