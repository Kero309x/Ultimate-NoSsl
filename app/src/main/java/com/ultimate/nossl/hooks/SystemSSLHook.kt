package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.core.api.BaseHook
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.TrustManager

class SystemSSLHook : BaseHook() {
    override val id = "system_ssl"
    override val name = "System SSL (javax.net.ssl)"
    override val targetFramework = "Android Platform"

    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam): Boolean = true

    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookSSLContextInit(lpparam)
        hookHttpsURLConnection(lpparam)
        hookSSLParameters(lpparam)
    }

    private fun hookSSLContextInit(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.SSLContext",
                lpparam.classLoader,
                "init",
                Array<javax.net.ssl.KeyManager>::class.java,
                Array<TrustManager>::class.java,
                java.security.SecureRandom::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[1] = SSLFactory.createEmptyTrustManagerArray()
                    }
                }
            )
            logDiagnostic("SSLContext.init hooked")
        } catch (t: Throwable) {
            logDiagnostic("SSLContext.init hook failed: ${t.message}")
        }

        try {
            XposedBridge.hookAllMethods(javax.net.ssl.SSLContext::class.java, "getSocketFactory", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result == null) {
                        param.result = SSLFactory.UNSAFE_SOCKET_FACTORY
                    }
                }
            })
            
            XposedBridge.hookAllMethods(javax.net.ssl.SSLSocketFactory::class.java, "getDefault", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any = SSLFactory.UNSAFE_SOCKET_FACTORY
            })
            logDiagnostic("SSLSocketFactory hooked")
        } catch (t: Throwable) {
            logDiagnostic("SSLSocketFactory hook failed: ${t.message}")
        }

        try {
            val tmf = XposedHelpers.findClassIfExists("javax.net.ssl.TrustManagerFactory", lpparam.classLoader)
            if (tmf != null) {
                XposedBridge.hookAllMethods(tmf, "getTrustManagers", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return SSLFactory.createEmptyTrustManagerArray()
                    }
                })
                logDiagnostic("TrustManagerFactory.getTrustManagers hooked")
            }
        } catch (t: Throwable) {
            logDiagnostic("TrustManagerFactory hook failed: ${t.message}")
        }
    }

    private fun hookHttpsURLConnection(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.setStaticObjectField(
                HttpsURLConnection::class.java,
                "defaultSSLSocketFactory",
                SSLFactory.UNSAFE_SOCKET_FACTORY
            )

            XposedBridge.hookAllMethods(HttpsURLConnection::class.java, "getDefaultHostnameVerifier", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any = SSLFactory.UNSAFE_VERIFIER
            })

            XposedBridge.hookAllMethods(HttpsURLConnection::class.java, "setDefaultHostnameVerifier", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = SSLFactory.UNSAFE_VERIFIER
                }
            })

            XposedBridge.hookAllMethods(HttpsURLConnection::class.java, "setSSLSocketFactory", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = SSLFactory.UNSAFE_SOCKET_FACTORY
                }
            })

            XposedBridge.hookAllMethods(HttpsURLConnection::class.java, "setHostnameVerifier", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = SSLFactory.UNSAFE_VERIFIER
                }
            })

            logDiagnostic("HttpsURLConnection factory and verifier replaced")
        } catch (t: Throwable) {
            logDiagnostic("HttpsURLConnection hook failed: ${t.message}")
        }
    }

    private fun hookSSLParameters(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "javax.net.ssl.SSLParameters",
                lpparam.classLoader,
                "setEndpointIdentificationAlgorithm",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[0] = null
                    }
                }
            )
            logDiagnostic("SSLParameters.setEndpointIdentificationAlgorithm hooked")
        } catch (t: Throwable) {
            logDiagnostic("SSLParameters hook failed: ${t.message}")
        }
    }
}
