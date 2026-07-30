
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class OkHttp2Hook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val pinner = XposedHelpers.findClassIfExists("com.squareup.okhttp.CertificatePinner", lpparam.classLoader)
            if (pinner != null) {
                XposedBridge.hookAllMethods(pinner, "check", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("OkHttp2", "CertificatePinner.check")
                        return null
                    }
                })
            }

            val builder = XposedHelpers.findClassIfExists("com.squareup.okhttp.OkHttpClient", lpparam.classLoader)
            if (builder != null) {
                XposedBridge.hookAllMethods(builder, "setSslSocketFactory", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = com.ultimate.nossl.utils.SSLFactory.UNSAFE_SOCKET_FACTORY
                        }
                    }
                })
                XposedBridge.hookAllMethods(builder, "setHostnameVerifier", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = com.ultimate.nossl.utils.SSLFactory.UNSAFE_VERIFIER
                        }
                    }
                })
            }
        } catch (e: Throwable) { }
    }
}
