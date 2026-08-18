package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class CronetHook {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCronetBuilders(lpparam)
    }

    private fun hookCronetBuilders(lpparam: XC_LoadPackage.LoadPackageParam) {
        val builders = listOf(
            "org.chromium.net.CronetEngine\$Builder",
            "org.chromium.net.impl.CronetEngineBuilderImpl",
            "org.chromium.net.impl.NativeCronetEngineBuilderWithLibraryLoaderImpl",
            "org.chromium.net.impl.JavaCronetEngineBuilderImpl",
            "org.chromium.net.impl.NativeCronetEngineBuilderImpl"
        )
        builders.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                
                XposedBridge.hookAllMethods(clazz, "build", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            XposedHelpers.callMethod(param.thisObject, "enablePublicKeyPinningBypassForLocalTrustAnchors", true)
                        } catch (ignored: Throwable) {}
                    }
                })

                XposedBridge.hookAllMethods(
                    clazz,
                    "enablePublicKeyPinningBypassForLocalTrustAnchors",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            if (param.args.isNotEmpty()) {
                                param.args[0] = true
                            }
                        }
                    }
                )

                XposedBridge.hookAllMethods(clazz, "addPublicKeyPins", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("Cronet", "$clsName.addPublicKeyPins blocked")
                        return param.thisObject
                    }
                })
            } catch (ignored: Throwable) { }
        }
    }
}
