package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ProxyDetectionHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNetworkCapabilities(lpparam)
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
                    val transport = param.args.firstOrNull() as? Int ?: return
                    // TRANSPORT_VPN = 4 (tell detection apps that VPN transport is absent)
                    if (transport == 4) {
                        Logger.hook("AntiVPN", "hasTransport(TRANSPORT_VPN) -> false")
                        param.result = false
                    }
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookNetworkInterface(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val netInterfaceClass = XposedHelpers.findClassIfExists("java.net.NetworkInterface", lpparam.classLoader)
            if (netInterfaceClass != null) {
                XposedBridge.hookAllMethods(netInterfaceClass, "isUp", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val iface = param.thisObject as? java.net.NetworkInterface ?: return
                        val name = iface.name ?: ""
                        if (name.contains("tun", ignoreCase = true) || name.contains("ppp", ignoreCase = true)) {
                            // If security tools check if tun/vpn interface is UP, report false
                            param.result = false
                        }
                    }
                })
            }
        } catch (ignored: Throwable) { }
    }
}
