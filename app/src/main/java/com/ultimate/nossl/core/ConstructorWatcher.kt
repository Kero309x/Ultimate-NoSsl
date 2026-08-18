
package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory

class ConstructorWatcher {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        watchOkHttpBuilders(lpparam)
        watchHttpsURLConnection(lpparam)
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
                        try {
                            val builder = param.thisObject
                            XposedHelpers.callMethod(builder, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
                            XposedHelpers.callMethod(builder, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
                        } catch (ignored: Throwable) {}
                    }
                })
            } catch (ignored: Throwable) { }
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
        } catch (ignored: Throwable) { }
    }
}
