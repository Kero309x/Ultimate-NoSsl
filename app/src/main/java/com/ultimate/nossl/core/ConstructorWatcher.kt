
package com.ultimate.nossl.core
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ConstructorWatcher {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        watchOkHttpBuilders(lpparam)
        watchHttpsURLConnection(lpparam)
    }

    private fun watchTrustManagers(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllConstructors(
                XposedHelpers.findClass("javax.net.ssl.X509TrustManager", lpparam.classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val instance = param.thisObject
                        val clazz = instance.javaClass
                        if (clazz.name == "javax.net.ssl.X509TrustManager") return

                        Logger.i("TrustManager instantiated: ${clazz.name}")
                        clazz.declaredMethods.forEach { method ->
                            if (method.name.contains("Trusted")) {
                                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                                    override fun beforeHookedMethod(param: MethodHookParam) {
                                        Logger.i("WATCHER: ${clazz.name}.${method.name} blocked")
                                        param.result = null
                                    }
                                })
                            }
                        }
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun watchSocketFactories(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllConstructors(
                XposedHelpers.findClass("javax.net.ssl.SSLSocketFactory", lpparam.classLoader),
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("SSLSocketFactory instantiated: ${param.thisObject.javaClass.name}")
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun watchOkHttpBuilders(lpparam: XC_LoadPackage.LoadPackageParam) {
        val builders = listOf(
            "okhttp3.OkHttpClient\$Builder",
            "okhttp3.OkHttpClient.Builder",
            "com.squareup.okhttp.OkHttpClient\$Builder"
        )

        builders.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("OkHttpClient.Builder instantiated")
                        injectUnsafeDefaults(param.thisObject, lpparam)
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun watchHttpsURLConnection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.HttpsURLConnection",
                lpparam.classLoader,
                "getDefaultSSLSocketFactory",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = SSLFactory.UNSAFE_SOCKET_FACTORY
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun injectUnsafeDefaults(builder: Any, lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // Using methods can cause null pointer exceptions if we are hooking them at the same time or passing wrong args
        } catch (e: Throwable) { }
    }
}
