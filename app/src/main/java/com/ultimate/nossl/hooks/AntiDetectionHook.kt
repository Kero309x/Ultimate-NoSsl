package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import java.io.File

class AntiDetectionHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookPackageManager(lpparam)
        hookStackTraces(lpparam)
        hookFileChecks(lpparam)
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
        } catch (ignored: Throwable) {}
    }

    private fun hookStackTraces(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                Throwable::class.java,
                "getStackTrace",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val stack = param.result as? Array<StackTraceElement> ?: return
                        val cleanStack = stack.filterNot { elem ->
                            val name = elem.className
                            name.contains("de.robv.android.xposed") ||
                            name.contains("com.ultimate.nossl") ||
                            name.contains("EdHooker") ||
                            name.contains("LspHooker")
                        }.toTypedArray()
                        param.result = cleanStack
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }

    private fun hookFileChecks(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                File::class.java,
                "exists",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val file = param.thisObject as? File ?: return
                        val path = file.absolutePath
                        if (path.endsWith("/su") ||
                            path.contains("/magisk") ||
                            path.contains("busybox") ||
                            path.contains("Superuser.apk") ||
                            path.contains("xposed")) {
                            param.result = false
                        }
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }
}
