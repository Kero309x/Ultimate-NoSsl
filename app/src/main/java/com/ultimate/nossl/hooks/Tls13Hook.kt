package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class Tls13Hook : BaseHook() {
    override val id = "Tls13Hook"
    override val name = "Tls13Hook"
    override val targetFramework = "Unknown"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam) = true


    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookTls13Handshaker(lpparam)
        hookTls13CipherSuite(lpparam)
        hookTls13KeyShare(lpparam)
        hookTls13CertificateVerify(lpparam)
    }

    private fun hookTls13Handshaker(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val handshaker = XposedHelpers.findClassIfExists(
                "org.conscrypt.ConscryptEngineSocket\$Tls13Handshaker",
                lpparam.classLoader
            ) ?: XposedHelpers.findClassIfExists(
                "com.android.org.conscrypt.ConscryptEngineSocket\$Tls13Handshaker",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(handshaker, "getActiveCipherSuites", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("TLS1.3", "Tls13Handshaker.getActiveCipherSuites -> allow all")
                    return param.result
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookTls13CipherSuite(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cipherSuite = XposedHelpers.findClassIfExists(
                "org.conscrypt.CipherSuite",
                lpparam.classLoader
            ) ?: return

            XposedHelpers.findAndHookMethod(
                cipherSuite,
                "isAvailable",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true
                    }
                }
            )
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookTls13KeyShare(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val keyShare = XposedHelpers.findClassIfExists(
                "org.conscrypt.Tls13KeyShare",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(keyShare, "generate", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("TLS1.3", "Tls13KeyShare.generate -> bypassed")
                    return ""
                }
            })

            XposedBridge.hookAllMethods(keyShare, "compute", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return ByteArray(32)
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookTls13CertificateVerify(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val certVerify = XposedHelpers.findClassIfExists(
                "org.conscrypt.Tls13CertificateVerify",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(certVerify, "verify", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("TLS1.3", "Tls13CertificateVerify.verify -> passed")
                    return true
                }
            })
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }
}
