
package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger


import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class NativeCryptoHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNativeCryptoClasses(lpparam)
    }

    private fun hookNativeCryptoClasses(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "com.android.org.conscrypt.NativeCrypto",
            "org.conscrypt.NativeCrypto",
            "org.apache.harmony.xnet.provider.jsse.NativeCrypto"
        )

        classes.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                
                XposedBridge.hookAllMethods(clazz, "SSL_CTX_set_verify", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.native("$clsName.SSL_CTX_set_verify")
                        return null
                    }
                })

                XposedBridge.hookAllMethods(clazz, "SSL_set_verify", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.native("$clsName.SSL_set_verify")
                        return null
                    }
                })

                val methodsToReplace = arrayOf(
                    "SSL_CTX_set_cert_verify_callback",
                    "SSL_set_cert_verify_callback",
                    "SSL_CTX_set_custom_verify",
                    "SSL_set_custom_verify"
                )
                methodsToReplace.forEach { method ->
                    XposedBridge.hookAllMethods(clazz, method, object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Logger.native("$clsName.$method")
                            return null
                        }
                    })
                }
            } catch (e: Throwable) { }
        }
    }
}
