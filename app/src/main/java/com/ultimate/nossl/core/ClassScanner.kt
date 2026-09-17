
package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ClassScanner {

    private val scannedClasses = ConcurrentHashMap.newKeySet<String>()
    private val hookedCount = AtomicInteger(0)
    private val maxClassesPerScan = 5000
    private val maxHookAttempts = 200

    private val safePrefixes = setOf(
        "android.", "androidx.", "java.", "javax.", "kotlin.",
        "kotlinx.", "org.jetbrains.", "com.ultimate.nossl.",
        "sun.", "libcore.", "dalvik.", "com.android.internal.",
        "android.support.", "android.arch.", "com.google.android.gms.ads.",
        "com.google.firebase.", "com.facebook.", "com.squareup.okhttp3.",
        "okio.", "com.google.gson.", "org.json.", "com.google.protobuf."
    )

    private val safeSuffixes = setOf(
        "\$Companion", "\$DefaultImpls", "\$WhenMappings",
        "\$1", "\$2", "\$3", "\$4", "\$5", "\$6", "\$7", "\$8", "\$9",
        "\$inlined", "\$lambda"
    )

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
        val name = clazz.name
        if (scannedClasses.contains(name)) return

        if (hookedCount.get() >= maxHookAttempts) return

        if (clazz.isInterface || Modifier.isAbstract(clazz.modifiers)) return

        for (prefix in safePrefixes) {
            if (name.startsWith(prefix)) return
        }

        for (suffix in safeSuffixes) {
            if (name.endsWith(suffix)) return
        }

        if (scannedClasses.size >= maxClassesPerScan) {
            Logger.d("Scanner: reached max classes limit ($maxClassesPerScan)")
            return
        }

        scannedClasses.add(name)

        try {
            if (javax.net.ssl.X509TrustManager::class.java.isAssignableFrom(clazz)) {
                hookTrustManager(clazz, name)
            } else if (javax.net.ssl.HostnameVerifier::class.java.isAssignableFrom(clazz)) {
                hookHostnameVerifier(clazz, name)
            }
        } catch (e: Throwable) {
            Logger.d("Scanner: failed to process $name: ${e.message}")
        }
    }

    private fun hookTrustManager(clazz: Class<*>, name: String) {
        try {
            Logger.hook("Scanner", "Dynamically found TrustManager: $name")
            clazz.declaredMethods.forEach { method ->
                try {
                    if (method.name.startsWith("check") && method.name.endsWith("Trusted")) {
                        method.isAccessible = true
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
                        hookedCount.incrementAndGet()
                    }
                } catch (ignored: Throwable) {}
            }
        } catch (ignored: Throwable) {}
    }

    private fun hookHostnameVerifier(clazz: Class<*>, name: String) {
        try {
            Logger.hook("Scanner", "Dynamically found HostnameVerifier: $name")
            clazz.declaredMethods.forEach { method ->
                try {
                    if (method.name == "verify" && method.returnType == java.lang.Boolean.TYPE) {
                        method.isAccessible = true
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                param.result = true
                            }
                        })
                        hookedCount.incrementAndGet()
                    }
                } catch (ignored: Throwable) {}
            }
        } catch (ignored: Throwable) {}
    }

    fun reset() {
        scannedClasses.clear()
        hookedCount.set(0)
    }

    fun getStats(): String =
        "scanned=${scannedClasses.size}, hooked=${hookedCount.get()}, maxHookAttempts=$maxHookAttempts"
}
