
package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CordovaHook : BaseHook() {
    override val id = "CordovaHook"
    override val name = "CordovaHook"
    override val targetFramework = "Unknown"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam) = true

    
    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
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
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
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
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
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
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }
}
