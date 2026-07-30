
package com.ultimate.nossl.hooks

import com.ultimate.nossl.utils.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileOutputStream

class FileHook {
    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedBridge.hookAllMethods(File::class.java, "createNewFile", object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: XC_MethodHook.MethodHookParam): Any {
                    val file = param.thisObject as File
                    try {
                        if (file.exists()) return false
                        ensureParent(file)
                        return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                    } catch (e: Throwable) {
                        // If it still fails, it might be the IOException we saw
                        // Try one last time after ensuring parent again
                        try {
                           ensureParent(file)
                           return file.createNewFile()
                        } catch (ee: Throwable) {
                           return false 
                        }
                    }
                }
            })

            XposedBridge.hookAllConstructors(File::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val file = param.thisObject as File
                    val path = file.absolutePath
                    if (path.contains("com.community.oneroom") && (path.endsWith(".log") || path.endsWith(".txt"))) {
                        ensureParent(file)
                    }
                }
            })

            // Hook FileOutputStream constructors
            val fosCls = FileOutputStream::class.java
            XposedBridge.hookAllConstructors(fosCls, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val arg = param.args.firstOrNull()
                    if (arg is File) {
                        ensureParent(arg)
                    } else if (arg is String) {
                        ensureParent(File(arg))
                    }
                }
            })
        } catch (e: Throwable) { }
    }

    private fun ensureParent(file: File) {
        try {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                if (parent.mkdirs()) {
                    Logger.i("FILE: Created missing parent directory: ${parent.absolutePath}")
                }
            }
        } catch (e: Throwable) {}
    }
}
