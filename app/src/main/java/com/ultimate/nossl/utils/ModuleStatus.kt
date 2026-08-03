package com.ultimate.nossl.utils

import androidx.annotation.Keep

@Keep
object ModuleStatus {
    @JvmStatic
    @Keep
    fun isModuleActive(): Boolean {
        // Returned false by default; when LSPosed / Xposed loads our module,
        // UltimateHook will self-hook this method to return true.
        return false
    }
}
