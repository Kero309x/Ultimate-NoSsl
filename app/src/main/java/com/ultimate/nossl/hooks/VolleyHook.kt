
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory


import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class VolleyHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookHurlStack(lpparam)
        hookHttpClientStack(lpparam)
    }

    private fun hookHurlStack(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.volley.toolbox.HurlStack",
                lpparam.classLoader,
                "createConnection",
                java.net.URL::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        val url = param.args[0] as java.net.URL
                        val conn = url.openConnection() as javax.net.ssl.HttpsURLConnection
                        conn.sslSocketFactory = SSLFactory.UNSAFE_SOCKET_FACTORY
                        conn.hostnameVerifier = SSLFactory.UNSAFE_VERIFIER
                        Logger.hook("Volley", "HurlStack.createConnection")
                        return conn
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookHttpClientStack(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.volley.toolbox.HttpClientStack",
                lpparam.classLoader,
                "performRequest",
                "com.android.volley.Request",
                Map::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("Volley", "HttpClientStack.performRequest")
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
