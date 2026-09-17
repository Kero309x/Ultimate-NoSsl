package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class GraphQLPinningHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookApolloCertificatePinner(lpparam)
    }

    private fun hookApolloCertificatePinner(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val certificatePinner = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.CertificatePinner",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(certificatePinner, "check", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("GraphQL", "Apollo CertificatePinner.check -> bypassed")
                    return true
                }
            })

            XposedBridge.hookAllMethods(certificatePinner, "checkHostname", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (e: Throwable) { }
    }
}
