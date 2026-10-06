package com.ultimate.nossl.core.registry

import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.core.ConfigManager
import com.ultimate.nossl.core.api.Hook
import com.ultimate.nossl.core.api.HookStatus
import com.ultimate.nossl.utils.Logger

object HookRegistry {
    private val hooks = mutableListOf<Hook>()

    fun register(hook: Hook) {
        if (!hooks.any { it.id == hook.id }) {
            hooks.add(hook)
        }
    }

    fun executeAll(lpparam: XC_LoadPackage.LoadPackageParam) {
        Logger.i("Executing HookRegistry for ${lpparam.packageName}")
        
        for (hook in hooks) {
            val legacyHookId = hook.id.replace("Hook", "") // fallback mapping for older config strings
            if (!ConfigManager.isHookEnabled(hook.id) && !ConfigManager.isHookEnabled(legacyHookId)) {
                Logger.i("Hook ${hook.id} is disabled in configuration. Skipping.")
                hook.diagnostics.status = HookStatus.UNSUPPORTED
                continue
            }

            try {
                hook.install(lpparam)
            } catch (e: Throwable) {
                Logger.e("FATAL: Unhandled exception while processing hook ${hook.id}", e)
                hook.diagnostics.status = HookStatus.FAILED
                hook.diagnostics.error = e
                hook.diagnostics.failureReason = e.message ?: "Unhandled crash"
            }
        }
    }
    
    fun getDiagnostics() = hooks.map { it.diagnostics }
}
