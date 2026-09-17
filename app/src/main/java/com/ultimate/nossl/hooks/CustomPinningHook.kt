
package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import javax.net.ssl.HttpsURLConnection

class CustomPinningHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookTrustKit(lpparam)
        hookAppAuth(lpparam)
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
                        Logger.hook("TrustKit", "$cls.getTrustManager bypassed")
                        return SSLFactory.TRUST_ALL
                    }
                })

                XposedBridge.hookAllMethods(clazz, "getSSLSocketFactory", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("TrustKit", "$cls.getSSLSocketFactory bypassed")
                        return SSLFactory.UNSAFE_SOCKET_FACTORY
                    }
                })
            } catch (ignored: Throwable) { }
        }

        try {
            val pinningTmCls = XposedHelpers.findClassIfExists(
                "com.datatheorem.android.trustkit.pinning.PinningTrustManager",
                lpparam.classLoader
            )
            if (pinningTmCls != null) {
                XposedBridge.hookAllMethods(pinningTmCls, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("TrustKit", "PinningTrustManager.checkServerTrusted bypassed")
                        return param.args.firstOrNull() as? List<*> ?: emptyList<Any>()
                    }
                })
            }
        } catch (ignored: Throwable) {}
    }

    private fun hookAppAuth(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val connectionBuilderCls = XposedHelpers.findClassIfExists(
                "net.openid.appauth.connectivity.DefaultConnectionBuilder",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(connectionBuilderCls, "openConnection", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val conn = param.result
                    if (conn is HttpsURLConnection) {
                        try {
                            conn.sslSocketFactory = SSLFactory.UNSAFE_SOCKET_FACTORY
                            conn.hostnameVerifier = SSLFactory.UNSAFE_VERIFIER
                            Logger.hook("AppAuth", "DefaultConnectionBuilder.openConnection configured with unsafe SSL")
                        } catch (ignored: Throwable) {}
                    }
                }
            })
        } catch (ignored: Throwable) { }
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
                    return param.args.firstOrNull() as? List<*> ?: emptyList<Any>()
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookTink(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val tinkClasses = listOf(
                "com.google.crypto.tink.ssl.DarkByteStreamingAead",
                "com.google.crypto.tink.integration.android.AndroidNetworkSecurityPolicy"
            )
            tinkClasses.forEach { cls ->
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "isCleartextTrafficPermitted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any = true
                })
            }
        } catch (e: Throwable) { }
    }
}
