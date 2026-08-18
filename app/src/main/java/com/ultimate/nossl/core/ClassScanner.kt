
package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import java.lang.reflect.Modifier

class ClassScanner {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookClassLoader(lpparam)
    }

    private fun hookClassLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                ClassLoader::class.java,
                "loadClass",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val clazz = param.result as? Class<*> ?: return
                        scanAndHookClass(clazz)
                    }
                }
            )
        } catch (ignored: Throwable) {}
    }

    private fun scanAndHookClass(clazz: Class<*>) {
        if (clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) return
        val name = clazz.name
        if (name.startsWith("android.") || name.startsWith("java.") || name.startsWith("kotlin.") || name.startsWith("com.ultimate.nossl")) {
            return
        }

        try {
            if (javax.net.ssl.X509TrustManager::class.java.isAssignableFrom(clazz)) {
                Logger.hook("Scanner", "Dynamically found TrustManager: $name")
                clazz.declaredMethods.forEach { method ->
                    if (method.name.startsWith("check") && method.name.endsWith("Trusted")) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val returnType = method.returnType
                                if (returnType == java.lang.Void.TYPE) {
                                    param.result = null
                                } else if (java.util.List::class.java.isAssignableFrom(returnType)) {
                                    val certs = param.args.firstOrNull() as? Array<*>
                                    param.result = certs?.toList() ?: emptyList<Any>()
                                }
                            }
                        })
                    }
                }
            } else if (javax.net.ssl.HostnameVerifier::class.java.isAssignableFrom(clazz)) {
                Logger.hook("Scanner", "Dynamically found HostnameVerifier: $name")
                clazz.declaredMethods.forEach { method ->
                    if (method.name == "verify" && method.returnType == java.lang.Boolean.TYPE) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = true
                            }
                        })
                    }
                }
            }
        } catch (ignored: Throwable) {}
    }
}
