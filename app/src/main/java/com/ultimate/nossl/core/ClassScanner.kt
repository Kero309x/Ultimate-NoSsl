
package com.ultimate.nossl.core
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.ReflectionUtil
import dalvik.system.BaseDexClassLoader

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ClassScanner {
    
    private val sslPatterns = listOf(
        ".*TrustManager.*", ".*CertificatePinner.*", ".*SSLSocketFactory.*",
        ".*HostnameVerifier.*", ".*CertificateChainCleaner.*", ".*SSL.*Verifier.*",
        ".*Pinning.*", ".*NetworkSecurity.*", ".*X509.*Trust.*", ".*Cert.*Validator.*",
        ".*Ssl.*Error.*", ".*TLS.*", ".*Conscrypt.*", ".*BoringSSL.*", ".*Trust.*Anchor.*",
        ".*SecurityContext.*", ".*CertificateVerification.*", ".*ClientCertificate.*",
        ".*Flutter.*Security.*", ".*Flutter.*SSL.*", ".*Dart.*Security.*",
        ".*HttpClient.*", ".*Http.*Security.*", ".*Tls.*Verifier.*",
        ".*SSL.*Policy.*", ".*Security.*Manager.*", ".*X509.*Extended.*"
    )

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookDexClassLoader(lpparam)
        hookPathClassLoader(lpparam)
        hookInMemoryDexClassLoader(lpparam)
        
        // Scan initial loader
        try {
            if (lpparam.classLoader is BaseDexClassLoader) {
                scanLoader(lpparam.classLoader as BaseDexClassLoader, lpparam)
            }
        } catch (e: Throwable) { }
    }

    private fun hookDexClassLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookConstructor(
                "dalvik.system.DexClassLoader",
                lpparam.classLoader,
                String::class.java, String::class.java, String::class.java, ClassLoader::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        scanLoader(param.thisObject as BaseDexClassLoader, lpparam)
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookPathClassLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookConstructor(
                "dalvik.system.PathClassLoader",
                lpparam.classLoader,
                String::class.java, ClassLoader::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        scanLoader(param.thisObject as BaseDexClassLoader, lpparam)
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun hookInMemoryDexClassLoader(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookConstructor(
                "dalvik.system.InMemoryDexClassLoader",
                lpparam.classLoader,
                "java.nio.ByteBuffer", ClassLoader::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("InMemoryDexClassLoader detected - possible obfuscated/DEX-protected app")
                    }
                }
            )
        } catch (e: Throwable) { }
    }

    private fun scanLoader(loader: BaseDexClassLoader, lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val pathList = ReflectionUtil.getField(loader, "pathList") ?: return
            val dexElements = ReflectionUtil.getField(pathList, "dexElements") as? Array<*> ?: return

            dexElements.forEach { element ->
                try {
                    val dexFile = ReflectionUtil.getField(element!!, "dexFile") ?: return@forEach
                    val entries = dexFile.javaClass.getDeclaredMethod("entries").invoke(dexFile) as java.util.Enumeration<String>

                    while (entries.hasMoreElements()) {
                        val className = entries.nextElement()
                        sslPatterns.forEach { pattern ->
                            if (className.matches(Regex(pattern))) {
                                Logger.i("SCANNER: Found SSL class: $className")
                                hookClassDynamically(className, loader, lpparam)
                            }
                        }
                        
                        // Also hook classes that might be implementing SSL interfaces directly
                        if (className.startsWith(lpparam.packageName)) {
                            try {
                                val clazz = Class.forName(className, false, loader)
                                if (javax.net.ssl.X509TrustManager::class.java.isAssignableFrom(clazz) ||
                                    javax.net.ssl.HostnameVerifier::class.java.isAssignableFrom(clazz)) {
                                    Logger.i("SCANNER: Found app-specific trust/verifier: $className")
                                    hookClassDynamically(className, loader, lpparam)
                                }
                            } catch (e: Throwable) { }
                        }
                    }
                } catch (e: Throwable) { }
            }
        } catch (e: Throwable) {
            Logger.e("Class scan failed", e)
        }
    }

    private fun hookClassDynamically(className: String, loader: ClassLoader, lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = Class.forName(className, false, loader)
            clazz.declaredMethods.forEach { method ->
                val methodModifiers = method.modifiers
                if (java.lang.reflect.Modifier.isAbstract(methodModifiers)) return@forEach

                val methodName = method.name
                val returnType = method.returnType

                if (methodName.contains("check", ignoreCase = true) && methodName.contains("Trusted", ignoreCase = true)) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            Logger.i("DYNAMIC: ${clazz.name}.${method.name} bypassed")
                            when {
                                returnType == java.lang.Boolean.TYPE || returnType == java.lang.Boolean::class.java -> param.result = true
                                returnType == java.lang.Void.TYPE -> param.result = null
                                java.util.List::class.java.isAssignableFrom(returnType) -> param.result = emptyList<Any>()
                                returnType.isArray -> param.result = java.lang.reflect.Array.newInstance(returnType.componentType, 0)
                            }
                        }
                    })
                } else if (methodName.contains("verify", ignoreCase = true) && method.parameterTypes.isNotEmpty()) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            Logger.i("DYNAMIC: ${clazz.name}.${method.name} -> true")
                            when {
                                returnType == java.lang.Boolean.TYPE || returnType == java.lang.Boolean::class.java -> param.result = true
                                returnType == java.lang.Void.TYPE -> param.result = null
                                java.util.List::class.java.isAssignableFrom(returnType) -> param.result = emptyList<Any>()
                                returnType.isArray -> param.result = java.lang.reflect.Array.newInstance(returnType.componentType, 0)
                            }
                        }
                    })
                }
            }
        } catch (e: Throwable) { }
    }
}
