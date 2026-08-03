package com.ultimate.nossl.core

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.UltimateHook

class NativeInterceptor {
    
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookLibraryLoading(lpparam)
        hookNativeSSLFunctions(lpparam)
    }

    private fun hookLibraryLoading(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllMethods(Runtime::class.java, "loadLibrary0", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val lib = param.args[1] as? String ?: return
                    if (lib.contains("hermes") || lib.contains("react") || lib.contains("crypto") || lib.contains("ssl") || lib.contains("flutter")) {
                        Logger.native("Library loaded: $lib")
                        if (lib.contains("flutter")) {
                            Logger.native("Flutter loaded, triggering native memory scan...")
                            try {
                                UltimateHook.scanFlutterNative()
                            } catch (e: Throwable) {
                                Logger.e("Failed to trigger native Flutter scan", e)
                            }
                        }
                    }
                }
            })
            
            XposedBridge.hookAllMethods(Runtime::class.java, "load0", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val path = param.args[1] as? String ?: return
                    if (path.contains("libreactnative.so") || path.contains("libhermes.so") || path.contains("libflutter.so")) {
                        Logger.native("Native framework loaded: ${path.substringAfterLast('/')}")
                        if (path.contains("flutter")) {
                            Logger.native("Flutter path loaded, triggering native memory scan...")
                            try {
                                UltimateHook.scanFlutterNative()
                            } catch (e: Throwable) {
                                Logger.e("Failed to trigger native Flutter scan", e)
                            }
                        }
                    }
                }
            })
        } catch (e: Throwable) {
            Logger.e("Failed to hook Runtime.loadLibrary0 / load0", e)
        }
    }

    private fun hookNativeSSLFunctions(lpparam: XC_LoadPackage.LoadPackageParam) {
        val nativeClasses = listOf(
            "com.android.org.conscrypt.NativeCrypto",
            "org.conscrypt.NativeCrypto",
            "com.google.android.gms.org.conscrypt.NativeCrypto",
            "org.apache.harmony.xnet.provider.jsse.NativeCrypto",
            "io.netty.handler.ssl.ReferenceCountedOpenSslEngine",
            "org.apache.tomcat.jni.SSL",
            "io.flutter.embedding.engine.FlutterJNI",
            "com.facebook.react.modules.network.NetworkingModule",
            "com.facebook.hermes.unicode.AndroidUnicodeUtils",
            "com.facebook.crypto.module.NativeCryptoModule",
            "com.facebook.react.modules.network.OkHttpClientProvider"
        )

        val methodsToHook = arrayOf(
            "SSL_CTX_set_custom_verify", "SSL_set_custom_verify",
            "SSL_CTX_set_verify", "SSL_set_verify",
            "SSL_do_handshake", "SSL_verify", "X509_verify_cert",
            "SSL_CTX_set_cert_verify_callback", "SSL_set_cert_verify_callback",
            "EVP_PKEY_verify", "RSA_verify", "ECDSA_verify",
            "SSL_get_verify_result", "SSL_get_peer_cert_chain",
            "X509_check_issued", "X509_check_ca", "X509_check_purpose",
            "SSL_use_certificate", "SSL_CTX_use_certificate",
            "SSL_CTX_set_default_verify_paths", "SSL_CTX_load_verify_locations",
            "SSL_get_error", "SSL_SESSION_get_id", "SSL_get_psk_identity",
            "SSL_connect", "SSL_accept", "SSL_read", "SSL_write",
            "SSL_CTX_use_PrivateKey", "SSL_use_PrivateKey",
            "verifyCertificateChain", "checkServerTrusted", "checkClientTrusted",
            "createOkHttpClient", "createOkHttpClientBuilder"
        )

        nativeClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach
                methodsToHook.forEach { method ->
                    try {
                        XposedBridge.hookAllMethods(clazz, method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val result = param.result
                if (method == "SSL_get_verify_result") {
                    param.result = 0 // X509_V_OK
                    Logger.native("$clsName.$method -> 0")
                    return
                }
                
                if (method.contains("verify", ignoreCase = true) || 
                    method == "SSL_do_handshake" || 
                    method == "SSL_connect" || 
                    method == "SSL_accept") {
                    
                    val returnType = (param.method as? java.lang.reflect.Method)?.returnType
                    when (returnType) {
                        java.lang.Integer.TYPE -> {
                            val resInt = result as? Int ?: -1
                            if (resInt <= 0) {
                                param.result = 1
                                Logger.native("$clsName.$method ($resInt) -> 1")
                            }
                        }
                        java.lang.Boolean.TYPE -> {
                            val resBool = result as? Boolean ?: false
                            if (!resBool) {
                                param.result = true
                                Logger.native("$clsName.$method ($resBool) -> true")
                            }
                        }
                        java.lang.Long.TYPE -> {
                            val resLong = result as? Long ?: -1L
                            if (resLong <= 0L) {
                                param.result = 1L
                                Logger.native("$clsName.$method ($resLong) -> 1L")
                            }
                        }
                    }
                }
            }
                        })
                    } catch (e: Throwable) { }
                }
            } catch (e: Throwable) { }
        }
    }
}
