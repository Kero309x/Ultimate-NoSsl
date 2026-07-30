package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import android.webkit.SslErrorHandler
import android.webkit.WebView
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class WebViewHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookWebViewClient(lpparam)
        hookSslErrorHandler(lpparam)
        hookWebViewSettings(lpparam)
        hookWebViewClientModern(lpparam)
        hookWebViewFactoryProvider(lpparam)
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
        } catch (e: Throwable) {
            Logger.e("WebViewFactory.getProvider hook failed", e)
        }
    }

    private fun hookWebViewClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.webkit.WebViewClient",
                lpparam.classLoader,
                "onReceivedSslError",
                WebView::class.java,
                SslErrorHandler::class.java,
                "android.net.http.SslError",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("WebView", "onReceivedSslError")
                        (param.args[1] as SslErrorHandler).proceed()
                        param.result = null
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookWebViewClientModern(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.webkit.WebViewClient",
                lpparam.classLoader,
                "onReceivedSslError",
                WebView::class.java,
                "android.webkit.WebResourceRequest",
                "android.webkit.SslErrorHandler",
                "android.net.http.SslError",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("WebView", "onReceivedSslError (modern)")
                        (param.args[2] as SslErrorHandler).proceed()
                        param.result = null
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookSslErrorHandler(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.webkit.SslErrorHandler",
                lpparam.classLoader,
                "cancel",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("SslErrorHandler", "cancel blocked")
                        param.result = null
                    }
                }
            )
        } catch (e: Throwable) { }
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
                        param.args[0] = 0
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
