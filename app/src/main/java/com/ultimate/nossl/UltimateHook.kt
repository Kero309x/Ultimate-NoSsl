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
                Logger.e("Failed to self-hook isModuleActive", e)
            }
            return
        }

        if (!ConfigManager.isTargetAppEnabled(lpparam.packageName)) {
            return
        }

        Logger.i("🚀 ULTIMATE MODULE LOADING: ${lpparam.packageName}")

        // Initialize Native hooks (ShadowHook & memory patching)
        try {
            System.loadLibrary("nossl")
            initNative()
            Logger.hook("Native", "Native engine initialized for ${lpparam.packageName}")
        } catch (e: Throwable) {
            Logger.w("Native engine load skipped: ${e.message}")
        }

        fun safeInit(hookId: String, action: () -> Unit) {
            if (!ConfigManager.isHookEnabled(hookId)) return
            try {
                action()
            } catch (t: Throwable) {
                Logger.w("Hook $hookId skipped for ${lpparam.packageName}: ${t.message}")
            }
        }

        // Phase 0: Core engine & Dynamic Scanner
        safeInit("HookEngine") { HookEngine.init(lpparam) }
        safeInit("ClassScanner") { ClassScanner().init(lpparam) }
        safeInit("ConstructorWatcher") { ConstructorWatcher().init(lpparam) }

        // Phase 1: System-level
        safeInit("SystemSSL") { SystemSSLHook().init(lpparam) }
        safeInit("GMS") { GmsHook().init(lpparam) }

        // Phase 2: Native layer
        safeInit("NativeCrypto") { NativeCryptoHook().init(lpparam) }
        safeInit("NativeInterceptor") { NativeInterceptor().init(lpparam) }

        // Phase 3: Major HTTP clients & Protocols
        safeInit("OkHttp") { OkHttpHook().init(lpparam) }
        safeInit("OkHttp2") { OkHttp2Hook().init(lpparam) }
        safeInit("Volley") { VolleyHook().init(lpparam) }
        safeInit("ApacheHttp") { ApacheHttpHook().init(lpparam) }
        safeInit("ModernHttp") { ModernHttpHook().init(lpparam) }
        safeInit("Cronet") { CronetHook().init(lpparam) }
        safeInit("ChromiumHook") { ChromiumHook().init(lpparam) }
        safeInit("gRPC") { GrpcHook().init(lpparam) }
        safeInit("WebSocket") { WebSocketHook().init(lpparam) }
        safeInit("Ktor") { KtorHook().init(lpparam) }

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
        safeInit("HPKP") { HPKPHook().init(lpparam) }
        safeInit("Tls13") { Tls13Hook().init(lpparam) }

        // Phase 7: Anti-detection & utilities
        safeInit("ProxyDetection") { ProxyDetectionHook().init(lpparam) }
        safeInit("AntiDetection") { AntiDetectionHook().init(lpparam) }

        // Phase 8: Dynamic & generic
        safeInit("CustomPinning") { CustomPinningHook().init(lpparam) }
        safeInit("GenericHook") { GenericHook().init(lpparam) }

        // Phase 9: GraphQL clients
        safeInit("GraphQL") { GraphQLPinningHook().init(lpparam) }

        Logger.i("✅ ALL HOOKS DEPLOYED FOR: ${lpparam.packageName}")
    }
}
