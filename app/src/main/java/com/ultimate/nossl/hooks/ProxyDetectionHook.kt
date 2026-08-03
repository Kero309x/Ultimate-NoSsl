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
        hookSystemProperties(lpparam)
        hookNetworkInterface(lpparam)
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

    private fun hookSystemProperties(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val systemClass = XposedHelpers.findClassIfExists("java.lang.System", lpparam.classLoader)
            if (systemClass != null) {
                XposedBridge.hookAllMethods(systemClass, "getProperty", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args.firstOrNull()?.toString() ?: return
                        if (key.contains("proxy")) {
                            Logger.i("Anti-Proxy bypass: getProperty($key) -> null")
                            param.result = null
                        }
                    }
                })
            }
        } catch (e: Throwable) { }
    }

    private fun hookNetworkInterface(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val netInterfaceClass = XposedHelpers.findClassIfExists("java.net.NetworkInterface", lpparam.classLoader)
            if (netInterfaceClass != null) {
                XposedBridge.hookAllMethods(netInterfaceClass, "getName", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val name = param.result as? String ?: return
                        if (name.contains("tun") || name.contains("ppp")) {
                            Logger.i("Anti-VPN bypass: NetworkInterface.getName() -> eth0")
                            param.result = "eth0"
                        }
                    }
                })
            }
        } catch (e: Throwable) { }
    }
}
