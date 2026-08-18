package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger
import com.ultimate.nossl.utils.SSLFactory

class GrpcHook {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookOkHttpChannelBuilder(lpparam)
        hookGrpcSecurity(lpparam)
    }

    private fun hookOkHttpChannelBuilder(lpparam: XC_LoadPackage.LoadPackageParam) {
        val builderClasses = listOf(
            "io.grpc.okhttp.OkHttpChannelBuilder",
            "io.grpc.netty.NettyChannelBuilder",
            "io.grpc.cronet.CronetChannelBuilder",
            "io.grpc.internal.ManagedChannelImplBuilder"
        )

        builderClasses.forEach { clsName ->
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: return@forEach

                XposedBridge.hookAllMethods(clazz, "sslSocketFactory", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = SSLFactory.UNSAFE_SOCKET_FACTORY
                        }
                    }
                })

                XposedBridge.hookAllMethods(clazz, "hostnameVerifier", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (param.args.isNotEmpty()) {
                            param.args[0] = SSLFactory.UNSAFE_VERIFIER
                        }
                    }
                })

                XposedBridge.hookAllMethods(clazz, "build", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            XposedHelpers.callMethod(param.thisObject, "sslSocketFactory", SSLFactory.UNSAFE_SOCKET_FACTORY)
                            XposedHelpers.callMethod(param.thisObject, "hostnameVerifier", SSLFactory.UNSAFE_VERIFIER)
                            Logger.hook("gRPC", "$clsName.build injected with unsafe SSL")
                        } catch (ignored: Throwable) {}
                    }
                })
            } catch (ignored: Throwable) { }
        }
    }

    private fun hookGrpcSecurity(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val tlsChannelCls = XposedHelpers.findClassIfExists("io.grpc.okhttp.TlsChannelCredentials", lpparam.classLoader)
            if (tlsChannelCls != null) {
                XposedBridge.hookAllMethods(tlsChannelCls, "create", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Logger.hook("gRPC", "TlsChannelCredentials.create intercepted")
                    }
                })
            }
        } catch (ignored: Throwable) { }
    }
}