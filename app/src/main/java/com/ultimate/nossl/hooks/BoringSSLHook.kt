
package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class BoringSSLHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookOpenSSLContext(lpparam)
        hookNativeCrypto(lpparam)
        hookSSLUtils(lpparam)
    }

    private fun hookOpenSSLContext(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val ctx = XposedHelpers.findClassIfExists(
                "com.android.org.conscrypt.OpenSSLContextImpl",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(ctx, "engineInit", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("BoringSSL", "OpenSSLContextImpl.engineInit")
                    if (param.args.size >= 2) {
                        param.args[1] = SSLFactory.createEmptyTrustManagerArray()
                    }
                }
            })
        } catch (ignored: Throwable) { }
    }

    private fun hookNativeCrypto(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "org.conscrypt.NativeCrypto",
            "com.android.org.conscrypt.NativeCrypto",
            "com.google.android.gms.org.conscrypt.NativeCrypto"
        )

        val methods = arrayOf(
            "SSL_CTX_set_custom_verify",
            "SSL_set_custom_verify",
            "SSL_CTX_set_verify",
            "SSL_set_verify",
            "SSL_CTX_set_cert_verify_callback",
            "SSL_set_cert_verify_callback",
            "X509_verify_cert"
        )

        classes.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                methods.forEach { method ->
                    XposedBridge.hookAllMethods(clazz, method, object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Logger.native("BoringSSL.$method bypassed")
                            return when (method) {
                                "X509_verify_cert" -> 1
                                else -> null
                            }
                        }
                    })
                }
            } catch (ignored: Throwable) { }
        }
    }

    private fun hookSSLUtils(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sslUtils = XposedHelpers.findClassIfExists(
                "com.android.org.conscrypt.SSLUtils",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(sslUtils, "verifyCertificateChain", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    Logger.hook("BoringSSL", "SSLUtils.verifyCertificateChain bypassed")
                    return null
                }
            })
        } catch (ignored: Throwable) { }
    }
}
