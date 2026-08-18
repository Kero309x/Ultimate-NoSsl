
package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.TrustManager

class SystemSSLHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookSSLContextInit(lpparam)
        hookHttpsURLConnection(lpparam)
        hookSSLParameters(lpparam)
        hookCertPathValidator(lpparam)
    }
 
    private fun hookCertPathValidator(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val cpv = XposedHelpers.findClassIfExists("java.security.cert.CertPathValidator", lpparam.classLoader)
            if (cpv != null) {
                XposedBridge.hookAllMethods(cpv, "validate", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        Logger.hook("SystemSSL", "CertPathValidator.validate bypassed")
                        try {
                            val resultClass = XposedHelpers.findClassIfExists("java.security.cert.PKIXCertPathValidatorResult", lpparam.classLoader)
                            if (resultClass != null) {
                                param.result = XposedHelpers.newInstance(resultClass, null, null, null, null)
                            }
                        } catch (ignored: Throwable) {
                            param.result = null
                        }
                    }
                })
            }
        } catch (ignored: Throwable) {}
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
                        Logger.hook("SSLContext", "init override with TrustAllManager")
                        param.args[1] = SSLFactory.createEmptyTrustManagerArray()
                    }
                }
            )
        } catch (e: Throwable) {
            Logger.e("SSLContext init hook failed", e)
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
        } catch (ignored: Throwable) {}

        try {
            val tmf = XposedHelpers.findClassIfExists("javax.net.ssl.TrustManagerFactory", lpparam.classLoader)
            if (tmf != null) {
                XposedBridge.hookAllMethods(tmf, "getTrustManagers", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return SSLFactory.createEmptyTrustManagerArray()
                    }
                })
            }
        } catch (ignored: Throwable) {}
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

            Logger.i("HttpsURLConnection default socket factory and verifier replaced")
        } catch (e: Throwable) {
            Logger.e("HttpsURLConnection hook failed", e)
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
        } catch (ignored: Throwable) { }
    }
}

