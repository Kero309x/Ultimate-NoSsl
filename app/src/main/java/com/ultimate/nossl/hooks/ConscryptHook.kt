package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ConscryptHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookConscrypt(lpparam)
        hookAndroidSecurity(lpparam)
    }

    private fun hookAndroidSecurity(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classNames = listOf(
            "android.security.net.config.RootTrustManager",
            "android.security.net.config.NetworkSecurityTrustManager",
            "com.android.org.conscrypt.TrustManagerImpl"
        )
        
        classNames.forEach { className ->
            try {
                val clazz = Class.forName(className, false, lpparam.classLoader) ?: return@forEach
                
                // Hook all checkServerTrusted methods
                XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Conscrypt", "$className.checkServerTrusted bypassed")
                        val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                        if (returnType == Void.TYPE) return null
                        if (returnType == java.util.List::class.java) {
                            val chain = param.args[0] as? Array<*>
                            return if (chain != null) chain.toList() else emptyList<Any>()
                        }
                        return param.args[0]
                    }
                })

                XposedBridge.hookAllMethods(clazz, "checkClientTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                        if (returnType == Void.TYPE) return null
                        if (returnType == java.util.List::class.java) {
                            val chain = param.args[0] as? Array<*>
                            return if (chain != null) chain.toList() else emptyList<Any>()
                        }
                        return param.args[0]
                    }
                })
                
                XposedBridge.hookAllMethods(clazz, "getTrustedChainForServer", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        val chain = param.args[0] as? Array<*>
                        return if (chain != null) chain.toList() else emptyList<Any>()
                    }
                })

            } catch (e: Throwable) { 
                Logger.e("Failed to hook $className", e)
            }
        }
    }

    private fun hookConscrypt(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classNames = listOf(
            "com.android.org.conscrypt.TrustManagerImpl",
            "org.conscrypt.TrustManagerImpl",
            "com.google.android.gms.org.conscrypt.TrustManagerImpl"
        )
        
        classNames.forEach { className ->
            try {
                val clazz = Class.forName(className, false, lpparam.classLoader) ?: return@forEach
                
                // checkTrustedRecursive
                try {
                    XposedBridge.hookAllMethods(clazz, "checkTrustedRecursive", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            val chain = param.args[0] as? Array<*>
                            return if (chain != null) chain.toList() else emptyList<Any>()
                        }
                    })
                } catch (e: Throwable) { }

                // verifyChain
                try {
                    XposedBridge.hookAllMethods(clazz, "verifyChain", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            val chain = param.args[0] as? Array<*>
                            return if (chain != null) chain.toList() else emptyList<Any>()
                        }
                    })
                } catch (e: Throwable) { }
            } catch (e: Throwable) { }
        }
    }
}
