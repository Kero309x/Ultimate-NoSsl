package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook
import de.robv.android.xposed.XC_MethodHook
import com.ultimate.nossl.UltimateHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class FlutterHook : BaseHook() {
    override val id = "Flutter"
    override val name = "Flutter Engine SSL Interceptor"
    override val targetFramework = "Flutter"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam): Boolean {
        return XposedHelpers.findClassIfExists("io.flutter.embedding.engine.FlutterJNI", lpparam.classLoader) != null
    }

    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        val flutterJNI = XposedHelpers.findClass("io.flutter.embedding.engine.FlutterJNI", lpparam.classLoader)
        
        XposedBridge.hookAllMethods(flutterJNI, "attachToNative", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                logDiagnostic("FlutterJNI attached to native engine -> triggering memory patch")
                try {
                    UltimateHook.scanFlutterNative()
                } catch (t: Throwable) {
                    logDiagnostic("Failed to trigger scanFlutterNative: ${t.message}")
                }
            }
        })

        XposedBridge.hookAllMethods(flutterJNI, "init", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                logDiagnostic("FlutterJNI init called -> triggering memory patch")
                try {
                    UltimateHook.scanFlutterNative()
                } catch (t: Throwable) {
                    logDiagnostic("Failed to trigger scanFlutterNative: ${t.message}")
                }
            }
        })
    }
}
