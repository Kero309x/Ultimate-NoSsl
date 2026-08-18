
package com.ultimate.nossl.core

import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.ultimate.nossl.utils.Logger

object HookEngine {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        Logger.i("Core HookEngine initialized for ${lpparam.packageName}")
    }
}
