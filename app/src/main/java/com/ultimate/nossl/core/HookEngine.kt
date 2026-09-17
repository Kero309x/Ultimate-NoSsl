package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

object HookEngine {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        Logger.i("HookEngine initializing for ${lpparam.packageName}")
        hookTrustManagers(lpparam)
        hookSSLContext(lpparam)
    }

    private fun hookTrustManagers(lpparam: XC_LoadPackage.LoadPackageParam) {
        val trustManagerClasses = listOf(
            "javax.net.ssl.X509TrustManager"
        )
        trustManagerClasses.forEach { cls ->
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("HookEngine", "$cls.checkServerTrusted bypassed")
                        return null
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookSSLContext(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val sslContext = XposedHelpers.findClassIfExists("javax.net.ssl.SSLContext", lpparam.classLoader) ?: return
            XposedHelpers.findAndHookMethod(
                sslContext, "init",
                null,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.hook("HookEngine", "SSLContext.init intercepted")
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
