
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CordovaHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCordovaWebView(lpparam)
        hookIonicWebView(lpparam)
        hookSystemWebViewClient(lpparam)
    }

    private fun hookCordovaWebView(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.cordova.CordovaWebViewClient",
                lpparam.classLoader,
                "onReceivedSslError",
                "android.webkit.WebView",
                "android.webkit.SslErrorHandler",
                "android.net.http.SslError",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Cordova", "CordovaWebViewClient.onReceivedSslError")
                        XposedHelpers.callMethod(param.args[1], "proceed")
                        return null
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookIonicWebView(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.ionicframework.cordova.webview.IonicWebViewEngine",
                lpparam.classLoader,
                "onReceivedSslError",
                "android.webkit.WebView",
                "android.webkit.SslErrorHandler",
                "android.net.http.SslError",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Ionic", "IonicWebViewEngine.onReceivedSslError")
                        XposedHelpers.callMethod(param.args[1], "proceed")
                        return null
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookSystemWebViewClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.cordova.engine.SystemWebViewClient",
                lpparam.classLoader,
                "onReceivedSslError",
                "android.webkit.WebView",
                "android.webkit.SslErrorHandler",
                "android.net.http.SslError",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Cordova", "SystemWebViewClient.onReceivedSslError")
                        XposedHelpers.callMethod(param.args[1], "proceed")
                        return null
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
