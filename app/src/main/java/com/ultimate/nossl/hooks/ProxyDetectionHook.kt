package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ProxyDetectionHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        // hookSystemProxyProperties breaks actual system proxy routing (OkHttp uses it).
        // Removed to fix React Native / OkHttp traffic not appearing in proxy tools like Charles/Burp.
        hookNetworkCapabilities(lpparam)
    }

    private fun hookNetworkCapabilities(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClassIfExists(
                "android.net.NetworkCapabilities",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(clazz, "hasTransport", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val transport = param.args[0] as? Int ?: return
                    // TRANSPORT_VPN = 4
                    if (transport == 4) {
                        Logger.i("Anti-VPN bypass: hasTransport(4) -> false")
                        param.result = false
                    }
                }
            })
        } catch (e: Throwable) { }
    }
}
