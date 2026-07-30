
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger


import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CustomPinningHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookTrustKit(lpparam)
        hookAppClarity(lpparam)
        hookTink(lpparam)
    }

    private fun hookTrustKit(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "com.datatheorem.android.trustkit.TrustKit",
            "com.datatheorem.android.trustkit.pinning.TrustManagerBuilder"
        )

        classes.forEach { cls ->
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "getTrustManager", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("TrustKit", "getTrustManager bypassed")
                        return com.ultimate.nossl.utils.SSLFactory.TRUST_ALL
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookAppClarity(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClassIfExists(
                "com.appclarity.ssl.PinningTrustManager",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    Logger.hook("AppClarity", "checkServerTrusted bypassed")
                    return null
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookTink(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClassIfExists(
                "com.google.crypto.tink.subtle.EngineFactory",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(clazz, "getInstance", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("Tink", "EngineFactory.getInstance")
                }
            })
        } catch (e: Throwable) { }
    }
}
