package com.ultimate.nossl

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.core.*
import com.ultimate.nossl.core.registry.HookRegistry
import com.ultimate.nossl.hooks.*
import com.ultimate.nossl.utils.Logger

class UltimateHook : IXposedHookLoadPackage {

    private external fun initNative()

    companion object {
        @JvmStatic
        external fun scanFlutterNative()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.ultimate.nossl") {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.ultimate.nossl.ui.MainViewModel",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
                XposedHelpers.findAndHookMethod(
                    "com.ultimate.nossl.utils.ModuleStatus",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
                Logger.i("✅ Self-hooked isModuleActive() -> true")
            } catch (e: Throwable) {
                Logger.e("Failed to self-hook isModuleActive", e)
            }
            return
        }

        if (!ConfigManager.isTargetAppEnabled(lpparam.packageName)) {
            return
        }

        Logger.i("🚀 ULTIMATE MODULE LOADING: ${lpparam.packageName}")

        try {
            System.loadLibrary("nossl")
            initNative()
            Logger.hook("Native", "Native engine initialized for ${lpparam.packageName}")
        } catch (e: Throwable) {
            Logger.w("Native engine load skipped: ${e.message}")
        }

        // Register all migrated hooks
        HookRegistry.register(SystemSSLHook())
        HookRegistry.register(GmsHook())
        HookRegistry.register(NativeCryptoHook())
        HookRegistry.register(OkHttpHook())
        HookRegistry.register(OkHttp2Hook())
        HookRegistry.register(VolleyHook())
        HookRegistry.register(ApacheHttpHook())
        HookRegistry.register(ModernHttpHook())
        HookRegistry.register(CronetHook())
        HookRegistry.register(ChromiumHook())
        HookRegistry.register(GrpcHook())
        HookRegistry.register(WebSocketHook())
        HookRegistry.register(KtorHook())
        HookRegistry.register(WebViewHook())
        HookRegistry.register(FlutterHook())
        HookRegistry.register(ReactNativeHook())
        HookRegistry.register(XamarinHook())
        HookRegistry.register(UnityHook())
        HookRegistry.register(CordovaHook())
        HookRegistry.register(ConscryptHook())
        HookRegistry.register(BoringSSLHook())
        HookRegistry.register(NetworkSecurityHook())
        HookRegistry.register(CertificateTransparencyHook())
        HookRegistry.register(HPKPHook())
        HookRegistry.register(Tls13Hook())
        HookRegistry.register(ProxyDetectionHook())
        HookRegistry.register(AntiDetectionHook())
        HookRegistry.register(CustomPinningHook())
        HookRegistry.register(GenericHook())
        HookRegistry.register(GraphQLPinningHook())

        // Execute them all safely via the new registry
        HookRegistry.executeAll(lpparam)

        // Utility scanners that haven't been ported yet
        if (ConfigManager.isHookEnabled("ClassScanner")) {
            try { ClassScanner().init(lpparam) } catch (t: Throwable) {}
        }
        if (ConfigManager.isHookEnabled("ConstructorWatcher")) {
            try { ConstructorWatcher().init(lpparam) } catch (t: Throwable) {}
        }

        Logger.i("✅ ALL HOOKS DEPLOYED FOR: ${lpparam.packageName}")
    }
}
