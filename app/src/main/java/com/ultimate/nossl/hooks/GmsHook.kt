package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class GmsHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookProviderInstaller(lpparam)
    }

    private fun hookProviderInstaller(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val providerInstallerCls = XposedHelpers.findClassIfExists(
                "com.google.android.gms.security.ProviderInstaller",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(providerInstallerCls, "installIfNeeded", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    Logger.hook("GMS", "ProviderInstaller.installIfNeeded bypassed")
                    return null
                }
            })

            XposedBridge.hookAllMethods(providerInstallerCls, "installIfNeededAsync", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    Logger.hook("GMS", "ProviderInstaller.installIfNeededAsync bypassed")
                    val listener = param.args.getOrNull(1)
                    if (listener != null) {
                        try {
                            val onInstalledMethod = listener.javaClass.declaredMethods.firstOrNull { 
                                it.name == "onProviderInstalled" 
                            }
                            onInstalledMethod?.isAccessible = true
                            onInstalledMethod?.invoke(listener)
                        } catch (ignored: Throwable) { }
                    }
                    return null
                }
            })
        } catch (ignored: Throwable) { }
    }
}
