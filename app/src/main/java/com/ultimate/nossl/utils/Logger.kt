
package com.ultimate.nossl.utils

import de.robv.android.xposed.XposedBridge

object Logger {
    private const val TAG = "[🔓ULTIMATE]"

    fun i(msg: String) = XposedBridge.log("$TAG ✅ $msg")
    fun w(msg: String) = XposedBridge.log("$TAG ⚠️ $msg")
    fun e(msg: String, t: Throwable? = null) {
        XposedBridge.log("$TAG ❌ $msg")
        t?.let { XposedBridge.log(it) }
    }
    fun hook(cls: String, method: String) = i("HOOKED: $cls.$method")
    fun native(func: String) = i("NATIVE: $func")
    fun scan(msg: String) = i("SCAN: $msg")
}
