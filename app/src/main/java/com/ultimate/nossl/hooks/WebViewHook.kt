package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WebViewHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookWebViewSetClient(lpparam)
        hookBaseWebViewClient(lpparam)
        hookWebViewSettings(lpparam)
        hookWebViewFactoryProvider(lpparam)
    }

    private fun hookWebViewSetClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                WebView::class.java,
                "setWebViewClient",
                WebViewClient::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val client = param.args.firstOrNull() as? WebViewClient ?: return
                        hookCustomWebViewClientClass(client.javaClass)
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }

    private fun hookCustomWebViewClientClass(clazz: Class<*>) {
        if (clazz == WebViewClient::class.java) return
        try {
            XposedBridge.hookAllMethods(clazz, "onReceivedSslError", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val handler = param.args.firstOrNull { it is SslErrorHandler } as? SslErrorHandler
                    if (handler != null) {
                        Logger.hook("WebView", "${clazz.name}.onReceivedSslError -> proceed()")
                        handler.proceed()
                        param.result = null
                    }
                }
            })
        } catch (ignored: Throwable) {}
    }

    private fun hookBaseWebViewClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllMethods(
                WebViewClient::class.java,
                "onReceivedSslError",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val handler = param.args.firstOrNull { it is SslErrorHandler } as? SslErrorHandler
                        if (handler != null) {
                            Logger.hook("WebView", "Base WebViewClient.onReceivedSslError -> proceed()")
                            handler.proceed()
                            param.result = null
                        }
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }

    private fun hookWebViewFactoryProvider(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val factoryClass = XposedHelpers.findClassIfExists("android.webkit.WebViewFactory", lpparam.classLoader)
            if (factoryClass != null) {
                XposedBridge.hookAllMethods(
                    factoryClass,
                    "getProvider",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val provider = param.result
                            if (provider != null) {
                                val webViewClassLoader = provider.javaClass.classLoader
                                if (webViewClassLoader != null) {
                                    ChromiumHook.applyWebViewClassLoader(webViewClassLoader)
                                }
                            }
                        }
                    }
                )
            }
        } catch (ignored: Throwable) { }
    }

    private fun hookWebViewSettings(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.webkit.WebSettings",
                lpparam.classLoader,
                "setMixedContentMode",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = 0 // MIXED_CONTENT_ALWAYS_ALLOW
                    }
                }
            )
        } catch (ignored: Throwable) { }
    }
}
