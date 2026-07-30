package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class OkHttpHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCertificatePinner(lpparam)
        hookHostnameVerifier(lpparam)
        hookCertificateChainCleaner(lpparam)
        hookRealConnection(lpparam)
        hookOkHttp3Builder(lpparam)
    }

    private fun hookCertificatePinner(lpparam: XC_LoadPackage.LoadPackageParam) {
        val pinnerClasses = listOf(
            "okhttp3.CertificatePinner",
            "okhttp3.internal.tls.CertificatePinner"
        )
        pinnerClasses.forEach { cls ->
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "check", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("OkHttp3", "CertificatePinner.check")
                        param.result = null
                    }
                })
                XposedBridge.hookAllMethods(clazz, "findMatchingPins", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return emptyList<Any>()
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookHostnameVerifier(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val verifierCls = XposedHelpers.findClassIfExists("okhttp3.internal.tls.OkHostnameVerifier", lpparam.classLoader)
            if (verifierCls != null) {
                XposedBridge.hookAllMethods(verifierCls, "verify", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("OkHttp3", "OkHostnameVerifier.verify")
                        return true
                    }
                })
            }
        } catch (e: Throwable) { }
    }

    private fun hookCertificateChainCleaner(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cleanerCls = XposedHelpers.findClassIfExists("okhttp3.internal.tls.CertificateChainCleaner", lpparam.classLoader)
            if (cleanerCls != null) {
                XposedBridge.hookAllMethods(cleanerCls, "clean", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("OkHttp3", "CertificateChainCleaner.clean")
                        val list = param.args.firstOrNull() as? List<*> ?: emptyList<Any>()
                        return list
                    }
                })
            }
        } catch (e: Throwable) { }
    }

    private fun hookRealConnection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val connCls = XposedHelpers.findClassIfExists("okhttp3.internal.connection.RealConnection", lpparam.classLoader)
            if (connCls != null) {
                XposedBridge.hookAllMethods(connCls, "isEligible", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true
                    }
                })
            }
        } catch (e: Throwable) { }
    }

    private fun hookOkHttp3Builder(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val builderCls = XposedHelpers.findClassIfExists("okhttp3.OkHttpClient\$Builder", lpparam.classLoader) ?: return
            
            XposedBridge.hookAllMethods(builderCls, "build", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val builder = param.thisObject
                    try {
                        XposedHelpers.callMethod(builder, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY, SSLFactory.TRUST_ALL)
                    } catch (e: Throwable) { }
                    try {
                        XposedHelpers.callMethod(builder, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
                    } catch (e: Throwable) { }
                }
            })

            XposedBridge.hookAllMethods(builderCls, "hostnameVerifier", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.isNotEmpty()) {
                        param.args[0] = SSLFactory.UNSAFE_VERIFIER
                    }
                }
            })

            XposedBridge.hookAllMethods(builderCls, "certificatePinner", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Do not set to null or risky defaults
                }
            })
            
            // Bypass app setting Proxy.NO_PROXY
            XposedBridge.hookAllMethods(builderCls, "proxy", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (param.args.isNotEmpty()) {
                        val proxy = param.args[0] as? java.net.Proxy
                        if (proxy == java.net.Proxy.NO_PROXY) {
                            Logger.i("OkHttp3: Blocked setting Proxy.NO_PROXY")
                            param.args[0] = null // Reset to default ProxySelector
                        }
                    }
                }
            })
        } catch (e: Throwable) { }
    }
}
