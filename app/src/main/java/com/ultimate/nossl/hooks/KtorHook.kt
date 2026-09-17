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
                        Logger.hook("Ktor", "AndroidEngineConfig.sslManager injected")
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
                    Logger.hook("Ktor", "OkHttpConfig constructed - SSL will be bypassed via OkHttpHook")
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

            XposedBridge.hookAllConstructors(cioEngineCls, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("Ktor", "CIOEngineConfig constructed - SSL will be bypassed via native hooks")
                }
            })
        } catch (ignored: Throwable) { }
    }
}
