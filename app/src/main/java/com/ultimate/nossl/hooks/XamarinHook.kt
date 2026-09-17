
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger


import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XamarinHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookMonoRuntime(lpparam)
        hookXamarinHttp(lpparam)
        hookX509Extensions(lpparam)
    }

    private fun hookMonoRuntime(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val mono = XposedHelpers.findClassIfExists("mono.android.Runtime", lpparam.classLoader) ?: return
            XposedBridge.hookAllMethods(mono, "register", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("Xamarin", "mono.android.Runtime.register")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookXamarinHttp(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "android.net.http.X509TrustManagerExtensions",
            "mono.java.security.cert.X509Certificate"
        )

        classes.forEach { cls ->
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("Xamarin", "$cls.checkServerTrusted")
                        return (param.args.firstOrNull() as? Array<*>)?.filterIsInstance<java.security.cert.X509Certificate>()?.toList() ?: emptyList<java.security.cert.X509Certificate>()
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookX509Extensions(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.net.http.X509TrustManagerExtensions",
                lpparam.classLoader,
                "checkServerTrusted",
                Class.forName("[Ljava.security.cert.X509Certificate;"),
                String::class.java,
                String::class.java,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("Xamarin", "X509TrustManagerExtensions.checkServerTrusted")
                        val certs = param.args[0] as? Array<*>
                        return certs?.filterIsInstance<java.security.cert.X509Certificate>() ?: emptyList<java.security.cert.X509Certificate>()
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
