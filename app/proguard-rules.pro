# Xposed API
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# Keep our Xposed entry point
-keep class com.ultimate.nossl.UltimateHook { *; }

# Keep hook classes and their members
-keep class com.ultimate.nossl.hooks.** { *; }
-keep class com.ultimate.nossl.core.** { *; }
-keep class com.ultimate.nossl.utils.** { *; }

# Room
-keep class com.ultimate.nossl.data.** { *; }

# Moshi and Retrofit (used in module)
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**

# General attributes
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
