package com.ultimate.nossl.core.api

import de.robv.android.xposed.callbacks.XC_LoadPackage

enum class HookStatus {
    UNINITIALIZED,
    INITIALIZED,
    ACTIVE,
    FAILED,
    UNSUPPORTED
}

data class HookDiagnostics(
    val hookId: String,
    val name: String,
    var status: HookStatus = HookStatus.UNINITIALIZED,
    var failureReason: String? = null,
    var error: Throwable? = null,
    val logs: MutableList<String> = mutableListOf()
)

interface Hook {
    val id: String
    val name: String
    val targetFramework: String
    val diagnostics: HookDiagnostics

    /**
     * Called early to verify if the hook is supported in this target app.
     */
    fun isSupported(lpparam: XC_LoadPackage.LoadPackageParam): Boolean

    /**
     * Perform initialization, class lookup, and hook installation.
     */
    fun install(lpparam: XC_LoadPackage.LoadPackageParam)
}
