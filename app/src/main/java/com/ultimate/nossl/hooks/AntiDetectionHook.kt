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
                    "com.topjohnwu.magisk",
                    "com.topjohnwu.superuser",
                    "org.lsposed.manager",
                    "org.lsposed.lspatch",
                    "com.tsng.hidemyapplist",
                    "me.weishu.kernelsu",
                    "com.rifsft.kernelsu",
                    "de.robv.android.xposed.installer",
                    "eu.chainfire.supersu",
                    "com.koushikdutta.superuser",
                    "com.noshufou.android.su",
                    "com.devadvance.rootcloak",
                    "com.devadvance.rootcloakplus",
                    "com.saurik.substrate",
                    "com.kingroot.kinguser",
                    "com.kingo.root",
                    "com.lefasroot"
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

                XposedBridge.hookAllMethods(pmClass, "getInstalledPackages", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val result = param.result ?: return
                            val packages = result::class.java.getMethod("getList").invoke(result) as? MutableList<*> ?: return
                            packages.removeAll { pkg ->
                                val pkgName = pkg?.javaClass?.getMethod("getPackageName")?.invoke(pkg)?.toString() ?: ""
                                badPackages.contains(pkgName)
                            }
                        } catch (ignored: Throwable) {}
                    }
                })

                XposedBridge.hookAllMethods(pmClass, "queryIntentActivities", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val result = param.result ?: return
                            val activities = result::class.java.getMethod("getList").invoke(result) as? MutableList<*> ?: return
                            activities.removeAll { act ->
                                val pkgName = act?.javaClass?.getMethod("getActivityInfo")?.invoke(act)
                                    ?.javaClass?.getMethod("getPackageName")?.invoke(act?.javaClass?.getMethod("getActivityInfo")?.invoke(act))
                                    ?.toString() ?: ""
                                badPackages.contains(pkgName)
                            }
                        } catch (ignored: Throwable) {}
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
                            name.contains("LspHooker") ||
                            name.contains("xposed") ||
                            name.contains("EdXposed") ||
                            name.contains("lsposed")
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
                            path.contains("xposed") ||
                            path.contains("XposedBridge") ||
                            path.contains("/supersu") ||
                            path.contains("SuperSU") ||
                            path.contains("/.core") ||
                            path.contains("/magiskhide") ||
                            path.contains("/su.d") ||
                            path.contains("/ksu") ||
                            path.contains("kernelsu") ||
                            path.contains("lspd") ||
                            path.contains("lsposed") ||
                            path.contains("/daemonsu") ||
                            path.contains("/supolicy") ||
                            path.contains("sbin/su") ||
                            path.contains("/system/xbin/su") ||
                            path.contains("/system/bin/su") ||
                            path.contains("RootCloak") ||
                            path.contains("rootcloak") ||
                            path.contains("substrate") ||
                            path.contains("hide-my-applist")) {
                            param.result = false
                        }
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }
}
