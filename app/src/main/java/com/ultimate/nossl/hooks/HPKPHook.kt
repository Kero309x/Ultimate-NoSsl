package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class HPKPHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookHpkpEnforcement(lpparam)
        hookHpkpHeaderParser(lpparam)
        hookHpkpPolicy(lpparam)
        hookHpkpPin(lpparam)
    }

    private fun hookHpkpEnforcement(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hpkpEnforcement = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.pkp.HPKPEnforcement",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(hpkpEnforcement, "evaluate", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("HPKP", "HPKPEnforcement.evaluate -> bypassed")
                    return true
                }
            })

            XposedBridge.hookAllMethods(hpkpEnforcement, "evaluatePins", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookHpkpHeaderParser(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val headerParser = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.pkp.HPKPHeaderParser",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(headerParser, "parse", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("HPKP", "HPKPHeaderParser.parse -> empty policy")
                    return ""
                }
            })

            XposedBridge.hookAllMethods(headerParser, "parseIncludeSubDomains", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return false
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookHpkpPolicy(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hpkpPolicy = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.pkp.HPKPPolicy",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(hpkpPolicy, "isExpired", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("HPKP", "HPKPPolicy.isExpired -> true (skip HPKP)")
                    return true
                }
            })

            XposedBridge.hookAllMethods(hpkpPolicy, "matches", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookHpkpPin(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val hpkpPin = XposedHelpers.findClassIfExists(
                "org.conscrypt.ct.pkp.HPKPPin",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(hpkpPin, "matches", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (e: Throwable) { }
    }
}
