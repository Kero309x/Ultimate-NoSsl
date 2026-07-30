package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class AntiDetectionHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookPackageManager(lpparam)
        hookSystemProperties(lpparam)
        hookDebug(lpparam)
        hookBuildProperties(lpparam)
    }

    private fun hookPackageManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val pmClass = XposedHelpers.findClassIfExists("android.app.ApplicationPackageManager", lpparam.classLoader)
                ?: XposedHelpers.findClassIfExists("android.content.pm.PackageManager", lpparam.classLoader)
            if (pmClass != null) {
                val badPackages = setOf(
                    "de.robv.android.xposed.installer",
                    "org.lsposed.manager",
                    "com.topjohnwu.magisk",
                    "eu.chainfire.supersu"
                )

                XposedBridge.hookAllMethods(pmClass, "getPackageInfo", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pkg = param.args.firstOrNull()?.toString()
                        if (pkg != null && badPackages.contains(pkg)) {
                            val exClass = XposedHelpers.findClassIfExists("android.content.pm.PackageManager\$NameNotFoundException", lpparam.classLoader)
                            if (exClass != null) {
                                param.throwable = exClass.getConstructor(String::class.java).newInstance(pkg) as Throwable
                            }
                        }
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val systemProperties = XposedHelpers.findClassIfExists("android.os.SystemProperties", lpparam.classLoader)
            if (systemProperties != null) {
                XposedBridge.hookAllMethods(systemProperties, "get", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args.firstOrNull()?.toString() ?: return
                        if (key.contains("debug.fuzz") || key.contains("service.adb.tcp.port")) {
                            param.result = ""
                        }
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun hookDebug(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val debugClass = XposedHelpers.findClassIfExists("android.os.Debug", lpparam.classLoader)
            if (debugClass != null) {
                XposedBridge.hookAllMethods(debugClass, "isDebuggerConnected", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })
                XposedBridge.hookAllMethods(debugClass, "waitingForDebugger", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false
                    }
                })
            }
        } catch (e: Throwable) {}
    }

    private fun hookBuildProperties(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val buildClass = XposedHelpers.findClassIfExists("android.os.Build", lpparam.classLoader)
            if (buildClass != null) {
                XposedHelpers.setStaticBooleanField(buildClass, "IS_EMULATOR", false)
                XposedHelpers.setStaticObjectField(buildClass, "TAGS", "release-keys")
            }
        } catch (e: Throwable) {}
    }
}
