package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.UltimateHook
import com.ultimate.nossl.utils.Logger

class NativeInterceptor {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookLibraryLoading(lpparam)
    }

    private fun hookLibraryLoading(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllMethods(Runtime::class.java, "loadLibrary0", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val libName = param.args.firstOrNull { it is String } as? String ?: return
                    if (libName.contains("flutter") || libName.contains("cronet") || libName.contains("crypto") || libName.contains("ssl")) {
                        Logger.native("Runtime.loadLibrary0: $libName")
                        try {
                            UltimateHook.scanFlutterNative()
                        } catch (ignored: Throwable) {}
                    }
                }
            })
            
            XposedBridge.hookAllMethods(Runtime::class.java, "load0", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val path = param.args.firstOrNull { it is String } as? String ?: return
                    if (path.contains("libflutter.so") || path.contains("libcronet.so") || path.contains("libssl.so")) {
                        Logger.native("Runtime.load0: ${path.substringAfterLast('/')}")
                        try {
                            UltimateHook.scanFlutterNative()
                        } catch (ignored: Throwable) {}
                    }
                }
            })
        } catch (e: Throwable) {
            Logger.e("Failed to hook library loading", e)
        }
    }
}
