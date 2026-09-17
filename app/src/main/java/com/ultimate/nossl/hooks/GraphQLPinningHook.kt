package com.ultimate.nossl.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

class GraphQLPinningHook {

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookApolloHttp(lpparam)
        hookApolloOkHttp(lpparam)
        hookApolloInterceptor(lpparam)
        hookApolloCertificatePinner(lpparam)
        hookApolloWebSocket(lpparam)
        hookKtorGraphQL(lpparam)
    }

    private fun hookApolloHttp(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val httpTransport = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.http.HttpNetworkTransport",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(httpTransport, "newBuilder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("GraphQL", "Apollo HttpNetworkTransport.newBuilder hooked")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookApolloOkHttp(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val okHttpEngine = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.okhttp.OkHttpEngine",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(okHttpEngine, "newBuilder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("GraphQL", "Apollo OkHttpEngine.newBuilder hooked")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookApolloInterceptor(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val interceptor = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.http.HttpInterceptor",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(interceptor, "intercept", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("GraphQL", "Apollo HttpInterceptor.intercept hooked")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookApolloCertificatePinner(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val certificatePinner = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.CertificatePinner",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(certificatePinner, "check", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    Logger.hook("GraphQL", "Apollo CertificatePinner.check -> bypassed")
                    return true
                }
            })

            XposedBridge.hookAllMethods(certificatePinner, "checkHostname", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    return true
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookApolloWebSocket(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val webSocketTransport = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.network.ws.WebSocketNetworkTransport",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(webSocketTransport, "newBuilder", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("GraphQL", "Apollo WebSocketNetworkTransport.newBuilder hooked")
                }
            })
        } catch (e: Throwable) { }
    }

    private fun hookKtorGraphQL(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val ktorGraphQL = XposedHelpers.findClassIfExists(
                "com.apollographql.apollo3.ktor.KtorGraphQLClient",
                lpparam.classLoader
            ) ?: return

            XposedBridge.hookAllMethods(ktorGraphQL, "execute", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    Logger.hook("GraphQL", "KtorGraphQLClient.execute hooked")
                }
            })
        } catch (e: Throwable) { }
    }
}
