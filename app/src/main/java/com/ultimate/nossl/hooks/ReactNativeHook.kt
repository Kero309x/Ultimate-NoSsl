package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ReactNativeHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNetworkingModule(lpparam)
        hookOkHttpClientProvider(lpparam)
        hookTurboModules(lpparam)
        hookFlipper(lpparam)
    }

    private fun hookNetworkingModule(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val networking = XposedHelpers.findClassIfExists(
                "com.facebook.react.modules.network.NetworkingModule",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(networking, "sendRequest", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("ReactNative", "NetworkingModule.sendRequest")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookOkHttpClientProvider(lpparam: XC_LoadPackage.LoadPackageParam) {
        val providers = listOf(
            "com.facebook.react.modules.network.OkHttpClientProvider",
            "com.facebook.react.modules.network.OkHttpClientFactory",
            "com.facebook.react.modules.network.CustomClientBuilderFactory"
        )

        providers.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                
                XposedBridge.hookAllMethods(clazz, "createClientBuilder", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val builder = param.result ?: return
                        injectBypass(builder)
                        Logger.hook("ReactNative", "$clsName.createClientBuilder bypassed")
                    }
                })

                XposedBridge.hookAllMethods(clazz, "createClient", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.hook("ReactNative", "$clsName.createClient called")
                    }
                })

                XposedBridge.hookAllMethods(clazz, "getOkHttpClient", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.hook("ReactNative", "$clsName.getOkHttpClient called")
                    }
                })

            } catch (e: Throwable) { }
        }
    }

    private fun hookTurboModules(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val turboManager = XposedHelpers.findClassIfExists(
                "com.facebook.react.turbomodule.core.TurboModuleManager",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(turboManager, "getModule", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val name = param.args[0] as? String ?: return
                    if (name.contains("Networking") || name.contains("HTTP")) {
                        Logger.i("TurboModule accessed: $name")
                    }
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookFlipper(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val interceptor = XposedHelpers.findClassIfExists(
                "com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(interceptor, "intercept", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    Logger.hook("ReactNative", "FlipperOkhttpInterceptor.intercept bypassed")
                    val chain = param.args[0]
                    val request = XposedHelpers.callMethod(chain, "request")
                    return XposedHelpers.callMethod(chain, "proceed", request)
                }
            })
        } catch (e: Throwable) { }
    }

    private fun injectBypass(builder: Any) {
        try {
            val methods = builder.javaClass.declaredMethods
            
            val sslMethod = methods.find { m ->
                val p = m.parameterTypes
                p.size == 2 && p[0].name.contains("SSLSocketFactory") && p[1].name.contains("X509TrustManager")
            }
            if (sslMethod != null) {
                sslMethod.isAccessible = true
                sslMethod.invoke(builder, SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
            } else {
                XposedHelpers.callMethod(builder, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
            }

            val verifierMethod = methods.find { m ->
                val p = m.parameterTypes
                p.size == 1 && p[0].name.contains("HostnameVerifier")
            }
            if (verifierMethod != null) {
                verifierMethod.isAccessible = true
                verifierMethod.invoke(builder, SSLFactory.UNSAFE_VERIFIER)
            } else {
                XposedHelpers.callMethod(builder, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
            }
        } catch (e: Throwable) { }
    }
}
