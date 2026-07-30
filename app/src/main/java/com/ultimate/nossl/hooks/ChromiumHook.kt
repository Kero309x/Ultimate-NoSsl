package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ChromiumHook {

    companion object {
        private var hooked = false

        fun applyWebViewClassLoader(classLoader: ClassLoader) {
            if (hooked) return
            hooked = true
            
            val classes = listOf(
                "org.chromium.net.X509Util",
                "com.android.org.chromium.net.X509Util",
                "kV", // Some common obfuscated names? Let's just stick to standard for now.
                "AwContentsStatics"
            )

            classes.forEach { clsName ->
                try {
                    val clazz = XposedHelpers.findClassIfExists(clsName, classLoader) ?: return@forEach
                    XposedBridge.hookAllMethods(clazz, "verifyServerCertificates", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any? {
                            Logger.hook("Chromium", "verifyServerCertificates bypassed in WebView")
                            try {
                                val resultClassName = if (clsName.startsWith("com.android")) {
                                    "com.android.org.chromium.net.AndroidCertVerifyResult"
                                } else {
                                    "org.chromium.net.AndroidCertVerifyResult"
                                }
                                val resultClass = XposedHelpers.findClassIfExists(resultClassName, classLoader)
                                if (resultClass != null) {
                                    try {
                                        return XposedHelpers.newInstance(resultClass, 0)
                                    } catch (e: Throwable) {}
                                    try {
                                        return XposedHelpers.newInstance(resultClass, 0, true, emptyList<Any>())
                                    } catch (e: Throwable) {}
                                }
                            } catch (e: Throwable) {
                                Logger.e("Chromium bypass result failed", e)
                            }
                            return null
                        }
                    })
                    Logger.i("ChromiumHook applied to $clsName in WebView classloader")
                } catch (e: Throwable) {}
            }
        }
    }

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "org.chromium.net.X509Util",
            "com.android.org.chromium.net.X509Util"
        )
        classes.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "verifyServerCertificates", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Chromium", "verifyServerCertificates bypassed")
                        try {
                            val resultClassName = if (clsName.startsWith("com.android")) {
                                "com.android.org.chromium.net.AndroidCertVerifyResult"
                            } else {
                                "org.chromium.net.AndroidCertVerifyResult"
                            }
                            val resultClass = XposedHelpers.findClassIfExists(resultClassName, lpparam.classLoader)
                            if (resultClass != null) {
                                try {
                                    return XposedHelpers.newInstance(resultClass, 0)
                                } catch (e: Throwable) {}
                                try {
                                    return XposedHelpers.newInstance(resultClass, 0, true, emptyList<Any>())
                                } catch (e: Throwable) {}
                            }
                        } catch (e: Throwable) {
                            Logger.e("Chromium bypass result failed", e)
                        }
                        return null
                    }
                })
            } catch (e: Throwable) {}
        }
    }
}
