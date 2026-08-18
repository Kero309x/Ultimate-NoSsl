# Xposed Framework Integration
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
-keep class com.ultimate.nossl.UltimateHook { *; }
-keep interface de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep interface de.robv.android.xposed.IXposedHookZygoteInit { *; }

# Native C++ / JNI Bindings
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class com.ultimate.nossl.UltimateHook {
    private native void initNative();
    public static native void scanFlutterNative();
}

# Core Hooks, Core Engine, and Utilities
-keep class com.ultimate.nossl.hooks.** { *; }
-keep class com.ultimate.nossl.core.** { *; }
-keep class com.ultimate.nossl.utils.** { *; }

# Room Database & Entities
-keep class com.ultimate.nossl.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# General Attributes & Reflection Safety
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

