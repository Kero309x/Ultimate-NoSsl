
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FlutterHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookFlutterJNI(lpparam)
        hookDartExecutor(lpparam)
        hookPlatformChannels(lpparam)
        hookNativeLoading(lpparam)
    }

    private fun hookNativeLoading(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.Runtime",
                lpparam.classLoader,
                "loadLibrary0",
                ClassLoader::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val libName = param.args[1] as? String ?: return
                        if (libName.contains("flutter")) {
                            Logger.i("libflutter.so detected in loadLibrary0")
                        }
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookFlutterJNI(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val flutterJNI = XposedHelpers.findClassIfExists(
                "io.flutter.embedding.engine.FlutterJNI",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(flutterJNI, "attachToNative", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.i("FlutterJNI attached to native engine")
                }
            })

            XposedBridge.hookAllMethods(flutterJNI, "handlePlatformMessage", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val channel = param.args[0] as? String ?: return
                    val message = param.args[1] as? java.nio.ByteBuffer
                    
                    if (channel.contains("network") || channel.contains("http") || channel.contains("security") || channel.contains("auth")) {
                        Logger.i("Flutter platform channel: $channel")
                    }
                }
            })

            // Hook native response methods
            val nativeMethods = arrayOf(
                "nativeInvokePlatformMessageResponseCallback",
                "nativeHandlePlatformMessage",
                "nativeHandlePlatformMessageResponse"
            )
            nativeMethods.forEach { method ->
                try {
                    XposedBridge.hookAllMethods(flutterJNI, method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            // Some methods might need result forcing here
                        }
                    })
                } catch (e: Throwable) { }
            }
        } catch (e: Throwable) { }
    }

    private fun hookDartExecutor(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val executor = XposedHelpers.findClassIfExists(
                "io.flutter.embedding.engine.dart.DartExecutor",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(executor, "executeDartCallback", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.i("DartExecutor callback executed")
                }
            })
        } catch (e: Throwable) { }

        // Attempt to hook Dart/Flutter security context if accessible via reflection
        try {
            val securityContextClass = XposedHelpers.findClassIfExists(
                "io.flutter.plugins.urllauncher.WebViewActivity", // Placeholder, Dart SecurityContext isn't exposed to Java directly usually, but we hook known methods
                lpparam.classLoader
            )
        } catch (e: Throwable) { }
    }

    private fun hookPlatformChannels(lpparam: XC_LoadPackage.LoadPackageParam) {
        val channels = listOf(
            "io.flutter.plugin.common.MethodChannel",
            "io.flutter.plugin.common.BasicMessageChannel"
        )
 
        channels.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "invokeMethod", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val method = param.args[0] as? String ?: return
                        if (method.contains("ssl", ignoreCase = true) ||
                            method.contains("cert", ignoreCase = true) ||
                            method.contains("trust", ignoreCase = true) ||
                            method.contains("verify", ignoreCase = true) ||
                            method.contains("check", ignoreCase = true)) {
                            Logger.i("Flutter method invoked: $method")
                            
                            // If it's a verification method, we might want to skip or force success
                            if (method.contains("checkPinning", ignoreCase = true) || 
                                method.contains("verifyCertificate", ignoreCase = true)) {
                                // This would require replacing the Result callback or similar
                            }
                        }
                    }
                })
                
                // Hook setMethodCallHandler to catch incoming calls from Dart
                XposedBridge.hookAllMethods(clazz, "setMethodCallHandler", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.i("Flutter MethodCallHandler set for: $clsName")
                    }
                })
            } catch (e: Throwable) { }
        }
    }
}
