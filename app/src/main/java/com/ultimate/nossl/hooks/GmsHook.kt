package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class GmsHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookProviderInstaller(lpparam)
        hookGoogleApiAvailability(lpparam)
        hookPhenotypeFlags(lpparam)
    }

    private fun hookProviderInstaller(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val providerInstallerCls = XposedHelpers.findClassIfExists(
                "com.google.android.gms.security.ProviderInstaller",
                lpparam.classLoader
            )
            if (providerInstallerCls != null) {
                // Hook installIfNeeded -> replace with no-op
                XposedBridge.hookAllMethods(providerInstallerCls, "installIfNeeded", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("GMS", "ProviderInstaller.installIfNeeded bypassed")
                        return null
                    }
                })

                // Hook installIfNeededAsync -> call onProviderInstalled on the listener
                XposedBridge.hookAllMethods(providerInstallerCls, "installIfNeededAsync", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        Logger.hook("GMS", "ProviderInstaller.installIfNeededAsync bypassed")
                        val listener = param.args.getOrNull(1)
                        if (listener != null) {
                            try {
                                val onInstalledMethod = listener.javaClass.declaredMethods.firstOrNull { 
                                    it.name == "onProviderInstalled" 
                                }
                                onInstalledMethod?.isAccessible = true
                                onInstalledMethod?.invoke(listener)
                            } catch (e: Throwable) { }
                        }
                        return null
                    }
                })
            }
        } catch (e: Throwable) { }
    }

    private fun hookGoogleApiAvailability(lpparam: XC_LoadPackage.LoadPackageParam) {
        val availClasses = listOf(
            "com.google.android.gms.common.GoogleApiAvailability",
            "com.google.android.gms.common.GooglePlayServicesUtil",
            "com.google.android.gms.common.GooglePlayServicesUtilLight"
        )

        availClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "isGooglePlayServicesAvailable", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return 0 // ConnectionResult.SUCCESS
                    }
                })
            } catch (e: Throwable) { }
        }
    }

    private fun hookPhenotypeFlags(lpparam: XC_LoadPackage.LoadPackageParam) {
        val phenotypeClasses = listOf(
            "com.google.android.gms.phenotype.Phenotype",
            "com.google.android.gms.internal.phenotype.FlagRegistrar",
            "com.google.android.gms.flags.FlagStore"
        )

        phenotypeClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                XposedBridge.hookAllMethods(clazz, "register", object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        return null
                    }
                })
            } catch (e: Throwable) { }
        }
    }
}
