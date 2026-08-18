package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import javax.net.ssl.HttpsURLConnection

class KtorHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookKtorAndroidEngine(lpparam)
        hookKtorOkHttpEngine(lpparam)
        hookKtorCIOEngine(lpparam)
    }

    private fun hookKtorAndroidEngine(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val androidConfigCls = XposedHelpers.findClassIfExists(
                "io.ktor.client.engine.android.AndroidEngineConfig",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllConstructors(androidConfigCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val sslManagerLambda: (HttpsURLConnection) -> Unit = { conn ->
                            conn.sslSocketFactory = SSLFactory.UNSAFE_SOCKET_FACTORY
                            conn.hostnameVerifier = SSLFactory.UNSAFE_VERIFIER
                        }
                        XposedHelpers.callMethod(param.thisObject, "setSslManager", sslManagerLambda)
                        Logger.hook("Ktor", "AndroidEngineConfig.sslManager injected with unsafe defaults")
                    } catch (ignored: Throwable) {}
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookKtorOkHttpEngine(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val okhttpConfigCls = XposedHelpers.findClassIfExists(
                "io.ktor.client.engine.okhttp.OkHttpConfig",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllConstructors(okhttpConfigCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val configBlock: (Any) -> Unit = { builder ->
                            try {
                                XposedHelpers.callMethod(builder, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
                                XposedHelpers.callMethod(builder, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
                            } catch (ignored: Throwable) {}
                        }
                        XposedHelpers.callMethod(param.thisObject, "config", configBlock)
                        Logger.hook("Ktor", "OkHttpConfig injected with unsafe SSL defaults")
                    } catch (ignored: Throwable) {}
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookKtorCIOEngine(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cioEngineCls = XposedHelpers.findClassIfExists(
                "io.ktor.client.engine.cio.CIOEngineConfig",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(cioEngineCls, "https", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("Ktor", "CIOEngineConfig.https intercepted")
                }
            })
        } catch (ignored: Throwable) { }
    }
}