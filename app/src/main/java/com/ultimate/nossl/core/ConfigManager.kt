package com.ultimate.nossl.core

import de.robv.android.xposed.XSharedPreferences
import java.io.File

object ConfigManager {
    private const val PREFS_NAME = "ultimate_nossl_prefs"
    private const val PACKAGE_NAME = "com.ultimate.nossl"

    private var xPrefs: XSharedPreferences? = null

    init {
        try {
            xPrefs = XSharedPreferences(PACKAGE_NAME, PREFS_NAME)
            xPrefs?.makeWorldReadable()
        } catch (e: Throwable) {
            try {
                val prefsFile = File("/data/user_de/0/$PACKAGE_NAME/shared_prefs/$PREFS_NAME.xml")
                if (prefsFile.exists()) {
                    xPrefs = XSharedPreferences(prefsFile)
                }
            } catch (ignored: Throwable) {}
        }
    }

    fun isHookEnabled(hookId: String): Boolean {
        return try {
            xPrefs?.reload()
            xPrefs?.getBoolean("hook_$hookId", true) ?: true
        } catch (e: Throwable) {
            true
        }
     }

    fun isHookEnabled(packageName: String): Boolean {
        if (packageName == PACKAGE_NAME) return false
        return try {
            xPrefs?.reload()
            xPrefs?.getBoolean("app_$packageName", true) ?: true
        } catch (e: Throwable) {
            true
        }
    }
}
