package com.ultimate.nossl.core.api

import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

abstract class BaseHook : Hook {
    override val diagnostics: HookDiagnostics by lazy {
        HookDiagnostics(hookId = id, name = name)
    }

    override fun install(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!isSupported(lpparam)) {
            diagnostics.status = HookStatus.UNSUPPORTED
            return
        }
        
        diagnostics.status = HookStatus.INITIALIZED
        try {
            onInstall(lpparam)
            if (diagnostics.status != HookStatus.FAILED) {
                diagnostics.status = HookStatus.ACTIVE
                Logger.hook(id, "$name successfully activated.")
            }
        } catch (t: Throwable) {
            diagnostics.status = HookStatus.FAILED
            diagnostics.error = t
            diagnostics.failureReason = t.message ?: "Unknown error during installation"
            Logger.e("Failed to install hook: $name", t)
        }
    }

    /**
     * Implementing classes should perform their Xposed hooking here.
     * Throw exceptions for hard failures, or manually set diagnostics.status = HookStatus.FAILED
     * for soft failures.
     */
    protected abstract fun onInstall(lpparam: XC_LoadPackage.LoadPackageParam)
    
    protected fun logDiagnostic(message: String) {
        diagnostics.logs.add(message)
        Logger.d("[$id] $message")
    }
}
