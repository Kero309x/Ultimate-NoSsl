package com.ultimate.nossl.ui.config

import android.content.Context
import android.content.SharedPreferences
import com.ultimate.nossl.core.config.ConfigurationRepository
import com.ultimate.nossl.core.config.NoSslConfiguration

class UiConfigurationRepository(private val context: Context) : ConfigurationRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("ultimate_nossl_prefs", Context.MODE_PRIVATE)

    override fun isHookEnabled(hookId: String): Boolean {
        return prefs.getBoolean("hook_$hookId", true)
    }

    override fun isTargetAppEnabled(packageName: String): Boolean {
        return prefs.getBoolean("app_$packageName", true)
    }

    override fun setHookEnabled(hookId: String, enabled: Boolean) {
        prefs.edit().putBoolean("hook_$hookId", enabled).apply()
    }

    override fun setTargetAppEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putBoolean("app_$packageName", enabled).apply()
    }

    override fun getConfiguration(): NoSslConfiguration {
        val allPrefs = prefs.all
        val enabledHooks = allPrefs.filterKeys { it.startsWith("hook_") && (allPrefs[it] as? Boolean == true) }.keys.map { it.removePrefix("hook_") }.toSet()
        val targetApps = allPrefs.filterKeys { it.startsWith("app_") && (allPrefs[it] as? Boolean == true) }.keys.map { it.removePrefix("app_") }.toSet()
        return NoSslConfiguration(enabledHooks, targetApps)
    }
}
