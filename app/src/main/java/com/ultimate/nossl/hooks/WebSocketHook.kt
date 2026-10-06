package com.ultimate.nossl.hooks

import com.ultimate.nossl.core.api.BaseHook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import javax.net.SocketFactory

class WebSocketHook : BaseHook() {
    override val id = "WebSocketHook"
    override val name = "WebSocketHook"
    override val targetFramework = "Unknown"
    override fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam) = true


    override fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookJavaWebSocket(lpparam)
        hookNvWebSocketClient(lpparam)
    }

    private fun hookJavaWebSocket(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val wsClientCls = XposedHelpers.findClassIfExists("org.java_websocket.client.WebSocketClient", lpparam.classLoader)
            if (wsClientCls != null) {
                XposedBridge.hookAllConstructors(wsClientCls, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            XposedHelpers.callMethod(param.thisObject, "setSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY as SocketFactory)
                            Logger.hook("WebSocket", "Java-WebSocket client injected with unsafe factory")
                        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
                    }
                })

                XposedBridge.hookAllMethods(wsClientCls, "setSocketFactory", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = SSLFactory.UNSAFE_SOCKET_FACTORY
                        }
                    }
                })
            }
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }

    private fun hookNvWebSocketClient(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val nvWsFactory = XposedHelpers.findClassIfExists("com.neovisionaries.ws.client.WebSocketFactory", lpparam.classLoader)
            if (nvWsFactory != null) {
                XposedBridge.hookAllMethods(nvWsFactory, "setSSLSocketFactory", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = SSLFactory.UNSAFE_SOCKET_FACTORY
                        }
                    }
                })

                XposedBridge.hookAllMethods(nvWsFactory, "setSSLContext", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = SSLFactory.UNSAFE_SSL_CONTEXT
                        }
                    }
                })

                XposedBridge.hookAllMethods(nvWsFactory, "setVerifyHostname", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = false
                        }
                    }
                })
            }
        } catch (t: Throwable) { logDiagnostic("Soft fail: ${t.message}") }
    }
}