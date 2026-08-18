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
        hookOkHttpClientProvider(lpparam)
        hookFlipper(lpparam)
        hookSSLPinningModules(lpparam)
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
                        val client = param.result ?: return
                        patchExistingClient(client)
                        Logger.hook("ReactNative", "$clsName.createClient bypassed")
                    }
                })

                XposedBridge.hookAllMethods(clazz, "getOkHttpClient", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val client = param.result ?: return
                        patchExistingClient(client)
                        Logger.hook("ReactNative", "$clsName.getOkHttpClient bypassed")
                    }
                })
            } catch (ignored: Throwable) { }
        }
    }

    private fun hookFlipper(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val interceptor = XposedHelpers.findClassIfExists(
                "com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor",
                lpparam.classLoader
            ) ?: return
            XposedBridge.hookAllMethods(interceptor, "intercept", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val chain = param.args.firstOrNull() ?: return null
                    val request = XposedHelpers.callMethod(chain, "request")
                    return XposedHelpers.callMethod(chain, "proceed", request)
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookSSLPinningModules(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pinningClasses = listOf(
            "com.reactnative.sslpinning.SSLPinningModule",
            "com.reactnative.sslpinning.PinningHelper",
            "com.levelasquez.androidsslpinning.AndroidSslPinningModule",
            "com.RNFetchBlob.RNFetchBlob",
            "com.RNFetchBlob.RNFetchBlobReq"
        )

        pinningClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                val targetMethods = arrayOf("evaluate", "fetch", "check", "getPinnedGrand", "getCustomOkHttpClient")
                targetMethods.forEach { m ->
                    try {
                        XposedBridge.hookAllMethods(clazz, m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                Logger.hook("RN_SSLPinning", "$clsName.$m intercepted")
                                if (m == "check" || m == "evaluate") {
                                    param.result = true
                                }
                            }
                        })
                    } catch (ignored: Throwable) { }
                }
            } catch (ignored: Throwable) { }
        }
    }

    private fun injectBypass(builder: Any) {
        try {
            XposedHelpers.callMethod(builder, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
            XposedHelpers.callMethod(builder, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
            val pinnerCls = XposedHelpers.findClassIfExists("okhttp3.CertificatePinner", builder.javaClass.classLoader)
            if (pinnerCls != null) {
                val defaultPinner = XposedHelpers.getStaticObjectField(pinnerCls, "DEFAULT")
                if (defaultPinner != null) {
                    XposedHelpers.callMethod(builder, "certificatePinner", defaultPinner)
                }
            }
        } catch (ignored: Throwable) { }
    }

    private fun patchExistingClient(client: Any) {
        try {
            val clientCls = client.javaClass
            fun setFieldIfExists(name: String, value: Any?) {
                try {
                    val field = clientCls.declaredFields.firstOrNull { it.name == name }
                    if (field != null) {
                        field.isAccessible = true
                        field.set(client, value)
                    }
                } catch (ignored: Throwable) { }
            }

            setFieldIfExists("sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY)
            setFieldIfExists("hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
            val pinnerCls = XposedHelpers.findClassIfExists("okhttp3.CertificatePinner", clientCls.classLoader)
            if (pinnerCls != null) {
                val defaultPinner = XposedHelpers.getStaticObjectField(pinnerCls, "DEFAULT")
                if (defaultPinner != null) {
                    setFieldIfExists("certificatePinner", defaultPinner)
                }
            }
        } catch (ignored: Throwable) { }
    }
}
