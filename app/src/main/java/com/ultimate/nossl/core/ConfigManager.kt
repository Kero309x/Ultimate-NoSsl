package com.ultimate.nossl.core

import de.robv.android.xposed.XSharedPreferences
import com.ultimate.nossl.core.config.ConfigurationRepository
import com.ultimate.nossl.core.config.NoSslConfiguration
import java.io.File

object ConfigManager : ConfigurationRepository {
    private const val PREFS_NAME = "ultimate_nossl_prefs"
    private const val PACKAGE_NAME = "com.ultimate.nossl"

    @Volatile private var xPrefs: XSharedPreferences? = null

    private fun prefs(): XSharedPreferences? {
        xPrefs?.let { return it }
        return synchronized(this) {
            xPrefs ?: runCatching {
                XSharedPreferences(PACKAGE_NAME, PREFS_NAME).apply {
                    makeWorldReadable()
                    reload()
                }
            }.getOrNull().also {
                if (it == null) {
                    val f = File("/data/data/$PACKAGE_NAME/shared_prefs/$PREFS_NAME.xml")
                    if (f.exists()) xPrefs = XSharedPreferences(f)
                } else xPrefs = it
            }
        }
    }

    override fun isHookEnabled(hookId: String): Boolean {
        return runCatching { prefs()?.getBoolean("hook_$hookId", true) }.getOrNull() ?: true
    }

    override fun isTargetAppEnabled(packageName: String): Boolean {
        if (packageName == PACKAGE_NAME) return false
        return runCatching { prefs()?.getBoolean("app_$packageName", false) }.getOrNull() ?: false
    }

    override fun setHookEnabled(hookId: String, enabled: Boolean) {
        throw UnsupportedOperationException("Xposed module cannot write to preferences directly.")
    }

    override fun setTargetAppEnabled(packageName: String, enabled: Boolean) {
        throw UnsupportedOperationException("Xposed module cannot write to preferences directly.")
    }

    override fun getConfiguration(): NoSslConfiguration {
        val allPrefs = prefs()?.all ?: emptyMap()
        val enabledHooks = allPrefs.filterKeys { it.startsWith("hook_") && (allPrefs[it] as? Boolean == true) }.keys.map { it.removePrefix("hook_") }.toSet()
        val targetApps = allPrefs.filterKeys { it.startsWith("app_") && (allPrefs[it] as? Boolean == true) }.keys.map { it.removePrefix("app_") }.toSet()
        return NoSslConfiguration(enabledHooks, targetApps)
    }
}
