package com.ultimate.nossl.hooks
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger


import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class UnityHook {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookUnityPlayer(lpparam)
        hookUnityWebRequest(lpparam)
    }

    private fun hookUnityPlayer(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val unityPlayer = XposedHelpers.findClassIfExists(
                "com.unity3d.player.UnityPlayer",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(unityPlayer, "UnitySendMessage", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val method = param.args[1] as? String ?: return
                    if (method.contains("Certificate", ignoreCase = true) ||
                        method.contains("SSL", ignoreCase = true)) {
                        Logger.i("Unity SSL message: $method")
                    }
                }
            })
        } catch (e: Throwable) { }

        try {
            val unityWebRequest = XposedHelpers.findClassIfExists(
                "UnityEngine.Networking.UnityWebRequest",
                lpparam.classLoader
            )
            if (unityWebRequest != null) {
                XposedBridge.hookAllMethods(unityWebRequest, "CertificateHandler", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any = true
                })
                Logger.hook("Unity", "UnityWebRequest.CertificateHandler bypassed")
            }
        } catch (e: Throwable) { }
    }

    private fun hookUnityWebRequest(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val certHandlerCls = XposedHelpers.findClassIfExists(
                "UnityEngine.Networking.CertificateHandler",
                lpparam.classLoader
            )
            if (certHandlerCls != null) {
                XposedBridge.hookAllMethods(certHandlerCls, "ReceiveCertificate", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        Logger.hook("Unity", "CertificateHandler.ReceiveCertificate -> approved")
                        return true
                    }
                })
            }
        } catch (e: Throwable) { }

        try {
            XposedHelpers.findAndHookMethod(
                "com.unity3d.player.UnityPlayerActivity",
                lpparam.classLoader,
                "onCreate",
                "android.os.Bundle",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.i("Unity Player Activity started")
                    }
                }
            )
        } catch (e: Throwable) { }
    }
}
