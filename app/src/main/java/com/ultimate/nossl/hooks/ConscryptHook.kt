package com.ultimate.nossl.hooks

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
                val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return@forEach
                
                XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("Conscrypt", "$className.checkServerTrusted bypassed")
                        val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                        if (returnType == Void.TYPE) return null
                        if (returnType != null && java.util.List::class.java.isAssignableFrom(returnType)) {
                            val arg0 = param.args.firstOrNull()
                            if (arg0 is Array<*>) return arg0.toList()
                            if (arg0 is List<*>) return arg0
                            return emptyList<Any>()
                        }
                        return param.args.firstOrNull()
                    }
                })

                XposedBridge.hookAllMethods(clazz, "checkClientTrusted", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                        if (returnType == Void.TYPE) return null
                        if (returnType != null && java.util.List::class.java.isAssignableFrom(returnType)) {
                            val arg0 = param.args.firstOrNull()
                            if (arg0 is Array<*>) return arg0.toList()
                            if (arg0 is List<*>) return arg0
                            return emptyList<Any>()
                        }
                        return param.args.firstOrNull()
                    }
                })
                
                XposedBridge.hookAllMethods(clazz, "getTrustedChainForServer", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        val arg0 = param.args.firstOrNull()
                        if (arg0 is Array<*>) return arg0.toList()
                        if (arg0 is List<*>) return arg0
                        return emptyList<Any>()
                    }
                })

            } catch (ignored: Throwable) { }
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
                val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: return@forEach
                
                XposedBridge.hookAllMethods(clazz, "checkTrustedRecursive", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        val arg0 = param.args.firstOrNull()
                        if (arg0 is Array<*>) return arg0.toList()
                        if (arg0 is List<*>) return arg0
                        return emptyList<Any>()
                    }
                })

                XposedBridge.hookAllMethods(clazz, "verifyChain", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        val arg0 = param.args.firstOrNull()
                        if (arg0 is Array<*>) return arg0.toList()
                        if (arg0 is List<*>) return arg0
                        return emptyList<Any>()
                    }
                })
            } catch (ignored: Throwable) { }
        }
    }
}
