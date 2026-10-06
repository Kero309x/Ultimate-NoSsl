
package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class VolleyHook : BaseHook() {
    override val id = "VolleyHook"
    override val name = "VolleyHook"
    override val targetFramework = "Unknown"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam) = true

    
    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookHurlStack(lpparam)
    }

    private fun hookHurlStack(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.android.volley.toolbox.HurlStack",
                lpparam.classLoader,
                "createConnection",
                URL::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val conn = param.result
                        if (conn is HttpsURLConnection) {
                            conn.sslSocketFactory = SSLFactory.UNSAFE_SOCKET_FACTORY
                            conn.hostnameVerifier = SSLFactory.UNSAFE_VERIFIER
                            Logger.hook("Volley", "HurlStack HTTPS connection secured with unsafe defaults")
                        }
                    }
                }
            )
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }
}
