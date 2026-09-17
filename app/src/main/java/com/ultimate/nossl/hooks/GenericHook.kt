package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class GenericHook {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookGenericCertValidators(lpparam)
    }

    private fun hookGenericCertValidators(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classPatterns = listOf(
            "com.android.org.bouncycastle.jsse.provider.ProvX509TrustManager",
            "org.bouncycastle.jsse.provider.ProvX509TrustManager",
            "javax.net.ssl.HostnameVerifier"
        )

        classPatterns.forEach { cls ->
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                
                if (cls.contains("HostnameVerifier")) {
                    XposedBridge.hookAllMethods(clazz, "verify", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any = true
                    })
                } else {
                    XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Logger.hook("GenericJSSE", "checkServerTrusted bypassed")
                            val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                            return when {
                                returnType == Void.TYPE -> null
                                returnType != null && java.util.List::class.java.isAssignableFrom(returnType) -> {
                                    val certs = param.args.firstOrNull() as? Array<*>
                                    certs?.filterIsInstance<java.security.cert.X509Certificate>()?.toList() ?: emptyList<java.security.cert.X509Certificate>()
                                }
                                else -> param.args.firstOrNull()
                            }
                        }
                    })
                    XposedBridge.hookAllMethods(clazz, "checkClientTrusted", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Logger.hook("GenericJSSE", "checkClientTrusted bypassed")
                            return null
                        }
                    })
                }
            } catch (e: Throwable) { }
        }
    }
}
