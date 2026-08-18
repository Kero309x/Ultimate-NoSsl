
package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class NetworkSecurityHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookNetworkSecurityConfig(lpparam)
    }

    private fun hookNetworkSecurityConfig(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classes = listOf(
            "android.security.net.config.NetworkSecurityConfig",
            "android.security.net.config.NetworkSecurityTrustManager",
            "android.security.net.config.ManifestConfigSource",
            "android.security.net.config.XmlConfigSource"
        )

        classes.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach

                when {
                    clsName.contains("TrustManager") -> {
                        XposedBridge.hookAllMethods(clazz, "checkServerTrusted", object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any? {
                                Logger.hook("NetworkSecurity", "$clsName.checkServerTrusted bypassed")
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
                    }
                    else -> {
                        XposedBridge.hookAllMethods(clazz, "isCleartextTrafficPermitted", object : XC_MethodReplacement() {
                            override fun replaceHookedMethod(param: MethodHookParam): Any = true
                        })
                    }
                }
            } catch (ignored: Throwable) { }
        }
    }
}
