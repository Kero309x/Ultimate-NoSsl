
package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import com.ultimate.nossl.UltimateHook
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FlutterHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookFlutterJNI(lpparam)
    }

    private fun hookFlutterJNI(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val flutterJNI = XposedHelpers.findClassIfExists(
                "io.flutter.embedding.engine.FlutterJNI",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(flutterJNI, "attachToNative", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("Flutter", "FlutterJNI attached to native engine -> trigger memory patch")
                    try {
                        UltimateHook.scanFlutterNative()
                    } catch (ignored: Throwable) {}
                }
            })

            XposedBridge.hookAllMethods(flutterJNI, "init", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        UltimateHook.scanFlutterNative()
                    } catch (ignored: Throwable) {}
                }
            })
        } catch (ignored: Throwable) { }
    }
}
