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
        hookHermesEngine(lpparam)
        hookJSBundleLoader(lpparam)
        hookNetworkingModule(lpparam)
        hookOkHttpClientProvider(lpparam)
        hookWebSocketModule(lpparam)
        hookTurboModules(lpparam)
        hookFlipper(lpparam)
        hookSSLPinningModules(lpparam)
        hookAnimatedNodesManager(lpparam)
    }

    private fun hookAnimatedNodesManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val animManager = XposedHelpers.findClassIfExists(
                "com.facebook.react.animated.NativeAnimatedNodesManager",
                lpparam.classLoader
            ) ?: return

            val methodsToHook = arrayOf("runUpdates", "getValue", "addAnimatedResponse")
            methodsToHook.forEach { m ->
                try {
                    XposedBridge.hookAllMethods(animManager, m, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.hasThrowable()) {
                                val t = param.throwable
                                if (t?.javaClass?.name?.contains("JSApplicationCausedNativeException") == true) {
                                    param.throwable = null
                                }
                            }
                        }
                    })
                } catch (e: Throwable) { }
            }
        } catch (e: Throwable) { }
    }

    private fun hookHermesEngine(lpparam: XC_LoadPackage.LoadPackageParam) {
        val hermesClasses = listOf(
            "com.facebook.hermes.reactnative.HermesExecutor",
            "com.facebook.hermes.reactnative.HermesExecutorFactory",
            "com.facebook.react.jscexecutor.JSCExecutor",
            "com.facebook.react.jscexecutor.JSCExecutorFactory"
        )

        hermesClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach

                val bundleMethods = arrayOf(
                    "loadScriptFromAsset",
                    "loadScriptFromFile",
                    "loadScriptFromNetwork",
                    "loadScript",
                    "evaluateJavaScript"
                )

                bundleMethods.forEach { methodName ->
                    try {
                        XposedBridge.hookAllMethods(clazz, methodName, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                Logger.hook("HermesEngine", "$clsName.$methodName called")
                            }
                        })
                    } catch (e: Throwable) { }
                }
            } catch (e: Throwable) { }
        }
    }

    private fun hookJSBundleLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val loaderCls = XposedHelpers.findClassIfExists(
                "com.facebook.react.bridge.JSBundleLoader",
                lpparam.classLoader
            ) ?: return

            val loaderMethods = arrayOf(
                "createAssetLoader",
                "createFileLoader",
                "createCachedBundleFromNetworkLoader",
                "createCachedFileFromNetworkLoader"
            )

            loaderMethods.forEach { method ->
                try {
                    XposedBridge.hookAllMethods(loaderCls, method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val pathOrUrl = param.args.firstOrNull { it is String } as? String
                            if (pathOrUrl != null) {
                                Logger.hook("JSBundleLoader", "$method: $pathOrUrl")
                            }
                        }
                    })
                } catch (e: Throwable) { }
            }
        } catch (e: Throwable) { }
    }

    private fun hookNetworkingModule(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val networking = XposedHelpers.findClassIfExists(
                "com.facebook.react.modules.network.NetworkingModule",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(networking, "sendRequest", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val url = param.args.getOrNull(1) as? String
                    if (url != null) {
                        Logger.hook("ReactNative", "NetworkingModule.sendRequest -> $url")
                    } else {
                        Logger.hook("ReactNative", "NetworkingModule.sendRequest")
                    }
                }
            })

            XposedBridge.hookAllMethods(networking, "sendRequestInternal", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("ReactNative", "NetworkingModule.sendRequestInternal")
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

                try {
                    val field = clazz.declaredFields.firstOrNull { it.name == "sOkHttpClient" || it.name == "sClient" }
                    if (field != null) {
                        field.isAccessible = true
                        val currentClient = field.get(null)
                        if (currentClient != null) {
                            patchExistingClient(currentClient)
                            Logger.hook("ReactNative", "$clsName static client field patched")
                        }
                    }
                } catch (e: Throwable) { }

            } catch (e: Throwable) { }
        }
    }

    private fun hookWebSocketModule(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wsCls = XposedHelpers.findClassIfExists(
                "com.facebook.react.modules.websocket.WebSocketModule",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(wsCls, "connect", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val url = param.args.firstOrNull { it is String } as? String
                    if (url != null) {
                        Logger.hook("ReactNative", "WebSocketModule.connect -> $url")
                    }
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookTurboModules(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val turboManager = XposedHelpers.findClassIfExists(
                "com.facebook.react.turbomodule.core.TurboModuleManager",
                lpparam.classLoader
            ) ?: return
            XposedBridge.hookAllMethods(turboManager, "getModule", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val name = param.args.firstOrNull() as? String ?: return
                    if (name.contains("Networking") || name.contains("HTTP") || name.contains("Fetch")) {
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
                            }
                        })
                    } catch (e: Throwable) { }
                }
            } catch (e: Throwable) { }
        }
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

            try {
                val pinnerCls = XposedHelpers.findClassIfExists("okhttp3.CertificatePinner", builder.javaClass.classLoader)
                if (pinnerCls != null) {
                    val defaultPinner = XposedHelpers.getStaticObjectField(pinnerCls, "DEFAULT")
                    XposedHelpers.callMethod(builder, "certificatePinner", defaultPinner)
                }
            } catch (e: Throwable) { }

        } catch (e: Throwable) { }
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
                } catch (e: Throwable) { }
            }

            setFieldIfExists("sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY)
            setFieldIfExists("hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)

            try {
                val pinnerCls = XposedHelpers.findClassIfExists("okhttp3.CertificatePinner", clientCls.classLoader)
                if (pinnerCls != null) {
                    val defaultPinner = XposedHelpers.getStaticObjectField(pinnerCls, "DEFAULT")
                    setFieldIfExists("certificatePinner", defaultPinner)
                }
            } catch (e: Throwable) { }

        } catch (e: Throwable) { }
    }
}
