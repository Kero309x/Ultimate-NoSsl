package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory
import javax.net.SocketFactory

class WebSocketHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
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
                        } catch (ignored: Throwable) {}
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
        } catch (ignored: Throwable) { }
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
        } catch (ignored: Throwable) { }
    }
}