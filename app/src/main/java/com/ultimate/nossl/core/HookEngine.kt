
package com.ultimate.nossl.core
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement

import com.ultimate.nossl.utils.Logger

import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object HookEngine {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookThreadCreation(lpparam)
    }

    private fun hookThreadCreation(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.Thread",
                lpparam.classLoader,
                "start",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val thread = param.thisObject as Thread
                        val name = thread.name
                        if (name.contains("ssl", ignoreCase = true) || 
                            name.contains("cert", ignoreCase = true)) {
                            Logger.i("SSL-related thread detected: $name")
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Logger.e("Thread hook failed", e)
        }
    }
}
