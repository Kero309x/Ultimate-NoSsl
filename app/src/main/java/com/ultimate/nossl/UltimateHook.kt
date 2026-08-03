package com.ultimate.nossl

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.core.*
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
                Logger.e("Failed to self-hook isModuleActive: ${e.message}")
            }
            return
        }

        Logger.i("🚀 ULTIMATE MODULE LOADING: ${lpparam.packageName}")

        // Initialize Native hooks (ShadowHook)
        try {
            System.loadLibrary("nossl")
            initNative()
            Logger.hook("Native", "ShadowHook initialized for ${lpparam.packageName}")
        } catch (e: Throwable) {
            Logger.w("Could not load native hooks: ${e.message}")
        }

        fun safeInit(name: String, action: () -> Unit) {
            try {
                action()
            } catch (t: Throwable) {
                Logger.w("Hook $name skipped for ${lpparam.packageName}: ${t.message}")
            }
        }

        // Phase 0: Core engine
        safeInit("HookEngine") { HookEngine.init(lpparam) }

        // Phase 1: System-level
        safeInit("SystemSSL") { SystemSSLHook().init(lpparam) }
        safeInit("GMS") { GmsHook().init(lpparam) }

        // Phase 2: Native layer
        safeInit("NativeCrypto") { NativeCryptoHook().init(lpparam) }
        safeInit("NativeInterceptor") { NativeInterceptor().init(lpparam) }

        // Phase 3: Major HTTP clients
        safeInit("OkHttp") { OkHttpHook().init(lpparam) }
        safeInit("OkHttp2") { OkHttp2Hook().init(lpparam) }
        safeInit("Volley") { VolleyHook().init(lpparam) }
        safeInit("ApacheHttp") { ApacheHttpHook().init(lpparam) }
        safeInit("Cronet") { CronetHook().init(lpparam) }
        safeInit("ChromiumHook") { ChromiumHook().init(lpparam) }

        // Phase 4: Web
        safeInit("WebView") { WebViewHook().init(lpparam) }

        // Phase 5: Cross-platform frameworks
        safeInit("Flutter") { FlutterHook().init(lpparam) }
        safeInit("ReactNative") { ReactNativeHook().init(lpparam) }
        safeInit("Xamarin") { XamarinHook().init(lpparam) }
        safeInit("Unity") { UnityHook().init(lpparam) }
        safeInit("Cordova") { CordovaHook().init(lpparam) }

        // Phase 6: SSL implementations
        safeInit("Conscrypt") { ConscryptHook().init(lpparam) }
        safeInit("BoringSSL") { BoringSSLHook().init(lpparam) }
        safeInit("NetworkSecurity") { NetworkSecurityHook().init(lpparam) }
        safeInit("CertificateTransparency") { CertificateTransparencyHook().init(lpparam) }

        // Phase 7: Anti-detection & utilities
        safeInit("ProxyDetection") { ProxyDetectionHook().init(lpparam) }
        safeInit("AntiDetection") { AntiDetectionHook().init(lpparam) }
        safeInit("FileHook") { FileHook().init(lpparam) }

        // Phase 8: Dynamic & generic (last)
        safeInit("CustomPinning") { CustomPinningHook().init(lpparam) }
        safeInit("GenericHook") { GenericHook().init(lpparam) }

        Logger.i("✅ ALL HOOKS DEPLOYED FOR: ${lpparam.packageName}")
    }
}
