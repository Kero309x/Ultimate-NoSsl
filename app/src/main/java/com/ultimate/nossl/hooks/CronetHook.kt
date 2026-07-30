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
        hookUrlRequest(lpparam)
        hookExperimentalEngine(lpparam)
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
                        Logger.hook("Cronet", "$clsName.build")
                    }
                })
                XposedBridge.hookAllMethods(
                    clazz,
                    "enablePublicKeyPinningBypassForLocalTrustAnchors",
                    object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            return param.thisObject
                        }
                    }
                )
                XposedBridge.hookAllMethods(clazz, "addPublicKeyPins", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("Cronet", "addPublicKeyPins blocked")
                        return param.thisObject
                    }
                })
                XposedBridge.hookAllMethods(clazz, "enableNetworkQualityEstimator", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return param.thisObject
                    }
                })
                
                // Add explicit bypass for HttpCache and other strict mode settings if possible
                XposedBridge.hookAllMethods(clazz, "enableHttpCache", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // param.args[0] = 0 // DISABLED
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookUrlRequest(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val requestBuilder = XposedHelpers.findClassIfExists(
                "org.chromium.net.UrlRequest\$Builder",
                lpparam.classLoader
            ) ?: return
            XposedBridge.hookAllMethods(requestBuilder, "build", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    Logger.hook("Cronet", "UrlRequest.Builder.build")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookExperimentalEngine(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val experimental = XposedHelpers.findClassIfExists(
                "org.chromium.net.ExperimentalCronetEngine",
                lpparam.classLoader
            ) ?: return
            XposedBridge.hookAllMethods(experimental, "newUrlRequestBuilder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.i("Cronet Experimental engine request created")
                }
            })
        } catch (e: Throwable) { }
    }
}
