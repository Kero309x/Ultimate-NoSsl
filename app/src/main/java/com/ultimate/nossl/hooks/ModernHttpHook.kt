package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory

class ModernHttpHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookRetrofitBuilder(lpparam)
        hookFuelManager(lpparam)
    }

    private fun hookRetrofitBuilder(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val retrofitBuilderCls = XposedHelpers.findClassIfExists(
                "retrofit2.Retrofit\$Builder",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(retrofitBuilderCls, "client", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val client = param.args.firstOrNull() ?: return
                    try {
                        val clientCls = client.javaClass
                        val sslField = clientCls.declaredFields.firstOrNull { it.name == "sslSocketFactory" }
                        if (sslField != null) {
                            sslField.isAccessible = true
                            sslField.set(client, SSLFactory.UNSAFE_SOCKET_FACTORY)
                        }
                        val verifierField = clientCls.declaredFields.firstOrNull { it.name == "hostnameVerifier" }
                        if (verifierField != null) {
                            verifierField.isAccessible = true
                            verifierField.set(client, SSLFactory.UNSAFE_VERIFIER)
                        }
                        Logger.hook("Retrofit", "Retrofit client injected with unsafe SSL parameters")
                    } catch (ignored: Throwable) {}
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookFuelManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val fuelManagerCls = XposedHelpers.findClassIfExists(
                "com.github.kittinunf.fuel.core.FuelManager",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllConstructors(fuelManagerCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        XposedHelpers.callMethod(param.thisObject, "setSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY)
                        XposedHelpers.callMethod(param.thisObject, "setHostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
                        Logger.hook("Fuel", "FuelManager initialized with unsafe SSL parameters")
                    } catch (ignored: Throwable) {}
                }
            })
        } catch (ignored: Throwable) { }
    }
}