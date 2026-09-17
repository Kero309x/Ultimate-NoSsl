
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger


import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ApacheHttpHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookAbstractVerifier(lpparam)
        hookBrowserCompatHostnameVerifier(lpparam)
        hookStrictHostnameVerifier(lpparam)
        hookDefaultHttpClient(lpparam)
    }

    private fun hookAbstractVerifier(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.http.conn.ssl.AbstractVerifier",
                lpparam.classLoader,
                "verify",
                String::class.java,
                Array<String>::class.java,
                Array<String>::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("ApacheHttp", "AbstractVerifier.verify")
                        return true
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookBrowserCompatHostnameVerifier(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.http.conn.ssl.BrowserCompatHostnameVerifier",
                lpparam.classLoader,
                "verify",
                String::class.java,
                "javax.net.ssl.SSLSession",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookStrictHostnameVerifier(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.http.conn.ssl.StrictHostnameVerifier",
                lpparam.classLoader,
                "verify",
                String::class.java,
                "javax.net.ssl.SSLSession",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookDefaultHttpClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "org.apache.http.impl.client.DefaultHttpClient",
                lpparam.classLoader,
                "execute",
                "org.apache.http.client.methods.HttpUriRequest",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("ApacheHttp", "DefaultHttpClient.execute")
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
