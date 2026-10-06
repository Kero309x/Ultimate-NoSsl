
package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CertificateTransparencyHook : BaseHook() {
    override val id = "CertificateTransparencyHook"
    override val name = "CertificateTransparencyHook"
    override val targetFramework = "Unknown"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam) = true

    
    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCTVerifier(lpparam)
        hookCTLogStore(lpparam)
        hookCTPolicy(lpparam)
    }

    private fun hookCTVerifier(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val verifier = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.CTVerifier",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(verifier, "verify", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("CertTransparency", "CTVerifier.verify -> true")
                    return true
                }
            })

            XposedBridge.hookAllMethods(verifier, "verifyCertificateTransparency", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookCTLogStore(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val logStore = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.CTLogStore",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(logStore, "getKnownLogs", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return emptyList<Any>()
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookCTPolicy(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val policy = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.CTPolicy",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(policy, "doesResultConformToPolicy", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }
}
