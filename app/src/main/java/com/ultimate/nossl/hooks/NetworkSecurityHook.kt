
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class NetworkSecurityHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNetworkSecurityConfig(lpparam)
        hookTrustedCertificateStore(lpparam)
    }

    private fun hookNetworkSecurityConfig(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "android.security.net.config.NetworkSecurityConfig",
            "android.security.net.config.NetworkSecurityTrustManager",
            "android.security.net.config.TrustedCertificateStore",
            "android.security.net.config.ManifestConfigSource",
            "android.security.net.config.XmlConfigSource"
        )

        classes.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach

                when {
                    clsName.contains("TrustManager") -> {
                        XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                Logger.hook("NetworkSecurity", "$clsName.checkServerTrusted")
                                val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                                if (returnType == Void.TYPE) return null
                                return param.args[0]
                            }
                        })
                    }
                    clsName.contains("TrustedCertificateStore") -> {
                        XposedBridge.hookAllMethods(clazz, "findAllIssuers", object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any {
                                return emptySet<java.security.cert.X509Certificate>()
                            }
                        })
                    }
                    else -> {
                        XposedBridge.hookAllMethods(clazz, "isCleartextTrafficPermitted", object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any = true
                        })
                    }
                }
            } catch (e: Throwable) { }
        }
    }

    private fun hookTrustedCertificateStore(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val tmi = XposedHelpers.findClassIfExists("com.android.org.conscrypt.TrustManagerImpl", lpparam.classLoader)
            if (tmi != null) {
                XposedBridge.hookAllMethods(tmi, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("NetworkSecurity", "TrustManagerImpl.checkServerTrusted bypassed")
                        return param.args[0]
                    }
                })
            }

            XposedHelpers.findAndHookMethod(
                "android.security.net.config.TrustedCertificateStoreAdapter",
                lpparam.classLoader,
                "getTrustAnchors",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return emptySet<Any>()
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookManifestConfig(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.security.net.config.ManifestConfigSource",
                lpparam.classLoader,
                "getConfigSource",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("NetworkSecurity", "ManifestConfigSource.getConfigSource")
                        return null
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
